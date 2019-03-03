import general.utility.Helper;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import viewmodel.Config;
import java.sql.ResultSet;
import java.sql.SQLException;

//patient registration
//Get Patient Key
//
public class MainMenuPane  {

    private static final Button[] userButtons = {new Button("Patient Registration"), new Button("Get Patient Key"), new Button("Delete Patient")};
    private static final Image[] userImages = {
            new Image("images/menu/registration.png", Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
            new Image("images/menu/modifyRecord.png", Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
    };
    private static final ToggleGroup PatientToggleGroup = new ToggleGroup();
    public static Scene scene = new Scene(new Group(), Config.WIDTH, Config.HEIGHT);
    private static VBox Record_List = new VBox(0);

    public void AddPatientToList(String name, String ID, byte[] keyencoded) {
        ToggleButton p1 = new ToggleButton(name + " - " + ID);
        p1.setUserData(name + " - " + ID);
        p1.setId(Helper.encode(keyencoded));
        System.out.println("ID : " + p1.getId());
        p1.setToggleGroup(PatientToggleGroup);
        Record_List.getChildren().add(p1);
        String SQL = "if (select top 1 ID from PatientList where PatientName = '" + name + "' and " +
                " ID = '" + ID + "' ) is null " +
                " insert into PatientList (PatientName, ID)\n" +
                " values ('" +
                name +
                "','" +
                ID +
                "')";
        try {
            GlobalVar.statement.executeUpdate(SQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void DelectPatient(String nameID) {
        for (int i = 0; i < Record_List.getChildren().size(); i++)
        {
            if (nameID.equals(Record_List.getChildren().get(i).getUserData().toString()))
            {
                Record_List.getChildren().remove(i);
                String name = nameID.substring(0, nameID.indexOf(" - "));
                String ID = nameID.substring(nameID.indexOf(" - ") + 3);
                System.out.println("delete patient " + " name :" + name + "*******ID :" + ID);
                String SQL = "delete from PatientList where PatientName = '" +
                        name +
                        "' and ID = '" +
                        ID +
                        "'";
                try {
                    GlobalVar.statement.executeUpdate(SQL);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String[] NameID_List() {
        System.out.println("checking2");
        String[] list = new String[Record_List.getChildren().size()];
        for (int i = 0; i < Record_List.getChildren().size(); i++)
        {
            list[i] = Record_List.getChildren().get(i).getUserData().toString();
        }
        return list;
    }

    public static void init()
    {
        userButtons[0].setOnAction(event -> {
            SceneManager.showScanScene(ScanPane.TYPE.PUBLIC_KEY);
        });
        userButtons[1].setOnAction(event -> {
            if(PatientToggleGroup.getSelectedToggle() != null)
            {
                String NameID = PatientToggleGroup.getSelectedToggle().getUserData().toString();
                ToggleButton p1 = (ToggleButton) PatientToggleGroup.getSelectedToggle();
                SceneManager.selected = NameID;
                System.out.println("ID : " + p1.getId());
                SceneManager.setEncodedEC(Helper.decode(p1.getId()));
                String name = NameID.substring(0, NameID.indexOf(" - "));
                String ID = NameID.substring(NameID.indexOf(" - ") + 3);
                String record_String = "";
                String SQL = "SELECT * FROM Customer where NewRecords = 1 and ID = '" +
                        ID +
                        "' and PatientName = '" +
                        name +
                        "'";
                try {
                    ResultSet rs = GlobalVar.statement.executeQuery(SQL);
                    int i = 0;
                    while (rs.next()) {
                        if ( i == 0 )
                        {
                            record_String = record_String + rs.getString("Records");
                            i = 1;
                        }
                        else record_String = record_String + "\n" + rs.getString("Records");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (record_String.equals(""))
                {
                    Alert alert = new Alert(Alert.AlertType.NONE);
                    alert.setTitle("Information Dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("No Record!");
                    DialogPane dialogPane = alert.getDialogPane();
                    dialogPane.getStylesheets().add(Config.CSS_STYLES);
                    Stage stage = (Stage) dialogPane.getScene().getWindow();
                    stage.getIcons().add(new Image("images/icon.png"));
                    alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
                    alert.showAndWait();
                }
                else {
                    SceneManager.setRecord_string(record_String);
                    SceneManager.showScanScene(ScanPane.TYPE.SECRET_KEY);
                }
            }
        });
        userButtons[2].setOnAction(event -> {
            DelectPatient(PatientToggleGroup.getSelectedToggle().getUserData().toString());
        });
        HBox hBox1 = new HBox(50);
        hBox1.setAlignment(Pos.CENTER);
        for (int i = 0; i < userImages.length; i++){
            VBox tempVBox = null;
            if (i == 1)
                tempVBox = new VBox(30.0,new ImageView(userImages[i]), userButtons[i], userButtons[i+1]);
            else
                tempVBox = new VBox(30.0,new ImageView(userImages[i]), userButtons[i]);
            tempVBox.setAlignment(Pos.CENTER);
            hBox1.getChildren().add(tempVBox);
        }

        Record_List.setPrefSize(400,400);
        Record_List.setAlignment(Pos.CENTER);

        ScrollPane scrollPane_List = new ScrollPane();
        scrollPane_List.setStyle("-fx-background-color:transparent");
        scrollPane_List.setContent(Record_List);
        scrollPane_List.setFitToWidth(true);
        scrollPane_List.setPrefHeight(700.0);
        VBox vbox_with_Label_ScrollPane = new VBox(20);
        Label test2 = new Label("Registered Patients");
        test2.setAlignment(Pos.CENTER);
        vbox_with_Label_ScrollPane.getChildren().add(test2);
        vbox_with_Label_ScrollPane.getChildren().add(scrollPane_List);
        vbox_with_Label_ScrollPane.setAlignment(Pos.CENTER);
        vbox_with_Label_ScrollPane.setMaxSize(400,700);
        hBox1.getChildren().add(vbox_with_Label_ScrollPane);

        scene.setRoot(hBox1);
    }
}