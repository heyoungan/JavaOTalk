package client;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * MainController:
 * - 메인 화면에서:
 *   1) 친구 목록 + Online/Offline 표시
 *   2) 채팅방 목록
 *   3) 친구 요청/수락
 *   4) 1:1 채팅 "대화하기" 버튼
 *   5) 채팅방 생성 버튼
 *   6) 프로필 보기 버튼
 * - 5초 주기로 get_online_status 요청 → 친구 목록에서 [Online]/[Offline] 갱신
 */
public class MainController implements MessageListener {

    @FXML private Label nicknameLabel;
    @FXML private ListView<String> friendListView; // 왼쪽: 친구 목록
    @FXML private ListView<String> roomListView;   // 오른쪽: 채팅방 목록
    @FXML private Button chatWithButton;           // "대화하기" 버튼 (1:1 채팅)

    private JSONArray currentFriends        = new JSONArray(); // 서버에서 내려온 친구 목록
    private JSONArray currentRooms          = new JSONArray(); // 서버에서 내려온 채팅방 목록
    private JSONArray currentFriendRequests = new JSONArray(); // 서버에서 내려온 친구 요청 목록

    /**
     * FXML 초기화
     */
    @FXML
    public void initialize() {
        // 닉네임 라벨 표시
        nicknameLabel.setText(MainApp.getNickname());

        // 서버 메시지 수신 리스너 등록
        MainApp.getNetworkClient().addMessageListener(this);

        // 친구를 선택하면 "대화하기" 버튼 활성화/비활성
        friendListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal)->{
            chatWithButton.setDisable(newVal == null);
        });

        // 초기 로딩
        loadFriendList();
        loadChatRooms();

        // [Online/Offline] 표시를 위해 5초마다 updateOnlineStatus() 호출
        Timeline onlineStatusTimer = new Timeline(new KeyFrame(Duration.seconds(5), e -> updateOnlineStatus()));
        onlineStatusTimer.setCycleCount(Timeline.INDEFINITE);
        onlineStatusTimer.play();
    }

    // ------------------------------------------------
    // 서버 요청(친구 목록, 채팅방 목록)
    // ------------------------------------------------
    private void loadFriendList() {
        JSONObject data = new JSONObject();
        String req = MessageProtocol.createRequest("get_friend_list", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    private void loadChatRooms() {
        JSONObject data = new JSONObject();
        String req = MessageProtocol.createRequest("get_chat_rooms", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    // ------------------------------------------------
    // FXML 액션 메서드
    // ------------------------------------------------
    @FXML
    public void onRefreshFriends() {
        loadFriendList();
    }

    @FXML
    public void onRefreshRooms() {
        loadChatRooms();
    }

    @FXML
    public void onProfile() {
        // 프로필 보기
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
            Parent root = loader.load();

            Stage st = new Stage();
            st.setTitle("My Profile");
            st.setScene(new Scene(root));
            st.show();
        } catch(Exception e) {
            e.printStackTrace();
            MainApp.showErrorDialog("프로필 화면 로딩 중 오류");
        }
    }

    @FXML
    public void onSendFriendRequest() {
        // 친구 요청 다이얼로그
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/send_friend_request_dialog.fxml"));
            Parent root = loader.load();

            Stage st = new Stage();
            st.setTitle("Send Friend Request");
            st.setScene(new Scene(root));
            st.show();
        } catch(Exception e) {
            e.printStackTrace();
            MainApp.showErrorDialog("친구 요청 화면 로딩 중 오류");
        }
    }

    @FXML
    public void onShowFriendRequests() {
        // get_friend_requests
        JSONObject data = new JSONObject();
        String req = MessageProtocol.createRequest("get_friend_requests", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    @FXML
    public void onChatWith() {
        // "대화하기" 버튼 → 1:1 채팅
        String selected = friendListView.getSelectionModel().getSelectedItem();
        if(selected == null) return;

        // "friend_id:nickname"
        String[] arr = selected.split(":");
        int friendId = Integer.parseInt(arr[0]);

        // 1:1 채팅방 생성
        JSONArray parts = new JSONArray();
        parts.put(MainApp.getUserId());
        parts.put(friendId);

        JSONObject data = new JSONObject();
        data.put("name","1:1 Chat with "+friendId);
        data.put("type","private");
        data.put("participants", parts);

        String req = MessageProtocol.createRequest("create_chat_room", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    @FXML
    public void onCreateChatRoom() {
        // 그룹 채팅방 생성
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_chat_room_dialog.fxml"));
            Parent root = loader.load();

            CreateChatRoomDialogController ctrl = loader.getController();
            ctrl.setFriends(currentFriends); // 다중 선택 가능

            Stage st = new Stage();
            st.setTitle("Create Chat Room");
            st.setScene(new Scene(root));
            st.show();
        } catch(Exception e) {
            e.printStackTrace();
            MainApp.showErrorDialog("채팅방 생성 화면 로딩 오류");
        }
    }

    @FXML
    public void onOpenChat() {
        // 선택된 방 열기
        String selected = roomListView.getSelectionModel().getSelectedItem();
        if(selected==null) return;

        String[] arr = selected.split(":");
        int roomId = Integer.parseInt(arr[0]);
        openChatRoom(roomId);
    }

    private void openChatRoom(int roomId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chatroom.fxml"));
            Parent root = loader.load();

            ChatRoomController ctrl = loader.getController();
            ctrl.setChatRoomId(roomId);

            Stage st = new Stage();
            st.setTitle("Chat Room " + roomId);
            st.setScene(new Scene(root));
            st.show();
        } catch(Exception e) {
            e.printStackTrace();
            MainApp.showErrorDialog("채팅방 로딩 중 오류");
        }
    }

    // ------------------------------------------------
    // [Online/Offline] 상태 갱신
    // ------------------------------------------------
    private void updateOnlineStatus() {
        if(currentFriends.length() == 0) return;

        List<Integer> friendIds = new ArrayList<>();
        for(int i=0; i<currentFriends.length(); i++){
            int fid = currentFriends.getJSONObject(i).getInt("friend_id");
            friendIds.add(fid);
        }
        if(friendIds.isEmpty()) return;

        JSONObject data = new JSONObject();
        data.put("friend_ids", friendIds);
        String req = MessageProtocol.createRequest("get_online_status", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    // ------------------------------------------------
    // 메시지 수신 처리
    // ------------------------------------------------
    @Override
    public void onMessageReceived(String message) {
        Platform.runLater(() -> {
            JSONObject resp = new JSONObject(message);
            String type   = resp.getString("type");
            String status = resp.optString("status","");
            JSONObject data = resp.optJSONObject("data");

            if(type.equals("get_friend_list") && status.equals("ok")){
                currentFriends = data.getJSONArray("friends");
                refreshFriendListView();
            }
            else if(type.equals("get_chat_rooms") && status.equals("ok")){
                currentRooms = data.getJSONArray("rooms");
                refreshRoomListView();
            }
            else if(type.equals("send_friend_request")){
                if(!status.equals("ok")){
                    MainApp.showErrorDialog("친구 요청 실패: "+data.optString("reason",""));
                }
            }
            else if(type.equals("get_friend_requests") && status.equals("ok")){
                // 받은 친구 요청 목록
                currentFriendRequests = data.getJSONArray("requests");
                showFriendRequestsDialog();
            }
            else if(type.equals("accept_friend_request")){
                if(!status.equals("ok")){
                    MainApp.showErrorDialog("친구 요청 수락 실패: "+data.optString("reason",""));
                }
            }
            else if(type.equals("create_chat_room")){
                if(!status.equals("ok")){
                    MainApp.showErrorDialog("채팅방 생성 실패: "+data.optString("reason",""));
                }
            }

            // 서버 push 이벤트
            if(type.equals("friend_list_updated")){
                currentFriends = data.getJSONArray("friends");
                refreshFriendListView();
            }
            else if(type.equals("friend_request_list_updated")){
                // 필요시 자동 다이얼로그
            }
            else if(type.equals("chat_rooms_updated")){
                currentRooms = data.getJSONArray("rooms");
                refreshRoomListView();
            }
            else if(type.equals("new_message")){
                // ChatRoomController에서 처리
            }
            else if(type.equals("get_online_status") && status.equals("ok")){
                JSONArray stList = data.getJSONArray("status_list");
                updateFriendListWithStatus(stList);
            }
        });
    }

    private void refreshFriendListView() {
        friendListView.getItems().clear();
        for(int i=0; i<currentFriends.length(); i++){
            JSONObject f = currentFriends.getJSONObject(i);
            friendListView.getItems().add(f.getInt("friend_id") + ":" + f.getString("nickname"));
        }
    }

    private void refreshRoomListView() {
        roomListView.getItems().clear();
        for(int i=0; i<currentRooms.length(); i++){
            JSONObject r = currentRooms.getJSONObject(i);
            roomListView.getItems().add(r.getInt("id") + ":" + r.getString("name"));
        }
    }

    /**
     * [Online]/[Offline] 표시
     */
    private void updateFriendListWithStatus(JSONArray stList){
        friendListView.getItems().clear();
        for(int i=0; i<currentFriends.length(); i++){
            JSONObject f = currentFriends.getJSONObject(i);
            int fid = f.getInt("friend_id");
            String nick = f.getString("nickname");

            boolean online = false;
            for(int j=0; j<stList.length(); j++){
                JSONObject stObj = stList.getJSONObject(j);
                if(stObj.getInt("user_id") == fid){
                    online = stObj.getBoolean("online");
                    break;
                }
            }

            String label = fid + ":" + nick + (online ? " [Online]" : " [Offline]");
            friendListView.getItems().add(label);
        }
    }

    /**
     * 친구 요청 목록 다이얼로그
     */
    private void showFriendRequestsDialog() {
        if(currentFriendRequests.length()==0){
            Alert a = new Alert(Alert.AlertType.INFORMATION, "No pending friend requests.");
            a.setTitle("Friend Requests");
            a.showAndWait();
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Friend Requests");

        ListView<String> lv = new ListView<>();
        for(int i=0;i<currentFriendRequests.length();i++){
            JSONObject r = currentFriendRequests.getJSONObject(i);
            lv.getItems().add(r.getInt("request_id")+": from "+r.getString("from_nickname"));
        }

        ButtonType acceptBtn = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(acceptBtn, ButtonType.CLOSE);
        dialog.getDialogPane().setContent(lv);

        dialog.setResultConverter(bt->{
            if(bt == acceptBtn){
                String sel = lv.getSelectionModel().getSelectedItem();
                if(sel!=null){
                    String[] arr = sel.split(":");
                    int reqId = Integer.parseInt(arr[0]);
                    // 수락 요청
                    JSONObject data = new JSONObject();
                    data.put("request_id", reqId);
                    String rq = MessageProtocol.createRequest("accept_friend_request", data);
                    MainApp.getNetworkClient().sendMessage(rq);
                }
            }
            return null;
        });

        dialog.showAndWait();
    }
}
