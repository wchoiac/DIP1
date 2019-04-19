import javafx.application.Application;
import javafx.stage.Stage;



public class Main extends Application {

    @Override
    public void start(final Stage stage) {
        SceneManager.init();
        SceneManager.setStage(stage);
        SceneManager.showLogInScene();
    }

    public static void main(String[] args) {
        launch(args);
    }
}