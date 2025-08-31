package client;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;

/**
 * MainApp:
 * - JavaFX Application의 시작점.
 * - 서버와 연결을 시도하고 로그인 화면(login.fxml)을 표시합니다.
 */
public class MainApp extends Application {
    private static NetworkClient networkClient; // 서버와의 통신을 담당하는 NetworkClient 객체
    private static int userId = -1;            // 현재 로그인한 사용자의 ID (-1은 로그인하지 않은 상태를 의미)
    private static String nickname = "NoName"; // 현재 로그인한 사용자의 닉네임

    /**
     * JavaFX 애플리케이션 시작 메서드.
     * - 서버와 연결하고 로그인 화면을 로드합니다.
     *
     * @param primaryStage 애플리케이션의 기본 Stage
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1) 서버 연결
        networkClient = new NetworkClient("localhost", 5007); // 서버 주소와 포트 지정
        boolean connected = networkClient.connect(); // 서버 연결 시도
        if (!connected) { // 연결 실패 시 에러 메시지를 출력하고 애플리케이션 종료
            System.err.println("Cannot connect to server.");
            System.exit(0);
        }

        // 2) login.fxml 파일을 로드하여 로그인 화면 표시
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load(); // FXML 파일 로드
        Scene scene = new Scene(root);

        // 스타일(CSS) 적용
        scene.getStylesheets().add(getClass().getResource("/client/resources/application.css").toExternalForm());

        // Stage 설정
        primaryStage.setTitle("Login"); // 창 제목 설정
        primaryStage.setScene(scene);  // Scene 설정
        primaryStage.show();           // 창 표시
    }

    /**
     * 서버 소켓에 접근하기 위한 메서드.
     *
     * @return 현재 NetworkClient 객체
     */
    public static NetworkClient getNetworkClient() {
        return networkClient;
    }

    /**
     * 로그인 후 사용자 ID를 저장합니다.
     *
     * @param uid 사용자 ID
     */
    public static void setUserId(int uid) {
        userId = uid;
    }

    /**
     * 현재 로그인한 사용자 ID를 반환합니다.
     *
     * @return 사용자 ID
     */
    public static int getUserId() {
        return userId;
    }

    /**
     * 로그인 후 사용자 닉네임을 저장합니다.
     *
     * @param nick 사용자 닉네임
     */
    public static void setNickname(String nick) {
        nickname = nick;
    }

    /**
     * 현재 로그인한 사용자 닉네임을 반환합니다.
     *
     * @return 사용자 닉네임
     */
    public static String getNickname() {
        return nickname;
    }

    /**
     * 에러 메시지를 표시하기 위한 다이얼로그 창.
     *
     * @param msg 에러 메시지
     */
    public static void showErrorDialog(String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");      // 다이얼로그 제목 설정
        alert.setHeaderText(null);    // 헤더 텍스트 비활성화
        alert.setContentText(msg);    // 에러 메시지 설정
        alert.showAndWait();          // 다이얼로그 표시
    }

    /**
     * 애플리케이션의 main 메서드.
     * - JavaFX 애플리케이션을 실행합니다.
     *
     * @param args 명령줄 인자
     */
    public static void main(String[] args) {
        launch(args); // JavaFX 애플리케이션 실행
    }
}
