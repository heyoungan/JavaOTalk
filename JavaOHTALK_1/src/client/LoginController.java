package client;

import org.json.JSONObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * LoginController:
 * - 로그인 및 회원가입을 처리하는 컨트롤러.
 * - 로그인 성공 시 메인 화면(main.fxml)을 로딩합니다.
 */
public class LoginController implements MessageListener {

    @FXML private TextField usernameField;  // 사용자 이름 입력 필드
    @FXML private PasswordField passwordField; // 비밀번호 입력 필드
    @FXML private Label statusLabel;        // 상태 메시지를 표시하는 라벨

    /**
     * 초기화 메서드:
     * - 메시지 리스너를 네트워크 클라이언트에 등록합니다.
     */
    @FXML
    public void initialize() {
        MainApp.getNetworkClient().addMessageListener(this);
    }

    /**
     * 로그인 버튼 클릭 시 실행:
     * - 입력값 검증 후 서버로 로그인 요청을 전송합니다.
     */
    @FXML
    public void onLogin() {
        String user = usernameField.getText().trim(); // 사용자 이름
        String pass = passwordField.getText().trim(); // 비밀번호

        // 입력값 검증
        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("아이디/비밀번호를 입력하세요."); // 경고 메시지
            return;
        }

        // 로그인 요청 데이터 생성
        JSONObject data = new JSONObject();
        data.put("username", user);
        data.put("password", pass);

        // 서버로 요청 전송
        String req = MessageProtocol.createRequest("login", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    /**
     * 회원가입 버튼 클릭 시 실행:
     * - 입력값 검증 후 서버로 회원가입 요청을 전송합니다.
     */
    @FXML
    public void onRegister() {
        String user = usernameField.getText().trim(); // 사용자 이름
        String pass = passwordField.getText().trim(); // 비밀번호

        // 입력값 검증
        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("아이디/비밀번호를 입력하세요."); // 경고 메시지
            return;
        }

        // 회원가입 요청 데이터 생성
        JSONObject data = new JSONObject();
        data.put("username", user);
        data.put("password", pass);
        data.put("nickname", user); // 닉네임은 기본적으로 사용자 이름으로 설정

        // 서버로 요청 전송
        String req = MessageProtocol.createRequest("register", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    /**
     * 서버에서 수신한 메시지를 처리합니다.
     *
     * @param msg 서버로부터 받은 메시지(JSON 형식)
     */
    @Override
    public void onMessageReceived(String msg) {
        Platform.runLater(() -> {
            JSONObject resp = new JSONObject(msg);
            String type = resp.getString("type"); // 메시지 타입
            String status = resp.optString("status", ""); // 처리 상태
            JSONObject d = resp.optJSONObject("data"); // 데이터 부분

            if (type.equals("login")) {
                // 로그인 응답 처리
                if (status.equals("ok")) {
                    int uid = d.getInt("user_id"); // 사용자 ID
                    JSONObject info = d.getJSONObject("user_info");
                    String nick = info.optString("nickname", "NoName"); // 닉네임
                    MainApp.setUserId(uid); // 사용자 ID 저장
                    MainApp.setNickname(nick); // 닉네임 저장
                    openMain(); // 메인 화면 열기
                } else {
                    // 로그인 실패 시 메시지 표시
                    statusLabel.setText("로그인 실패: " + d.optString("reason", ""));
                }
            } else if (type.equals("register")) {
                // 회원가입 응답 처리
                if (status.equals("ok")) {
                    statusLabel.setText("회원가입 성공! 로그인 해주세요."); // 성공 메시지
                } else {
                    // 회원가입 실패 시 메시지 표시
                    statusLabel.setText("회원가입 실패: " + d.optString("reason", ""));
                }
            }
        });
    }

    /**
     * 메인 화면을 로딩하고 현재 로그인 창을 대체합니다.
     */
    private void openMain() {
        try {
            // 현재 스테이지 가져오기
            Stage st = (Stage) usernameField.getScene().getWindow();

            // 메인 화면 로드
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            // 새로운 Scene 생성 및 스타일 적용
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/client/resources/application.css").toExternalForm());

            // 스테이지 설정
            st.setTitle("JavaOH! Chat");
            st.setScene(scene);
        } catch (Exception e) {
            // 로딩 실패 시 오류 메시지 표시
            e.printStackTrace();
            MainApp.showErrorDialog("메인 화면 로딩 실패");
        }
    }
}
