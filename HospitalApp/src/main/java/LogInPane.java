import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import viewmodel.Config;
import com.github.kittinunf.fuel.core.FuelManager;

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

    private static final Label CertLabel = new Label("Certificate");
    private static final Label getCertLabel = new Label("");
    private static final Button CertImport = new Button("Import Certificate");

    private static final Label IPlabel = new Label("IP Address");
    private static final TextField IPtext = new TextField();
    private static final CheckBox ssl = new CheckBox("TLS/SSL");

    private static final Label SQLlocationLabel = new Label("SQL Server location");
    private static final TextField SQLlocation = new TextField();
    private static final Label DBnameLabel = new Label("Database");
    private static final TextField DBname = new TextField();
    private static final Label SQLusernameLabel = new Label("Database Username");
    private static final TextField SQLusername = new TextField();
    private static final Label SQLpwLabel = new Label("Database Password");
    private static final PasswordField SQLpw = new PasswordField();
    private static File file = null;

    public static Scene scene = new Scene(new Group(), Config.WIDTH / 2, Config.HEIGHT / 2);

    public static void login() throws Exception{
        InetAddress inetAddress = InetAddress.getByName(IPtext.getText());
        GlobalVar.fullNodeRestClient = new FullNodeRestClient(inetAddress, SecurityHelper.getX509FromDER(file));
        GlobalVar.fullNodeRestClient.login(username.getText(), password.getText().toCharArray());
    }

    public static void SQLlogin() throws Exception{
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
    }

    {
        CertImport.getStyleClass().add("small-button");
        container.setAlignment(Pos.CENTER);
        username.setPromptText("Username");
        password.setPromptText("Password");
        IPtext.setPromptText("IP Address");
        invalidWarning.visibleProperty().setValue(false);

        CertImport.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Certificate File");
            file = fileChooser.showOpenDialog(SceneManager.getStage());
            if(file != null)
            if (file.exists()){
                System.out.println(file);
                String fileName = file.toString().substring(file.toString().lastIndexOf('\\') + 1);
                getCertLabel.setText(fileName);
            }
        });


        submitBtn.setOnAction(event -> {
            if(file == null){
                invalidWarning.visibleProperty().setValue(true);
                invalidWarning.setText(Config.WRONG_CET);
            } else if(username.getText().isEmpty()){
                invalidWarning.visibleProperty().setValue(true);
                invalidWarning.setText(Config.USERNAME_WARNING);
            } else if(password.getText().isEmpty()) {
                invalidWarning.visibleProperty().setValue(true);
                invalidWarning.setText(Config.PASSWORD_WARNING);
            } else if(IPtext.getText().isEmpty()){
                invalidWarning.visibleProperty().setValue(true);
                invalidWarning.setText(Config.IP_WARNING);
            }


            try{
                login();
                System.out.println(GeneralHelper.bytesToStringHex(GlobalVar.fullNodeRestClient.getMedicalOrgIdentifier()));
                try {
                    SQLlogin();
                    SceneManager.MainMenuInit();
                    SceneManager.showMainMenuScene();
                } catch (Exception e) {
                    e.printStackTrace();
                    invalidWarning.visibleProperty().setValue(true);
                    invalidWarning.setText(Config.WRONG_SQL_WARNING);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        });


        VBox vbox = new VBox(12); // spacing = 8
        HBox hBox0 = new HBox(20.0, CertLabel, CertImport, getCertLabel);
        hBox0.setAlignment(Pos.CENTER);
        HBox hBox01 = new HBox(20.0, IPlabel, IPtext);
        hBox01.setAlignment(Pos.CENTER);
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
                hBox0,
                hBox01,
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
        IPtext.setText("25.58.208.225");
        SQLlocation.setText("25.58.208.225");
        DBname.setText("Customer");
        SQLusername.setText("SA");
        SQLpw.setText("Fyp123456");
        username.setText("user");
        password.setText("1234");
        scene.setRoot(vbox);

    }

}