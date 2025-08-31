package client;

/**
 * MessageListener:
 * - 서버로부터 받은 메시지(String) 처리 콜백
 */
public interface MessageListener {
    void onMessageReceived(String message);
}
