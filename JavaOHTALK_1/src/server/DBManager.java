package server;

import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * DBManager:
 *  - MySQL 연결
 *  - 회원가입/로그인, 친구, 친구요청, 채팅방+멤버, 메시지 전부 담당
 *  - chat_room_members 구조
 */
public class DBManager {
    private Connection conn;

    public DBManager(String url, String user, String pass) throws SQLException {
        conn = DriverManager.getConnection(url, user, pass);
    }

    public Connection getConnection() {
        return conn;
    }

    // 샘플: SHA-256 해싱
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for(byte b: hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------
    // 1) [유저 / Auth]
    // ---------------------

    public boolean registerUser(String username, String password, String nickname) {
        if(isUsernameExist(username)) {
            return false;
        }
        String hashed = hashPassword(password);
        String sql = "INSERT INTO users(username, password, nickname) VALUES(?,?,?)";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashed);
            ps.setString(3, nickname);
            ps.executeUpdate();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isUsernameExist(String username) {
        String sql = "SELECT id FROM users WHERE username=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int loginCheck(String username, String password) {
        String sql = "SELECT id,password FROM users WHERE username=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                String stored = rs.getString("password");
                String input  = hashPassword(password);
                if(stored.equals(input)) {
                    return rs.getInt("id");
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public JSONObject getUserInfo(int userId) {
        String sql = "SELECT id,username,nickname,profile_image FROM users WHERE id=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("username", rs.getString("username"));
                obj.put("nickname", rs.getString("nickname"));
                obj.put("profile_image", rs.getString("profile_image"));
                return obj;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getNicknameByUserId(int userId) {
        String sql = "SELECT nickname FROM users WHERE id=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return rs.getString("nickname");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "User"+userId;
    }

    // ---------------------
    // 2) [친구 / Friend]
    // ---------------------

    public JSONArray getFriendList(int userId) {
        String sql = "SELECT f.friend_user_id, u.nickname, u.username "
                   + "FROM friends f JOIN users u ON f.friend_user_id=u.id "
                   + "WHERE f.user_id=?";
        JSONArray arr = new JSONArray();
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                JSONObject o = new JSONObject();
                o.put("friend_id", rs.getInt("friend_user_id"));
                o.put("nickname", rs.getString("nickname"));
                o.put("username", rs.getString("username"));
                arr.put(o);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    public boolean addFriend(int userId, int friendId) {
        if(isFriend(userId, friendId) || userId==friendId) return false;
        String sql = "INSERT INTO friends(user_id, friend_user_id) VALUES(?,?)";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.executeUpdate();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeFriend(int userId, int friendId) {
        String sql = "DELETE FROM friends WHERE user_id=? AND friend_user_id=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.executeUpdate();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isFriend(int userId, int friendId) {
        String sql = "SELECT id FROM friends WHERE user_id=? AND friend_user_id=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------------------
    // 3) [친구 요청 friend_requests]
    // ---------------------

    public boolean sendFriendRequest(int fromUserId, int toUserId) {
        if(isFriend(fromUserId, toUserId) || fromUserId==toUserId) return false;
        String chk = "SELECT id FROM friend_requests WHERE from_user_id=? AND to_user_id=? AND status='pending'";
        try(PreparedStatement c = conn.prepareStatement(chk)) {
            c.setInt(1, fromUserId);
            c.setInt(2, toUserId);
            ResultSet r = c.executeQuery();
            if(r.next()) return false; // 이미 pending
        } catch(Exception e) { e.printStackTrace(); }

        String sql = "INSERT INTO friend_requests(from_user_id,to_user_id,status) VALUES(?,?,?)";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fromUserId);
            ps.setInt(2, toUserId);
            ps.setString(3,"pending");
            ps.executeUpdate();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public JSONArray getPendingFriendRequests(int userId) {
        String sql = "SELECT fr.id, fr.from_user_id, u.nickname, u.username "
                   + "FROM friend_requests fr JOIN users u ON fr.from_user_id=u.id "
                   + "WHERE fr.to_user_id=? AND fr.status='pending'";
        JSONArray arr = new JSONArray();
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                JSONObject o = new JSONObject();
                o.put("request_id", rs.getInt("id"));
                o.put("from_user_id", rs.getInt("from_user_id"));
                o.put("from_nickname", rs.getString("nickname"));
                o.put("from_username", rs.getString("username"));
                arr.put(o);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    public boolean acceptFriendRequest(int requestId, int accepterId) {
        String sel = "SELECT from_user_id,to_user_id FROM friend_requests WHERE id=? AND status='pending'";
        try(PreparedStatement s = conn.prepareStatement(sel)) {
            s.setInt(1, requestId);
            ResultSet rs = s.executeQuery();
            if(rs.next()) {
                int fromId = rs.getInt("from_user_id");
                int toId   = rs.getInt("to_user_id");
                if(toId == accepterId) {
                    // update status
                    String upd = "UPDATE friend_requests SET status='accepted' WHERE id=?";
                    try(PreparedStatement ups = conn.prepareStatement(upd)) {
                        ups.setInt(1, requestId);
                        ups.executeUpdate();
                    }
                    // 양방향 friends
                    addFriend(fromId, toId);
                    addFriend(toId, fromId);
                    return true;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ---------------------
    // 4) [채팅방 + 멤버 + 메시지]
    // ---------------------

    public int createChatRoom(String name, String type) {
        String sql = "INSERT INTO chat_rooms(name,type) VALUES(?,?)";
        try(PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if(rs.next()) return rs.getInt(1);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean deleteChatRoom(int roomId) {
        String sql = "DELETE FROM chat_rooms WHERE id=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addChatRoomMember(int roomId, int userId) {
        String chk = "SELECT id FROM chat_room_members WHERE room_id=? AND user_id=?";
        try(PreparedStatement c = conn.prepareStatement(chk)) {
            c.setInt(1, roomId);
            c.setInt(2, userId);
            ResultSet r = c.executeQuery();
            if(r.next()) return false; 
        } catch(Exception e) { e.printStackTrace(); }

        String sql = "INSERT INTO chat_room_members(room_id,user_id) VALUES(?,?)";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            ps.executeUpdate();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeChatRoomMember(int roomId, int userId) {
        String sql = "DELETE FROM chat_room_members WHERE room_id=? AND user_id=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, userId);
            int affected = ps.executeUpdate();
            if(affected>0) {
                // 남은 멤버 0 => 방 삭제
                if(countRoomMembers(roomId)==0) {
                    deleteChatRoom(roomId);
                }
                return true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private int countRoomMembers(int roomId) {
        String sql = "SELECT COUNT(*) as cnt FROM chat_room_members WHERE room_id=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return rs.getInt("cnt");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public JSONArray getMembersOfRoom(int roomId) {
        String sql = "SELECT u.id,u.nickname "
                   + "FROM chat_room_members m JOIN users u ON m.user_id=u.id "
                   + "WHERE m.room_id=?";
        JSONArray arr = new JSONArray();
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                JSONObject mem = new JSONObject();
                mem.put("user_id", rs.getInt("id"));
                mem.put("nickname", rs.getString("nickname"));
                arr.put(mem);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    public JSONObject getChatRoomInfo(int roomId) {
        String sql = "SELECT name,type FROM chat_rooms WHERE id=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                JSONObject r = new JSONObject();
                r.put("id", roomId);
                r.put("name", rs.getString("name"));
                r.put("type", rs.getString("type"));
                r.put("participants", getMembersOfRoom(roomId));
                return r;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONArray getChatRoomsForUser(int userId) {
        String sql = "SELECT c.id,c.name,c.type "
                   + "FROM chat_room_members m JOIN chat_rooms c ON m.room_id=c.id "
                   + "WHERE m.user_id=?";
        JSONArray arr = new JSONArray();
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                JSONObject room = new JSONObject();
                room.put("id", rs.getInt("id"));
                room.put("name", rs.getString("name"));
                room.put("type", rs.getString("type"));
                room.put("participants", getMembersOfRoom(rs.getInt("id")));
                arr.put(room);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    // ---------------------
    // 5) 메시지
    // ---------------------

    public boolean saveMessage(int roomId, int senderId, String msg) {
        String sql = "INSERT INTO messages(chat_room_id,sender_id,message) VALUES(?,?,?)";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, senderId);
            ps.setString(3, msg);
            ps.executeUpdate();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public JSONArray loadMessages(int roomId) {
        String sql = "SELECT m.id,m.sender_id,u.nickname,m.message,m.timestamp "
                   + "FROM messages m JOIN users u ON m.sender_id=u.id "
                   + "WHERE m.chat_room_id=? ORDER BY m.id DESC LIMIT 50";
        JSONArray arr = new JSONArray();
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                JSONObject msg = new JSONObject();
                msg.put("id", rs.getInt("id"));
                msg.put("sender_id", rs.getInt("sender_id"));
                msg.put("sender_nickname", rs.getString("nickname"));
                msg.put("message", rs.getString("message"));
                msg.put("timestamp", rs.getTimestamp("timestamp").toString());
                arr.put(msg);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    public void close() {
        try {
            if(conn!=null && !conn.isClosed()) {
                conn.close();
            }
        } catch(Exception e){}
    }
}
