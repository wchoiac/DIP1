package viewmodel.panes;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import viewmodel.SceneManager;

public class MainMenuPane extends BorderPane {

    private VBox container;
    private Label title;
    private Button scanUserID;
    private Button signRecord;
    private Button quitButton;

    public MainMenuPane() {
        container = new VBox(50);
        container.setAlignment(Pos.CENTER);
        title = new Label("MediRec");
        scanUserID = new Button("Scan ID");
        signRecord = new Button("Sign Record");
        quitButton = new Button("Quit");
        connectComponents();
        styleComponents();
        setCallbacks();
    }

    private void connectComponents() {
        container.getChildren().addAll(
                title,
                scanUserID,
                signRecord,
                quitButton
        );
        this.setCenter(container);
    }

    private void styleComponents() {

    }

    private void setCallbacks() {
        scanUserID.setOnAction(e -> SceneManager.getInstance().showScanScene());
        signRecord.setOnAction(e -> SceneManager.getInstance().showSignScene());
        quitButton.setOnAction(e -> Platform.exit());
    }
}
