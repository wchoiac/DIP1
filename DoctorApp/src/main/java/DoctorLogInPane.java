import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import viewmodel.Config;

public class DoctorLogInPane extends Application {
    private static final VBox container = new VBox(35.0);
    private static final Label title = new Label("SIGN IN");
    private static final Label SQLserver = new Label("SQL Server");
    private static final TextField SQL = new TextField();
    private static final Label Database = new Label("Database");
    private static final TextField Db = new TextField();
    private static final Label usernameLabel = new Label("Username");
    private static final TextField username = new TextField();
    private static final Label passwordLabel = new Label("Password");
    private static final Label invalidWarning = new Label("");
    private static final PasswordField password = new PasswordField();
    private static final Button submitBtn = new Button("Sign In");

    public void init() {
        container.setAlignment(Pos.CENTER);
        username.setPromptText("Username");
        password.setPromptText("Password");
        invalidWarning.visibleProperty().setValue(false);
    }


    @Override
    public void start(final Stage stage) {
        stage.setTitle("MediRec");
        stage.getIcons().add(new Image("images/icon.png"));
        Scene scene = new Scene(new Group(), Config.WIDTH / 2, Config.HEIGHT / 2);

        VBox vbox = new VBox(35); // spacing = 8
        HBox hBox0 = new HBox(13.0, SQLserver, SQL);
        hBox0.setAlignment(Pos.CENTER);
        HBox hBox00 = new HBox(26.0, Database, Db);
        hBox00.setAlignment(Pos.CENTER);
        HBox hBox1 = new HBox(20.0, usernameLabel, username);
        hBox1.setAlignment(Pos.CENTER);
        HBox hBox2 = new HBox(20.0, passwordLabel, password);
        hBox2.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(
                title,
                hBox0,
                hBox00,
                hBox1,
                hBox2,
                invalidWarning,
                submitBtn
        );
        vbox.setAlignment(Pos.CENTER);

        scene.setRoot(vbox);
        scene.getStylesheets().add("css/styles.css");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}