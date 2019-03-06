import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import viewmodel.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class LogInPane {
    private static final VBox container = new VBox(35.0);
    private static final Label title = new Label("SIGN IN");
    private static final Label SQLlocationLabel = new Label("SQL Server location");
    private static final TextField SQLlocation = new TextField();
    private static final Label DBnameLabel = new Label("Database");
    private static final TextField DBname = new TextField();
    private static final Label usernameLabel = new Label("Username");
    private static final TextField username = new TextField();
    private static final Label passwordLabel = new Label("Password");
    private static final PasswordField password = new PasswordField();
    private static final Label invalidWarning = new Label("");
    private static final Button submitBtn = new Button("Sign In");
    public static Scene scene = new Scene(new Group(), Config.WIDTH / 2, Config.HEIGHT / 2);

    public void init() {
        connect_components();
        setbuttonfunction();
    }

    public void setbuttonfunction() {
        submitBtn.setOnAction(event -> {
            String connectionUrl = "jdbc:sqlserver://" +
                    SQLlocation.getText() +
                    ";databaseName=" +
                    DBname.getText() +
                    ";user=" +
                    username.getText() +
                    ";password=" +
                    password.getText() +
                    "";
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                SceneManager.conn  = DriverManager.getConnection(connectionUrl);
                SceneManager.statement = SceneManager.conn.createStatement();
                System.out.println("OK");
                SceneManager.updateList();
                SceneManager.showMainMenuScene();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void connect_components()
     {
        container.setAlignment(Pos.CENTER);
        SQLlocation.setText("localhost:1433");
        DBname.setText("Customer");
        username.setText("root");
        password.setText("1234");
        invalidWarning.visibleProperty().setValue(false);

        VBox vbox = new VBox(35); // spacing = 8
        HBox hBox1 = new HBox(20.0, SQLlocationLabel, SQLlocation);
        hBox1.setAlignment(Pos.CENTER);
        HBox hBox2 = new HBox(20.0, DBnameLabel, DBname);
        hBox2.setAlignment(Pos.CENTER);
        HBox hBox3 = new HBox(20.0, usernameLabel, username);
        hBox3.setAlignment(Pos.CENTER);
        HBox hBox4 = new HBox(20.0, passwordLabel, password);
        hBox4.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(
                title,
                hBox1,
                hBox2,
                hBox3,
                hBox4,
                invalidWarning,
                submitBtn
        );
        vbox.setAlignment(Pos.CENTER);

        scene.setRoot(vbox);

    }

}