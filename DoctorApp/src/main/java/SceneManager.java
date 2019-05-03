import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import viewmodel.Config;

import java.sql.Connection;
import java.sql.Statement;

public class SceneManager {
    private static LogInPane logInPane = new LogInPane();
    private static DoctorMainPane doctorMainPane = new DoctorMainPane();
    private static DoctorRecordPane doctorRecordPane = new DoctorRecordPane();
    private static Stage stage = null;
    public static Connection conn = null;
    public static Statement statement = null;
    public static String selected = null;

    public static void init(){
        Scene[] scenes = {logInPane.scene, doctorMainPane.scene, doctorRecordPane.scene};
        logInPane.init();
        doctorRecordPane.init();
        doctorMainPane.init();
        addStylesheets(scenes);
    }

    public static void addStylesheets(Scene[] scenes){
        for (Scene scene: scenes) {
            scene.getStylesheets().add(Config.CSS_STYLES);
        }
    }

    public static void setStage(Stage stage_arg) {
        stage = stage_arg;
        stage.setTitle("MediRec");
        stage.getIcons().add(new Image("images/icon.png"));
        stage.setOnCloseRequest(event -> {
            System.out.println("exiting....");
        });
    }

    private static void showScene(Scene scene_arg){
        if (stage == null)
            return;
        stage.hide();
        stage.setScene(scene_arg);
        stage.show();
    }

    public static void showLogInScene(){
        showScene(logInPane.scene);
    }

    public static void showMainMenuScene(){
        showScene(doctorMainPane.scene);
    }

    public static void updateList() {
        doctorMainPane.updateList();
    }

    public static void showRecordScene(){
        doctorRecordPane.loadrecords(selected);
        selected = null;
        showScene(doctorRecordPane.scene);
    }



}