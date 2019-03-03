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

//patient registration
//Get Patient Key
//
public class DoctorMainPane  {

    private static final Label PatientList = new Label("Registered Patients");
    private static final Button WriteButton = new Button("Write Record");
    private static final Button RefreshButton = new Button("", new ImageView(new Image("images/menu/refresh.png", 30,30, true, true)));
    private static final ToggleGroup toggleGroup = new ToggleGroup();
    private static final VBox List = new VBox(0);
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
    }

    public void connect_components() {

        List.setPrefSize(400,400);
        List.setAlignment(Pos.CENTER);

        ScrollPane sp = new ScrollPane();
        sp.setStyle("-fx-background-color:transparent");
        sp.setContent(List);
        sp.setFitToWidth(true);
        sp.setPrefHeight(400.0);

        RefreshButton.setStyle("-fx-pref-width:60");

        HBox hbox = new HBox( 20);
        hbox.getChildren().addAll(PatientList, RefreshButton);
        hbox.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(20);
        vbox.getChildren().addAll(hbox, sp, WriteButton);
        vbox.setAlignment(Pos.CENTER);
        vbox.setMaxSize(400,700);

        scene.setRoot(vbox);
        scene.getStylesheets().add("css/styles.css");

    }

}