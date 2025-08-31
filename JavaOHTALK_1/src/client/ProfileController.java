package client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.application.Platform;
import javafx.stage.Stage;

import org.json.JSONObject;

/**
 * ProfileController:
 * - 본인 프로필(Username, Nickname, etc.)
 */
public class ProfileController implements MessageListener {
    @FXML private ImageView profileImageView;
    @FXML private Label usernameLabel;
    @FXML private Label nicknameLabel;

    @FXML
    public void initialize() {
        MainApp.getNetworkClient().addMessageListener(this);
        loadProfile();
    }

    private void loadProfile() {
        JSONObject data = new JSONObject();
        data.put("user_id", MainApp.getUserId());
        String req = MessageProtocol.createRequest("get_profile", data);
        MainApp.getNetworkClient().sendMessage(req);
    }

    @FXML
    public void onClose() {
        Stage st = (Stage)usernameLabel.getScene().getWindow();
        st.close();
    }

    @Override
    public void onMessageReceived(String msg) {
        Platform.runLater(()->{
            JSONObject resp = new JSONObject(msg);
            String type = resp.getString("type");
            String status = resp.optString("status","");
            JSONObject d = resp.optJSONObject("data");

            if(type.equals("get_profile") && status.equals("ok")) {
                JSONObject pf = d.getJSONObject("profile");
                usernameLabel.setText("Username: "+pf.getString("username"));
                nicknameLabel.setText("Nickname: "+pf.optString("nickname","NoName"));
                String imgPath = pf.optString("profile_image","");
                if(imgPath!=null && !imgPath.isEmpty()) {
                    Image img = new Image("file:"+imgPath,true);
                    profileImageView.setImage(img);
                }
            }
        });
    }
}
