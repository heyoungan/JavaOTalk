package client;

import org.json.JSONObject;

/**
 * MessageProtocol:
 * - 클라이언트 <-> 서버 간 JSON 형식 메시지 유틸
 */
public class MessageProtocol {
    /**
     * 클라이언트에서 서버로 "요청" 만들 때
     */
    public static String createRequest(String type, JSONObject data) {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("data", data);
        return obj.toString();
    }

    /**
     * 서버 -> 클라이언트 "응답"
     */
    public static String createResponse(String type, String status, JSONObject data) {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("status", status);
        obj.put("data", data);
        return obj.toString();
    }

    /**
     * 서버 -> 클라이언트 "이벤트(push)"
     */
    public static String createEvent(String type, JSONObject data) {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("status", "ok");
        obj.put("data", data);
        return obj.toString();
    }
}
