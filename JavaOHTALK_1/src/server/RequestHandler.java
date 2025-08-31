package server;

import org.json.JSONArray;
import org.json.JSONObject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * RequestHandler:
 * - 클라이언트 요청(JSON)을 파싱하고,
 * - DBManager와 상호작용해 결과를 얻은 뒤,
 * - MessageProtocol로 응답(JSON) 생성.
 * 
 * "기존 기능" 전부 포함:
 *  - 회원가입(register)
 *  - 로그인(login)
 *  - 친구 리스트(get_friend_list), 친구 요청(send_friend_request), 수락(accept_friend_request)
 *  - 채팅방 생성(create_chat_room), 나가기(leave_chat_room), 메시지 전송(send_message) 등
 */
public class RequestHandler {
    private DBManager db;         // DB 접근
    private ServerMain server;    // 서버(이벤트 push 등)

    /**
     * 생성자
     */
    public RequestHandler(DBManager db, ServerMain server) {
        this.db = db;
        this.server = server;
    }

    /**
     * handleRequest:
     * @param handler  ClientHandler (userId등 보관)
     * @param request  클라이언트 -> 서버 JSON string
     * @return         서버 -> 클라이언트 응답(JSON string)
     */
    public String handleRequest(ClientHandler handler, String request) {
        JSONObject reqObj = new JSONObject(request);    // {"type":"xxx", "data":{...}}
        String type = reqObj.getString("type");
        JSONObject data = reqObj.getJSONObject("data");
        JSONObject respData = new JSONObject();         // 응답 data

        try {
            switch(type) {
                // ------------------------------------------------
                // 회원가입
                // ------------------------------------------------
                case "register": {
                    // data: {username, password, nickname}
                    if(!data.has("username")||!data.has("password")||!data.has("nickname")) {
                        return failResp(type,"Invalid data");
                    }
                    String uname = data.getString("username");
                    String pass  = data.getString("password");
                    String nick  = data.getString("nickname");
                    boolean regOk = db.registerUser(uname, pass, nick);
                    if(regOk) {
                        return MessageProtocol.createResponse("register","ok",respData);
                    } else {
                        respData.put("reason","Username exists or DB error");
                        return MessageProtocol.createResponse("register","fail",respData);
                    }
                }

                // ------------------------------------------------
                // 로그인
                // ------------------------------------------------
                case "login": {
                    // data: {username, password}
                    if(!data.has("username")||!data.has("password")) {
                        return failResp(type,"Invalid data");
                    }
                    String uname = data.getString("username");
                    String pass  = data.getString("password");
                    int uid = db.loginCheck(uname, pass);
                    if(uid>0) {
                        // 로그인 성공
                        handler.setUserId(uid);
                        server.setUserOnline(uid, handler);

                        JSONObject uinfo = db.getUserInfo(uid); // user info
                        respData.put("user_id", uid);
                        respData.put("user_info", uinfo);
                        return MessageProtocol.createResponse("login","ok",respData);
                    } else {
                        respData.put("reason","Invalid username or password");
                        return MessageProtocol.createResponse("login","fail",respData);
                    }
                }

                // ------------------------------------------------
                // 친구 목록
                // ------------------------------------------------
                case "get_friend_list": {
                    // data: {}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    JSONArray flist = db.getFriendList(handler.getUserId());
                    respData.put("friends", flist);
                    return MessageProtocol.createResponse("get_friend_list","ok",respData);
                }

                // ------------------------------------------------
                // 친구 요청 보내기
                // ------------------------------------------------
                case "send_friend_request": {
                    // data: {to_username}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    if(!data.has("to_username")) return failResp(type,"Invalid data");
                    String toUser = data.getString("to_username");
                    int toId = getUserIdByUsername(toUser);
                    if(toId<=0) {
                        respData.put("reason","User not found");
                        return MessageProtocol.createResponse("send_friend_request","fail",respData);
                    }
                    boolean frOk = db.sendFriendRequest(handler.getUserId(), toId);
                    if(frOk) {
                        // 상대방에게 friend_request_list_updated push
                        server.pushFriendRequestListUpdated(toId);
                        return MessageProtocol.createResponse("send_friend_request","ok",respData);
                    } else {
                        respData.put("reason","Already requested or DB error");
                        return MessageProtocol.createResponse("send_friend_request","fail",respData);
                    }
                }

                // ------------------------------------------------
                // 친구 요청 목록
                // ------------------------------------------------
                case "get_friend_requests": {
                    // data: {}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    JSONArray reqs = db.getPendingFriendRequests(handler.getUserId());
                    respData.put("requests", reqs);
                    return MessageProtocol.createResponse("get_friend_requests","ok",respData);
                }

                // ------------------------------------------------
                // 친구 요청 수락
                // ------------------------------------------------
                case "accept_friend_request": {
                    // data: {request_id}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    if(!data.has("request_id")) return failResp(type,"Invalid data");
                    int rqid = data.getInt("request_id");
                    boolean accepted = db.acceptFriendRequest(rqid, handler.getUserId());
                    if(accepted) {
                        // fromUser, toUser 모두 friend_list_updated push
                        int fromId = getFromUserIdOfRequest(rqid);
                        if(fromId>0) server.pushFriendListUpdated(fromId);
                        server.pushFriendListUpdated(handler.getUserId());
                        return MessageProtocol.createResponse("accept_friend_request","ok",respData);
                    } else {
                        respData.put("reason","Request not found or DB error");
                        return MessageProtocol.createResponse("accept_friend_request","fail",respData);
                    }
                }

                // ------------------------------------------------
                // 채팅방 목록
                // ------------------------------------------------
                case "get_chat_rooms": {
                    // data: {}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    JSONArray rooms = db.getChatRoomsForUser(handler.getUserId());
                    respData.put("rooms", rooms);
                    return MessageProtocol.createResponse("get_chat_rooms","ok",respData);
                }

                // ------------------------------------------------
                // 채팅방 생성
                // ------------------------------------------------
                case "create_chat_room": {
                    // data: {name, type, participants:[...]}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    if(!data.has("name")||!data.has("type")||!data.has("participants")) {
                        return failResp(type,"Invalid data");
                    }
                    String rname = data.getString("name");
                    String rtype = data.getString("type");
                    JSONArray parts = data.getJSONArray("participants");

                    int newRoomId = db.createChatRoom(rname, rtype);
                    if(newRoomId<=0) {
                        respData.put("reason","DB error creating room");
                        return MessageProtocol.createResponse("create_chat_room","fail",respData);
                    }
                    // 멤버 등록
                    for(int i=0; i<parts.length(); i++){
                        db.addChatRoomMember(newRoomId, parts.getInt(i));
                    }
                    // 참가자 모두에게 chat_rooms_updated
                    for(int i=0; i<parts.length(); i++){
                        server.pushChatRoomsUpdated(parts.getInt(i));
                    }
                    respData.put("room_id", newRoomId);
                    return MessageProtocol.createResponse("create_chat_room","ok",respData);
                }

                // ------------------------------------------------
                // 채팅방 나가기
                // ------------------------------------------------
                case "leave_chat_room": {
                    // data: {room_id}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    if(!data.has("room_id")) return failResp(type,"Invalid data");
                    int roomId = data.getInt("room_id");
                    boolean left = db.removeChatRoomMember(roomId, handler.getUserId());
                    if(left) {
                        // 본인에게 chat_rooms_updated
                        server.pushChatRoomsUpdated(handler.getUserId());
                        // 방이 아직 살아있다면 남은 멤버에게도 push
                        if(db.getChatRoomInfo(roomId)!=null) {
                            JSONArray remain = db.getMembersOfRoom(roomId);
                            for(int i=0; i<remain.length(); i++){
                                int uid = remain.getJSONObject(i).getInt("user_id");
                                server.pushChatRoomsUpdated(uid);
                            }
                        }
                        return MessageProtocol.createResponse("leave_chat_room","ok",respData);
                    } else {
                        respData.put("reason","DB error or invalid room");
                        return MessageProtocol.createResponse("leave_chat_room","fail",respData);
                    }
                }

                // ------------------------------------------------
                // 메시지 전송
                // ------------------------------------------------
                case "send_message": {
                    // data: {room_id, message}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    if(!data.has("room_id")||!data.has("message")) return failResp(type,"Invalid data");
                    int roomId = data.getInt("room_id");
                    String msg = data.getString("message");
                    boolean saved = db.saveMessage(roomId, handler.getUserId(), msg);
                    if(saved) {
                        server.broadcastMessageToRoom(roomId, handler.getUserId(), msg);
                        return MessageProtocol.createResponse("send_message","ok",respData);
                    } else {
                        respData.put("reason","DB error");
                        return MessageProtocol.createResponse("send_message","fail",respData);
                    }
                }

                // ------------------------------------------------
                // 메시지 로드
                // ------------------------------------------------
                case "load_messages": {
                    // data: {room_id}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    if(!data.has("room_id")) return failResp(type,"Invalid data");
                    int roomId = data.getInt("room_id");
                    JSONArray arr = db.loadMessages(roomId);
                    respData.put("messages", arr);
                    return MessageProtocol.createResponse("load_messages","ok",respData);
                }

                // ------------------------------------------------
                // 프로필 조회
                // ------------------------------------------------
                case "get_profile": {
                    // data: {user_id}
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    int pid = data.getInt("user_id");
                    JSONObject pf = db.getUserInfo(pid);
                    if(pf!=null) {
                        respData.put("profile", pf);
                        return MessageProtocol.createResponse("get_profile","ok",respData);
                    } else {
                        respData.put("reason","User not found");
                        return MessageProtocol.createResponse("get_profile","fail",respData);
                    }
                }
                
                case "get_online_status": {
                    // data: { friend_ids:[ ... ] }
                    if(!handler.isAuthenticated()) return failResp(type,"Not authenticated");
                    JSONArray arr = data.getJSONArray("friend_ids");
                    JSONArray statusList = new JSONArray();
                    for(int i=0; i<arr.length(); i++){
                        int fid = arr.getInt(i);
                        boolean online = server.isUserOnline(fid);
                        JSONObject o = new JSONObject();
                        o.put("user_id", fid);
                        o.put("online", online);
                        statusList.put(o);
                    }
                    respData.put("status_list", statusList);
                    return MessageProtocol.createResponse("get_online_status","ok",respData);
                }


                default: {
                    respData.put("reason","Unknown request type");
                    return MessageProtocol.createResponse(type,"fail",respData);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            respData.put("reason","Server error:"+e.getMessage());
            return MessageProtocol.createResponse(type,"fail",respData);
        }
    }

    // ------------------------------------------------
    // 헬퍼 메서드: username -> userId
    // ------------------------------------------------
    private int getUserIdByUsername(String username) {
        try {
            String sql = "SELECT id FROM users WHERE username=?";
            PreparedStatement ps = db.getConnection().prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return rs.getInt("id");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 친구 요청 from_user_id 얻기
     */
    private int getFromUserIdOfRequest(int requestId) {
        try {
            String sql = "SELECT from_user_id FROM friend_requests WHERE id=?";
            PreparedStatement ps = db.getConnection().prepareStatement(sql);
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return rs.getInt("from_user_id");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 실패 응답
     */
    private String failResp(String type, String reason) {
        JSONObject rd = new JSONObject();
        rd.put("reason", reason);
        return MessageProtocol.createResponse(type,"fail",rd);
    }
}
