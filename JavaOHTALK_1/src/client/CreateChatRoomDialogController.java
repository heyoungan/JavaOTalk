package client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.stage.Stage;

/**
 * CreateChatRoomDialogController:
 * 그룹 채팅방 생성 다이얼로그의 컨트롤러 클래스.
 * - 방 이름 설정 및 참가자 선택 후 채팅방 생성 요청을 서버로 전송합니다.
 */
public class CreateChatRoomDialogController implements MessageListener {

    @FXML private TextField roomNameField;       // 방 이름 입력 필드
    @FXML private ListView<String> friendListView; // 친구 목록 리스트뷰
    @FXML private Label statusLabel;            // 상태 메시지를 표시하는 라벨

    private JSONArray friends = new JSONArray(); // 친구 목록을 저장하는 JSON 배열

    /**
     * 컨트롤러 초기화 메서드
     * - 메시지 리스너를 등록합니다.
     */
    @FXML
    public void initialize() {
        MainApp.getNetworkClient().addMessageListener(this);
    }

    /**
     * 친구 목록을 설정하고 리스트뷰를 업데이트합니다.
     *
     * @param friends 서버로부터 받은 친구 목록(JSON 배열)
     */
    public void setFriends(JSONArray friends) {
        this.friends = friends;

        // 다중 선택 모드 활성화
        friendListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 친구 목록을 리스트뷰에 추가
        for (int i = 0; i < friends.length(); i++) {
            JSONObject f = friends.getJSONObject(i);
            friendListView.getItems().add(f.getInt("friend_id") + ":" + f.getString("nickname"));
        }
    }

    /**
     * "생성" 버튼 클릭 이벤트 처리
     * - 입력된 방 이름과 선택된 친구들로 채팅방 생성 요청을 보냅니다.
     */
    @FXML
    public void onCreate() {
        String rname = roomNameField.getText().trim(); // 방 이름 가져오기
        if (rname.isEmpty()) { // 방 이름이 비어있을 경우
            statusLabel.setText("방 이름을 입력하세요."); // 경고 메시지 표시
            return;
        }

        // 참가자 목록 초기화 및 본인 추가
        JSONArray parts = new JSONArray();
        parts.put(MainApp.getUserId()); // 현재 사용자 ID 추가

        // 선택된 친구를 참가자 목록에 추가
        for (String sel : friendListView.getSelectionModel().getSelectedItems()) {
            String[] arr = sel.split(":");
            int fid = Integer.parseInt(arr[0]); // 친구 ID 추출
            parts.put(fid);
        }

        // 참가자 수 검증 (최소 2명 필요: 본인 + 친구 1명 이상)
        if (parts.length() < 2) {
            statusLabel.setText("최소 본인 + 1명 = 2명 이상이 필요합니다."); // 경고 메시지 표시
            return;
        }

        // 채팅방 생성 요청 데이터 구성
        JSONObject data = new JSONObject();
        data.put("name", rname); // 방 이름
        data.put("type", "group"); // 채팅방 유형 (그룹)
        data.put("participants", parts); // 참가자 목록

        // 서버로 채팅방 생성 요청 전송
        String req = MessageProtocol.createRequest("create_chat_room", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    /**
     * 서버에서 수신한 메시지를 처리합니다.
     *
     * @param msg 서버로부터 수신된 메시지
     */
    @Override
    public void onMessageReceived(String msg) {
        Platform.runLater(() -> {
            JSONObject resp = new JSONObject(msg);
            String type = resp.getString("type"); // 메시지 유형
            String status = resp.optString("status", ""); // 상태 정보
            JSONObject d = resp.optJSONObject("data"); // 추가 데이터

            // 채팅방 생성 응답 처리
            if (type.equals("create_chat_room")) {
                if (status.equals("ok")) {
                    // 채팅방 생성 성공 시 다이얼로그 창 닫기
                    Stage st = (Stage) roomNameField.getScene().getWindow();
                    st.close();
                } else {
                    // 채팅방 생성 실패 시 상태 메시지 표시
                    statusLabel.setText("채팅방 생성 실패: " + d.optString("reason", ""));
                }
            }
        });
    }
}
