package server;

import org.json.JSONObject;

/**
 * MessageProtocol:
 * - 서버 <-> 클라이언트 간 메시지를 JSON 포맷으로
 */
public class MessageProtocol {
    /**
     * 클라이언트가 서버로 요청 만들 때 (참고용)
     */
    public static String createRequest(String type, JSONObject data) {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("data", data);
        return obj.toString();
    }

    /**
     * 서버가 클라이언트로 응답
     */
    public static String createResponse(String type, String status, JSONObject data) {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("status", status);
        obj.put("data", data);
        return obj.toString();
    }

    /**
     * 서버가 이벤트(push)를 보낼 때
     */
    public static String createEvent(String type, JSONObject data) {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("status", "ok");
        obj.put("data", data);
        return obj.toString();
    }
}
