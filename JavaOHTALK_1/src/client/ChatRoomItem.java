package client;

/**
 * ChatRoomItem:
 * - 채팅방 정보를 저장하는 클래스.
 * - 채팅방 ID와 이름을 포함하며, ListView<ChatRoomItem>에서 사용됩니다.
 */
public class ChatRoomItem {
    private int roomId;      // 채팅방의 고유 ID
    private String roomName; // 채팅방의 이름

    /**
     * 생성자: 채팅방 ID와 이름을 초기화합니다.
     *
     * @param roomId   채팅방 ID (고유 식별자)
     * @param roomName 채팅방 이름 (사용자 친화적 이름)
     */
    public ChatRoomItem(int roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
    }

    /**
     * 채팅방의 ID를 반환합니다.
     *
     * @return 채팅방 ID (int)
     */
    public int getRoomId() {
        return roomId;
    }

    /**
     * 채팅방의 이름을 반환합니다.
     *
     * @return 채팅방 이름 (String)
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * ChatRoomItem 객체를 문자열로 표현합니다.
     * - 주로 디버깅 및 로깅 목적으로 사용됩니다.
     *
     * @return "채팅방 ID:채팅방 이름" 형식의 문자열
     */
    @Override
    public String toString() {
        return roomId + ":" + roomName;
    }
}
