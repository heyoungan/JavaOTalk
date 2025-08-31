package client;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.json.JSONObject;

/**
 * ChatRoomCell:
 * ListView에서 사용할 ChatRoomItem에 대한 커스텀 ListCell 클래스.
 * - 채팅방 이름을 표시하며, "나가기" 버튼을 통해 채팅방 나가기 요청을 서버로 전송합니다.
 */
public class ChatRoomCell extends ListCell<ChatRoomItem> {
    private Label nameLabel;    // 채팅방 이름을 표시하는 라벨
    private Button leaveButton; // "나가기" 버튼
    private HBox container;     // 라벨과 버튼을 포함하는 HBox 레이아웃

    /**
     * ChatRoomCell 생성자.
     * - UI 구성 요소를 초기화하고 이벤트 핸들러를 설정합니다.
     */
    public ChatRoomCell() {
        super();

        // 라벨 초기화: 채팅방 이름 표시
        nameLabel = new Label();

        // "나가기" 버튼 초기화 및 텍스트 설정
        leaveButton = new Button("나가기");

        // 버튼과 라벨 사이의 공백을 위한 Spacer 생성
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Spacer가 가능한 공간을 모두 차지하도록 설정

        // HBox 레이아웃 구성: 라벨, Spacer, 버튼을 포함
        container = new HBox(10, nameLabel, spacer, leaveButton); // 구성 요소 간 간격 10px
        container.setStyle("-fx-padding:5;"); // 레이아웃에 여백 설정

        // "나가기" 버튼 클릭 이벤트 처리
        leaveButton.setOnAction(e -> {
            ChatRoomItem item = getItem(); // 현재 셀의 아이템 가져오기
            if (item == null) return;     // 아이템이 없으면 이벤트 처리 종료
            leaveChatRoom(item.getRoomId()); // 채팅방 나가기 요청 실행
        });
    }

    /**
     * 셀의 내용을 업데이트합니다.
     *
     * @param item  현재 셀에 할당된 ChatRoomItem 객체
     * @param empty 셀이 비어 있는지 여부
     */
    @Override
    protected void updateItem(ChatRoomItem item, boolean empty) {
        super.updateItem(item, empty);

        // 아이템이 없거나 비어 있으면 셀을 초기화
        if (empty || item == null) {
            setText(null);     // 텍스트 초기화
            setGraphic(null);  // 그래픽 초기화
        } else {
            // 아이템이 존재하면 라벨에 채팅방 이름과 ID 설정
            nameLabel.setText(item.getRoomName() + " (#" + item.getRoomId() + ")");

            // 셀의 그래픽을 HBox 레이아웃으로 설정
            setGraphic(container);
        }
    }

    /**
     * 서버로 채팅방 나가기 요청을 전송합니다.
     *
     * @param roomId 나가려는 채팅방의 고유 ID
     */
    private void leaveChatRoom(int roomId) {
        // JSON 객체 생성 및 방 ID 추가
        JSONObject data = new JSONObject();
        data.put("room_id", roomId);

        // 채팅방 나가기 요청 메시지 생성
        String req = MessageProtocol.createRequest("leave_chat_room", data);

        // 네트워크 클라이언트를 통해 메시지 전송
        MainApp.getNetworkClient().sendMessage(req);
    }
}
