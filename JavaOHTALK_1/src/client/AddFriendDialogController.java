package client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import org.json.JSONObject;
import javafx.stage.Stage;
import server.MessageProtocol;

/**
 * "친구 추가" 다이얼로그 컨트롤러 클래스.
 * 사용자 입력을 처리하고 서버와 통신하며 UI를 업데이트합니다.
 */
public class AddFriendDialogController implements MessageListener {

    @FXML private TextField usernameField; // 사용자 이름 입력 필드
    @FXML private Label statusLabel;      // 상태를 표시하는 라벨

    /**
     * 초기화 메서드: 메시지 리스너를 등록합니다.
     */
    @FXML
    public void initialize() {
        // 네트워크 클라이언트에 메시지 리스너를 추가
        MainApp.getNetworkClient().addMessageListener(this);
    }

    /**
     * "추가" 버튼 클릭 이벤트를 처리합니다.
     * 사용자가 입력한 이름을 검증한 후 서버로 요청을 전송합니다.
     */
    @FXML
    public void onAdd() {
        // 사용자 입력값을 가져오고 공백을 제거
        String fuser = usernameField.getText().trim();

        // 입력값 검증: 공백일 경우 경고 메시지를 표시
        if (fuser.isEmpty()) {
            statusLabel.setText("아이디를 입력하세요.");
            return;
        }

        // JSON 객체에 사용자 이름 추가
        JSONObject data = new JSONObject();
        data.put("friend_username", fuser);

        // 친구 추가 요청 메시지를 생성
        String req = MessageProtocol.createRequest("add_friend", data);

        // 서버로 메시지를 전송
        MainApp.getNetworkClient().sendMessage(req);
    }

    /**
     * 서버에서 수신한 메시지를 처리합니다.
     * 메시지 유형에 따라 UI를 업데이트합니다.
     */
    @Override
    public void onMessageReceived(String message) {
        // UI 스레드에서 실행되도록 설정
        Platform.runLater(() -> {
            // JSON 형식으로 수신 메시지를 파싱
            JSONObject resp = new JSONObject(message);
            String type = resp.getString("type");
            String status = resp.optString("status", "fail");
            JSONObject data = resp.optJSONObject("data");

            // 친구 추가 응답 메시지를 처리
            if (type.equals("add_friend")) {
                if (status.equals("ok")) {
                    // 성공 메시지 표시 및 창 닫기
                    statusLabel.setText("친구 추가 성공!");
                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    stage.close();
                } else {
                    // 실패 사유를 사용자에게 표시
                    String reason = data.optString("reason", "친구 추가 실패");
                    statusLabel.setText("추가 실패: " + reason);
                }
            }
        });
    }
}
