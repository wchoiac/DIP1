import general.security.SecurityHelper;
import general.utility.GeneralHelper;
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

import java.io.File;
import java.net.InetAddress;
import java.sql.DriverManager;

public class LogInPane  {
    private static final VBox container = new VBox(35.0);
    private static final Label title = new Label("SIGN IN");
    private static final Label usernameLabel = new Label("Username");
    private static final TextField username = new TextField();
    private static final Label passwordLabel = new Label("Password");
    private static final Label invalidWarning = new Label("");
    private static final PasswordField password = new PasswordField();
    private static final Button submitBtn = new Button("Sign In");

    private static final Label SQLlocationLabel = new Label("SQL Server location");
    private static final TextField SQLlocation = new TextField();
    private static final Label DBnameLabel = new Label("Database");
    private static final TextField DBname = new TextField();
    private static final Label SQLusernameLabel = new Label("Database Username");
    private static final TextField SQLusername = new TextField();
    private static final Label SQLpwLabel = new Label("Database Password");
    private static final PasswordField SQLpw = new PasswordField();

    public static Scene scene = new Scene(new Group(), Config.WIDTH / 2, Config.HEIGHT / 2);

    public static void login() throws Exception{
        InetAddress inetAddress = InetAddress.getByName("25.43.79.11");
        GlobalVar.fullNodeRestClient = new FullNodeRestClient(inetAddress, SecurityHelper.getX509FromDER(new File("med0.cer")));
    }

    {

        container.setAlignment(Pos.CENTER);
        username.setPromptText("Username");
        password.setPromptText("Password");
        invalidWarning.visibleProperty().setValue(false);

        submitBtn.setOnAction(event -> {
            try{
                login();
                GlobalVar.fullNodeRestClient.login(username.getText(), password.getText().toCharArray());
                String connectionUrl = "jdbc:sqlserver://" +
                        SQLlocation.getText() +
                        ";databaseName=" +
                        DBname.getText() +
                        ";user=" +
                        SQLusername.getText() +
                        ";password=" +
                        SQLpw.getText() +
                        "";
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                GlobalVar.conn = DriverManager.getConnection(connectionUrl);
                GlobalVar.statement = GlobalVar.conn.createStatement();
                // Iterate through the data in the result set and display it.
                System.out.println(GeneralHelper.bytesToStringHex(GlobalVar.fullNodeRestClient.getMedicalOrgIdentifier()));
                SceneManager.showMainMenuScene();
            } catch (Exception e){
                invalidWarning.visibleProperty().setValue(true);
                invalidWarning.setText(Config.WRONG_WARNING);
            }
        });

        VBox vbox = new VBox(15); // spacing = 8
        HBox hBox1 = new HBox(20.0, SQLlocationLabel, SQLlocation);
        hBox1.setAlignment(Pos.CENTER);
        HBox hBox2 = new HBox(20.0, DBnameLabel, DBname);
        hBox2.setAlignment(Pos.CENTER);
        HBox hBox3 = new HBox(20.0, SQLusernameLabel, SQLusername);
        hBox3.setAlignment(Pos.CENTER);
        HBox hBox4 = new HBox(20.0, SQLpwLabel, SQLpw);
        hBox4.setAlignment(Pos.CENTER);
        HBox hBox5 = new HBox(20.0, usernameLabel, username);
        hBox5.setAlignment(Pos.CENTER);
        HBox hBox6 = new HBox(20.0, passwordLabel, password);
        hBox6.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(
                title,
                hBox1,
                hBox2,
                hBox3,
                hBox4,
                hBox5,
                hBox6,
                invalidWarning,
                submitBtn
        );
        vbox.setAlignment(Pos.CENTER);
        invalidWarning.setStyle("-fx-text-fill:red;-fx-font-size:15;");
        SQLlocation.setText("localhost:1433");
        DBname.setText("Customer");
        SQLusername.setText("root");
        SQLpw.setText("1234");
        username.setText("user");
        password.setText("1234");
        scene.setRoot(vbox);

    }

}