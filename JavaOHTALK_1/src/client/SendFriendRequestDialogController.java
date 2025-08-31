package client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import org.json.JSONObject;
import javafx.stage.Stage;

/**
 * SendFriendRequestDialogController:
 * - 친구 요청을 보내는 다이얼로그 컨트롤러.
 * - 사용자가 입력한 아이디를 서버로 전송하여 친구 요청을 처리합니다.
 */
public class SendFriendRequestDialogController implements MessageListener {

    @FXML private TextField usernameField; // 친구 요청을 보낼 사용자 아이디 입력 필드
    @FXML private Label statusLabel;      // 상태 메시지를 표시하는 라벨

    /**
     * 초기화 메서드:
     * - 메시지 리스너를 등록하여 서버 응답을 처리할 준비를 합니다.
     */
    @FXML
    public void initialize() {
        MainApp.getNetworkClient().addMessageListener(this);
    }

    /**
     * "보내기" 버튼 클릭 시 실행:
     * - 입력값 검증 후 서버로 친구 요청을 전송합니다.
     */
    @FXML
    public void onSend() {
        // 사용자 입력값 가져오기
        String user = usernameField.getText().trim();

        // 입력값 검증: 비어 있는 경우 경고 메시지 표시
        if (user.isEmpty()) {
            statusLabel.setText("아이디를 입력하세요."); // 경고 메시지
            return;
        }

        // 서버로 전송할 요청 데이터 생성
        JSONObject data = new JSONObject();
        data.put("to_username", user); // 요청 대상 사용자 이름 추가

        // 메시지 프로토콜 생성 및 서버로 전송
        String req = MessageProtocol.createRequest("send_friend_request", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    /**
     * 서버로부터 수신한 메시지를 처리합니다.
     *
     * @param msg 서버로부터 수신한 메시지(JSON 형식)
     */
    @Override
    public void onMessageReceived(String msg) {
        Platform.runLater(() -> { // UI 업데이트를 위한 JavaFX 플랫폼 스레드 실행
            JSONObject resp = new JSONObject(msg);
            String type = resp.getString("type");       // 메시지 타입
            String status = resp.optString("status", ""); // 처리 상태
            JSONObject d = resp.optJSONObject("data"); // 추가 데이터

            // 친구 요청 전송에 대한 응답 처리
            if (type.equals("send_friend_request")) {
                if (status.equals("ok")) {
                    // 성공 메시지 표시 및 다이얼로그 창 닫기
                    statusLabel.setText("친구 요청 전송 성공!");
                    Stage st = (Stage) usernameField.getScene().getWindow();
                    st.close();
                } else {
                    // 실패 메시지 표시
                    statusLabel.setText("요청 실패: " + d.optString("reason", ""));
                }
            }
        });
    }
}
