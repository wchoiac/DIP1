package viewmodel.panes;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import main.Helper;

public class QRCodePane extends BorderPane {
    private ImageView qrView;
    private Button backButton;

    public QRCodePane() {
        qrView = new ImageView();
        backButton = new Button("Back");

        connectComponents();
        styleComponents();
        setCallbacks();
    }

    public void drawQRCode(String str) {
        Helper.getInstance().drawQRCode(qrView, str);
    }

    private void connectComponents() {
        setAlignment(qrView, Pos.CENTER);
        setAlignment(backButton, Pos.CENTER);
        setCenter(qrView);
        setBottom(backButton);
    }

    private void styleComponents() {

    }

    private void setCallbacks() {

    }
}
