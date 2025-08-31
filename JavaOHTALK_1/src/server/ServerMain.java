package server;

import java.net.*;
import java.util.*;
import java.io.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * ServerMain:
 * - 서버소켓 열고 클라이언트 accept
 * - 온라인 유저 관리
 * - 이벤트 push (chat_rooms_updated, friend_list_updated, etc.)
 */
public class ServerMain {
    private DBManager dbManager;                // DB
    private RequestHandler requestHandler;      // 요청 처리
    private Map<Integer, ClientHandler> onlineUsers; // userId -> handler
    private int port = 5007;

    public ServerMain() {
        // Thread-safe map
        onlineUsers = Collections.synchronizedMap(new HashMap<>());
    }

    public void startServer() {
        try {
            // DB 접속
            dbManager = new DBManager(
                "jdbc:mysql://localhost:3306/chat_app?useSSL=false&serverTimezone=UTC",
                "root","jakewe03210519!!"
            );
            requestHandler = new RequestHandler(dbManager, this);

            ServerSocket ss = new ServerSocket(port);
            System.out.println("Server started on port "+port);

            while(true) {
                Socket client = ss.accept();
                ClientHandler ch = new ClientHandler(client, this);
                ch.start();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * RequestHandler getter
     */
    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    /**
     * 유저 온라인 등록
     */
    public void setUserOnline(int userId, ClientHandler handler) {
        onlineUsers.put(userId, handler);
    }

    /**
     * 유저 오프라인
     */
    public void setUserOffline(int userId) {
        onlineUsers.remove(userId);
    }

    /**
     * 유저가 온라인인지
     */
    public boolean isUserOnline(int userId) {
        return onlineUsers.containsKey(userId);
    }

    /**
     * 특정 방에 메시지 broadcast
     */
    public void broadcastMessageToRoom(int roomId, int senderId, String msg) {
        try {
            String senderNick = dbManager.getNicknameByUserId(senderId);
            JSONArray members = dbManager.getMembersOfRoom(roomId);

            JSONObject data = new JSONObject();
            data.put("room_id", roomId);
            data.put("sender_id", senderId);
            data.put("sender_nickname", senderNick);
            data.put("message", msg);
            data.put("timestamp", System.currentTimeMillis());

            String ev = MessageProtocol.createEvent("new_message", data);

            // 멤버에게 전송
            for(int i=0; i<members.length(); i++){
                int uid = members.getJSONObject(i).getInt("user_id");
                if(onlineUsers.containsKey(uid)) {
                    onlineUsers.get(uid).sendMessage(ev);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 방 목록 갱신 push (chat_rooms_updated)
     */
    public void pushChatRoomsUpdated(int userId) {
        if(onlineUsers.containsKey(userId)) {
            JSONArray rooms = dbManager.getChatRoomsForUser(userId);
            JSONObject data = new JSONObject();
            data.put("rooms", rooms);
            String msg = MessageProtocol.createEvent("chat_rooms_updated", data);
            onlineUsers.get(userId).sendMessage(msg);
        }
    }

    /**
     * 친구 목록 갱신 push
     */
    public void pushFriendListUpdated(int userId) {
        if(onlineUsers.containsKey(userId)) {
            JSONArray flist = dbManager.getFriendList(userId);
            JSONObject data = new JSONObject();
            data.put("friends", flist);
            String ev = MessageProtocol.createEvent("friend_list_updated", data);
            onlineUsers.get(userId).sendMessage(ev);
        }
    }

    /**
     * 친구 요청 목록 갱신 push
     */
    public void pushFriendRequestListUpdated(int userId) {
        if(onlineUsers.containsKey(userId)) {
            JSONArray reqs = dbManager.getPendingFriendRequests(userId);
            JSONObject data = new JSONObject();
            data.put("requests", reqs);
            String ev = MessageProtocol.createEvent("friend_request_list_updated", data);
            onlineUsers.get(userId).sendMessage(ev);
        }
    }

    public static void main(String[] args) {
        ServerMain server = new ServerMain();
        server.startServer();
    }
}
