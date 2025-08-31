package client;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChatRoomController:
 * 채팅방 내부에서 메시지 송수신과 UI 업데이트를 담당하는 컨트롤러 클래스.
 * - 이모티콘 삽입, 메시지 전송, 방 나가기 등 다양한 기능 포함.
 */
public class ChatRoomController implements MessageListener {

    @FXML private Label roomNameLabel;      // 채팅방 이름을 표시하는 라벨
    @FXML private ListView<Label> messageListView; // 메시지를 표시하는 리스트뷰
    @FXML private TextField messageField;  // 메시지 입력 필드
    @FXML private Button leaveButton;      // 방 나가기 버튼

    private int chatRoomId; // 현재 채팅방의 ID

    /**
     * 컨트롤러 초기화 메서드
     * - 메시지 리스너를 등록합니다.
     */
    @FXML
    public void initialize() {
        MainApp.getNetworkClient().addMessageListener(this);
    }

    /**
     * 채팅방 ID를 설정하고 메시지를 로드합니다.
     *
     * @param roomId 채팅방 ID
     */
    public void setChatRoomId(int roomId) {
        this.chatRoomId = roomId;
        loadMessages(); // 서버로부터 메시지를 로드
        roomNameLabel.setText("Chat Room #" + roomId); // 채팅방 이름 설정
    }

    /**
     * 서버에 메시지 로드 요청을 전송합니다.
     */
    private void loadMessages() {
        JSONObject data = new JSONObject();
        data.put("room_id", chatRoomId);
        String req = MessageProtocol.createRequest("load_messages", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    /**
     * 메시지를 전송합니다.
     * - 사용자가 입력한 메시지를 서버로 보냅니다.
     */
    @FXML
    public void onSend() {
        String msg = messageField.getText().trim(); // 입력된 메시지를 가져옴
        if (msg.isEmpty()) return; // 공백 메시지는 전송하지 않음

        JSONObject data = new JSONObject();
        data.put("room_id", chatRoomId); // 채팅방 ID 추가
        data.put("message", msg); // 메시지 추가
        String req = MessageProtocol.createRequest("send_message", data);
        MainApp.getNetworkClient().sendMessage(req); // 서버로 메시지 전송

        messageField.clear(); // 입력 필드 초기화
    }

    /**
     * 이모티콘 선택 다이얼로그를 열고 선택한 이모티콘을 메시지 필드에 삽입합니다.
     */
    @FXML
    public void onEmojiPicker() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/emoji_picker_dialog.fxml"));
            DialogPane pane = loader.load();
            EmojiPickerDialogController ctrl = loader.getController();

            Dialog<String> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Select Emoji");

            dialog.setResultConverter(btnType -> {
                if (btnType == ButtonType.OK) {
                    return ctrl.getSelectedEmojiName(); // 선택된 이모티콘 반환
                }
                return null;
            });

            dialog.showAndWait().ifPresent(emojiName -> {
                if (emojiName != null && !emojiName.isEmpty()) {
                    // 선택된 이모티콘을 메시지 필드에 추가
                    messageField.setText(messageField.getText() + " <EMOJI:" + emojiName + ">");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            MainApp.showErrorDialog("이모티콘 창 로딩 오류");
        }
    }

    /**
     * 서버로 방 나가기 요청을 전송합니다.
     */
    @FXML
    public void onLeaveRoom() {
        JSONObject data = new JSONObject();
        data.put("room_id", chatRoomId); // 방 ID 설정
        String req = MessageProtocol.createRequest("leave_chat_room", data);
        MainApp.getNetworkClient().sendMessage(req); // 서버로 요청 전송
    }

    /**
     * 서버로부터 수신한 메시지를 UI에 반영합니다.
     *
     * @param message 서버에서 수신한 메시지
     */
    @Override
    public void onMessageReceived(String message) {
        Platform.runLater(() -> {
            JSONObject resp = new JSONObject(message);
            String type = resp.getString("type");
            String status = resp.optString("status", "");
            JSONObject d = resp.optJSONObject("data");

            if (type.equals("load_messages") && status.equals("ok")) {
                // 서버에서 메시지 목록을 받아와 리스트뷰에 추가
                JSONArray arr = d.getJSONArray("messages");
                messageListView.getItems().clear();
                for (int i = arr.length() - 1; i >= 0; i--) {
                    JSONObject m = arr.getJSONObject(i);
                    Label chatItem = createChatLabel(m.getString("sender_nickname"), m.getString("message"));
                    messageListView.getItems().add(chatItem);
                }
            } else if (type.equals("send_message") && status.equals("fail")) {
                // 메시지 전송 실패 시 오류 다이얼로그 표시
                MainApp.showErrorDialog("메시지 전송 실패: " + d.optString("reason", ""));
            } else if (type.equals("new_message")) {
                // 새로운 메시지가 수신되면 리스트뷰에 추가
                if (d.getInt("room_id") == chatRoomId) {
                    Label chatItem = createChatLabel(d.getString("sender_nickname"), d.getString("message"));
                    messageListView.getItems().add(chatItem);
                }
            } else if (type.equals("leave_chat_room") && status.equals("ok")) {
                // 방 나가기 성공 시 창 닫기
                Stage st = (Stage) leaveButton.getScene().getWindow();
                st.close();
            } else {
                // 방 나가기 실패 시 오류 다이얼로그 표시
                MainApp.showErrorDialog("방 나가기 실패: " + d.optString("reason", ""));
            }
        });
    }

    /**
     * 메시지에 포함된 <EMOJI:xxx> 태그를 이미지로 변환하여 표시합니다.
     *
     * @param sender 메시지를 보낸 사람
     * @param text   메시지 내용
     * @return 변환된 메시지를 포함하는 라벨
     */
    private Label createChatLabel(String sender, String text) {
        Label container = new Label();
        TextFlow flow = new TextFlow();
        flow.getChildren().add(new Label(sender + ": ")); // 발신자 표시

        // <EMOJI:xxx> 태그를 찾기 위한 정규식
        Pattern pat = Pattern.compile("<EMOJI:([^>]+)>");
        Matcher m = pat.matcher(text);

        int lastEnd = 0;
        while (m.find()) {
            // 태그 이전의 텍스트 추가
            if (m.start() > lastEnd) {
                flow.getChildren().add(new Text(text.substring(lastEnd, m.start())));
            }
            String emojiName = m.group(1);
            try {
                // 이모티콘 이미지를 로드하여 추가
                String path = getClass().getResource("/client/resources/emojis/" + emojiName + ".png").toExternalForm();
                ImageView iv = new ImageView(new Image(path));
                iv.setFitWidth(20);
                iv.setFitHeight(20);
                flow.getChildren().add(iv);
            } catch (Exception e) {
                // 이미지 로드 실패 시 태그 그대로 표시
                flow.getChildren().add(new Text("<EMOJI:" + emojiName + ">"));
            }
            lastEnd = m.end();
        }
        // 태그 이후의 텍스트 추가
        if (lastEnd < text.length()) {
            flow.getChildren().add(new Text(text.substring(lastEnd)));
        }

        container.setGraphic(flow);
        container.setStyle("-fx-padding:5;");
        return container;
    }
}
