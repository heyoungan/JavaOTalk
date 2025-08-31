package client;

import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;

/**
 * EmojiPickerDialogController:
 * - 이모티콘 선택 다이얼로그 컨트롤러.
 * - 65개의 이모티콘 버튼을 FlowPane에 배치하고, 클릭 시 선택된 이모티콘 이름을 설정합니다.
 * - OK 버튼을 누르면 선택된 이모티콘 이름을 반환합니다.
 */
public class EmojiPickerDialogController {

    @FXML private FlowPane emojiFlow; // 이모티콘 버튼들을 배치할 FlowPane

    private String selectedEmojiName = "";   // 현재 선택된 이모티콘 이름
    private Button lastSelectedButton = null; // 마지막으로 선택된 버튼 (테두리 스타일 초기화를 위해 저장)

    /**
     * 선택된 이모티콘 이름을 반환합니다.
     *
     * @return 선택된 이모티콘 이름 (예: "emoji_01")
     */
    public String getSelectedEmojiName() {
        return selectedEmojiName;
    }

    /**
     * 컨트롤러 초기화 메서드.
     * - 이모티콘을 로드하고 UI를 초기화합니다.
     */
    @FXML
    public void initialize() {
        loadEmojis(); // 이모티콘 버튼 생성 및 FlowPane에 추가
    }

    /**
     * 이모티콘 버튼을 생성하여 FlowPane에 추가합니다.
     * - 65개의 이모티콘 이미지 파일(emoji_01~emoji_65)을 버튼으로 변환.
     * - 각 버튼 클릭 시 선택된 이모티콘을 설정하고 스타일 변경.
     */
    private void loadEmojis() {
        for (int i = 1; i <= 65; i++) {
            // 이모티콘 파일 이름 생성 (예: emoji_01, emoji_02, ...)
            String emojiName = String.format("emoji_%02d", i);

            // 이모티콘 이미지 파일 경로 가져오기
            URL res = getClass().getResource("/client/resources/emojis/" + emojiName + ".png");
            if (res != null) {
                // 이모티콘 이미지 로드 및 크기 설정
                Image img = new Image(res.toExternalForm(), 32, 32, true, true);
                ImageView iv = new ImageView(img);

                // 버튼 생성 및 이미지 설정
                Button btn = new Button();
                btn.setGraphic(iv); // 버튼에 이모티콘 이미지 추가
                btn.setStyle("-fx-background-color: transparent;"); // 버튼 배경 투명 설정

                // 버튼 클릭 이벤트 처리
                btn.setOnAction(e -> {
                    // 선택된 이모티콘 이름 저장
                    selectedEmojiName = emojiName;
                    System.out.println("Selected emoji: " + selectedEmojiName);

                    // 이전에 선택된 버튼의 스타일 초기화
                    if (lastSelectedButton != null) {
                        lastSelectedButton.setStyle("-fx-background-color: transparent;");
                    }

                    // 현재 선택된 버튼에 검은색 테두리 적용
                    btn.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: transparent;");
                    lastSelectedButton = btn; // 현재 버튼을 마지막 선택 버튼으로 설정
                });

                // FlowPane에 버튼 추가
                emojiFlow.getChildren().add(btn);
            }
        }
    }
}
