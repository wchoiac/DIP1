import javafx.application.Application;
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
import java.util.ArrayList;

//patient registration
//Get Patient Key
//
public class DoctorMainPane  {

    private static final Label PatientList = new Label("Registered Patients");
    private static final Label AllPatient = new Label("Patients Record");
    private static final Label nul = new Label("");
    private static final Button WriteButton = new Button("Write Record");
    private static final Button ShowButton = new Button("Show Record");
    private static final Button RefreshButton = new Button("", new ImageView(new Image("images/menu/refresh.png", 30,30, true, true)));
    private static final ToggleGroup toggleGroup = new ToggleGroup();
    private static final ToggleGroup toggleGroup2 = new ToggleGroup();
    private static final VBox List = new VBox(0);
    private static final VBox AllRecordsList = new VBox(0);
    private static final Image[] userImages = {
            new Image("images/menu/registration.png", Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
            new Image("images/menu/modifyRecord.png", Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
    };
    public static Scene scene = new Scene(new Group(), Config.WIDTH, Config.HEIGHT);
    private static String selected = "";

    public void init() {
        connect_components();
        setbuttonfunction();
    }

    public void setbuttonfunction() {
        RefreshButton.setOnAction(event -> {
            updateList();
        });

        WriteButton.setOnAction(event -> {
            if (toggleGroup.getSelectedToggle() != null)
            {
                SceneManager.selected = toggleGroup.getSelectedToggle().getUserData().toString();
                SceneManager.showRecordScene();
            }
        });

        ShowButton.setOnAction(event -> {
            if (toggleGroup2.getSelectedToggle() != null)
            {
                SceneManager.selected = toggleGroup2.getSelectedToggle().getUserData().toString();
                System.out.println(toggleGroup2.getSelectedToggle().getUserData().toString());
                SceneManager.showRecordScene();
            }
        });

    }

    public void updateList(){
        List.getChildren().clear();
        String SQL = "SELECT * FROM PatientList";
        try {
            ResultSet rs = SceneManager.statement.executeQuery(SQL);
            while (rs.next()) {
                String nameID = rs.getString("PatientName") + " - " + rs.getString("ID");
                ToggleButton p1 = new ToggleButton(nameID);
                p1.setUserData(nameID);
                p1.setToggleGroup(toggleGroup);
                List.getChildren().add(p1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        AllRecordsList.getChildren().clear();
        SQL = "SELECT * FROM Customer where PatientName is not null order by Timestamp";
        ArrayList<String> AllnameID = new ArrayList<>();
        try {
            ResultSet rs = SceneManager.statement.executeQuery(SQL);
            whileloop:
            while (rs.next()) {
                String nameID = rs.getString("PatientName") + " - " +
                        rs.getString("ID") + " ^ " +
                        rs.getString("patientIdentifier");
                System.out.println(nameID);
                for (int i = 0; i < AllnameID.size(); i++){
                    if(AllnameID.get(i).contains(rs.getString("patientIdentifier"))){
                        AllnameID.set(i, nameID);
                        continue whileloop;
                    }
                }
                AllnameID.add(nameID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < AllnameID.size(); i++){
            String nameID = AllnameID.get(i).substring(0, AllnameID.get(i).indexOf(" ^ "));
            ToggleButton p1 = new ToggleButton(nameID);
            p1.setUserData(AllnameID.get(i));
            p1.setToggleGroup(toggleGroup2);
            AllRecordsList.getChildren().add(p1);
        }
    }



    public void connect_components() {

        List.setPrefSize(400,400);
        List.setAlignment(Pos.CENTER);

        AllRecordsList.setPrefSize(400,400);
        AllRecordsList.setAlignment(Pos.CENTER);

        ScrollPane sp = new ScrollPane();
        sp.setStyle("-fx-background-color:transparent");
        sp.setContent(List);
        sp.setFitToWidth(true);
        sp.setPrefHeight(400.0);

        ScrollPane sp2 = new ScrollPane();
        sp2.setStyle("-fx-background-color:transparent");
        sp2.setContent(AllRecordsList);
        sp2.setFitToWidth(true);
        sp2.setPrefHeight(400.0);

        RefreshButton.setStyle("-fx-pref-width:60");

        HBox hbox = new HBox( 20);
        hbox.getChildren().add(PatientList);
        hbox.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(20);
        vbox.getChildren().addAll(hbox, sp, WriteButton);
        vbox.setAlignment(Pos.CENTER);
        vbox.setMaxSize(400,700);

        HBox hbox2 = new HBox(20);
        hbox2.getChildren().add(AllPatient);
        hbox2.setAlignment(Pos.CENTER);

        VBox vbox2 = new VBox(20);
        vbox2.getChildren().addAll(hbox2, sp2, ShowButton);
        vbox2.setAlignment(Pos.CENTER);
        vbox2.setMaxSize(400,700);

        VBox vbox3 = new VBox(50);
        vbox3.getChildren().add(nul);
        vbox3.getChildren().add(RefreshButton);
        vbox3.setAlignment(Pos.TOP_CENTER);
        vbox3.setMaxSize(400,700);

        HBox hbox0 = new HBox(40);
        hbox0.getChildren().add(vbox);
        hbox0.getChildren().add(vbox2);
        hbox0.getChildren().add(vbox3);
        hbox0.setAlignment(Pos.CENTER);

        scene.setRoot(hbox0);
        scene.getStylesheets().add("css/styles.css");

    }

}