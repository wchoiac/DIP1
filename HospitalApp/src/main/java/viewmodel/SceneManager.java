package viewmodel;

import javafx.scene.Scene;
import javafx.stage.Stage;
import viewmodel.panes.*;

/**
 * Handles scene changing logic
 */
public class SceneManager {
    private static final SceneManager ourInstance = new SceneManager();
    private final Scene mainMenuScene;
    private final Scene scanScene;
    private final Scene qrScene;
    private Stage stage;

    private SceneManager() {
        mainMenuScene = new Scene(new MainMenuPane(), Config.WIDTH, Config.HEIGHT);
        scanScene = new Scene(new ScanPane(), Config.WIDTH, Config.HEIGHT);
        qrScene = new Scene(new QRCodePane(), Config.WIDTH, Config.HEIGHT);
        mainMenuScene.getStylesheets().add(Config.CSS_STYLES);
        scanScene.getStylesheets().add(Config.CSS_STYLES);
        qrScene.getStylesheets().add(Config.CSS_STYLES);
    }

    public static SceneManager getInstance() {
        return ourInstance;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(e -> ((ScanPane)scanScene.getRoot()).disposeWebCamCamera());
    }

    public QRCodePane getQRCodePane() {
        return (QRCodePane) qrScene.getRoot();
    }

    private void showScene(Scene scene) {
        if (stage == null)
            return;

        stage.setTitle("MediRec");
        stage.hide();
        stage.setScene(scene);
        stage.show();
    }

    public void showMainMenuScene() {
        showScene(mainMenuScene);
    }
    public void showScanScene() {
        showScene(scanScene);
        ((ScanPane)scanScene.getRoot()).startWebCam();
        ((ScanPane)scanScene.getRoot()).setIsSign(false);
    }
    public void showSignScene() {
        showScene(scanScene);
        ((ScanPane)scanScene.getRoot()).startWebCam();
        ((ScanPane)scanScene.getRoot()).setIsSign(true);
    }
    public void showQRScene() { showScene(qrScene); }

}
