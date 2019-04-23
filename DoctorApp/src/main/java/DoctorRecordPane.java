import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import viewmodel.Config;

import javax.naming.Name;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

//patient registration
//Get Patient Key
//
public class DoctorRecordPane  {

    private static TextArea input = new TextArea();
    private static Button createButton = new Button("Create");
    private static Button cancelButton = new Button("Cancel");
    public static Scene scene = new Scene(new Group(), Config.WIDTH, Config.HEIGHT);
    private static String name = null;
    private static String ID = null;
    private static String Gender = null;
    private static String Birth = null;
    private static String address = null;
    private static String phoneNum = null;
    private static String nationality = null;
    private static String extra = null;
    private static String patientID = null;

    public void init() {
        setButtonFunction();
    }
    private void clear() {
        name = null;
        ID = null;
        Gender = null;
        Birth = null;
        address = null;
        phoneNum = null;
        nationality = null;
        extra = null;
        patientID = null;
        input.setText(null);
    }

    private void setButtonFunction() {
        createButton.setOnAction(event -> {
            if (input.getText() == null || input.getText().trim().isEmpty()){
                System.out.println("No record is created");
            } else {
                String SQL = "insert into Customer (patientIdentifier, MedName, Records," +
                        " NewRecords) \n" +
                        "values ('" +
                        patientID +
                        "' , 'local','" +
                        input.getText() +
                        "', 1 )";
                try {
                    SceneManager.statement.executeUpdate(SQL);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            clear();
            SceneManager.updateList();
            SceneManager.showMainMenuScene();
        });
        cancelButton.setOnAction(event -> {
            clear();
            SceneManager.updateList();
            SceneManager.showMainMenuScene();
        });
    }

    public void loadinfo(){
        String SQL = "SELECT * FROM Customer where PatientName = '" +
                name +
                "' and ID = '" +
                ID + "' and records is null and NewRecords is null " +
                "order by Timestamp";
        ResultSet rs = null;
        try {
            rs = SceneManager.statement.executeQuery(SQL);
            while (rs.next()) {
                String timestamp = rs.getString("Timestamp");
                Gender = rs.getString("Gender");
                Birth = rs.getString("Date of birth");
                address = rs.getString("address");
                phoneNum = rs.getString("phoneNum");
                nationality = rs.getString("nationality");
                extra = rs.getString("extra");
                patientID = rs.getString("patientIdentifier");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void loadrecords(String nameID) {
        name = nameID.substring(0, nameID.indexOf(" - "));
        ID = nameID.substring(nameID.indexOf(" - ") + 3, nameID.indexOf(" ^ "));
        loadinfo();
        String SQL = "SELECT * FROM Customer where patientIdentifier = '" +
                patientID +
                "' and NewRecords is not null order by Timestamp";
        String records_data = "";
        try {
            ResultSet rs = SceneManager.statement.executeQuery(SQL);
            while (rs.next()) {

                String timestring = null;
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                if(rs.getString("Timestamp") != null)
                    timestring = dateFormat.format(new Timestamp(Long.parseLong(rs.getString("Timestamp"))));
                else timestring = "    ";
                records_data = records_data + "From " + rs.getString("MedName") + "    " + timestring + "\n" +
                        rs.getString("Records") + "\n\n";
            }
            if(!records_data.equals(""))
            records_data = records_data.substring(0, records_data.length() - 2);
            System.out.println(records_data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        HBox tophBox = new HBox(50);
        tophBox.setAlignment(Pos.CENTER);

        Label Records = new Label(records_data);
        Records.setDisable(true);
        Records.setWrapText(true);
        Records.setStyle("-fx-opacity: 1.0;-fx-font-size: 20.0");
        Records.setPrefWidth(450);
        Records.setMinHeight(650);

        ScrollPane sp = new ScrollPane();
        sp.setStyle("-fx-background-color:transparent");
        sp.setContent(Records);
        sp.setFitToWidth(true);
        sp.setPrefHeight(680.0);
        VBox leftvbox = new VBox(20);
        Label test2 = new Label("Records");
        test2.setAlignment(Pos.CENTER);
        leftvbox.getChildren().add(test2);
        leftvbox.getChildren().add(sp);

        leftvbox.setAlignment(Pos.CENTER);
        leftvbox.setMaxSize(600,700);
        tophBox.getChildren().add(leftvbox);

        input.setStyle("-fx-font-size: 20.0");
        input.setMinHeight(700);
        input.setPrefWidth(450);

        VBox midvbox = new VBox(20);
        Label NewRecord = new Label("New Record");
        NewRecord.setAlignment(Pos.CENTER);
        midvbox.getChildren().add(NewRecord);
        midvbox.getChildren().add(input);
        midvbox.setAlignment(Pos.CENTER);
        tophBox.getChildren().add(midvbox);


        VBox rightvbox = new VBox(20);
        rightvbox.setMaxSize(450,700);
        Label patientInformation = new Label("Patient information");
        patientInformation.setAlignment(Pos.CENTER);
        rightvbox.setAlignment(Pos.CENTER);
        Label text2 = new Label(
                "\n\n\n\n\nName : " + name + "\n\n" +
                        "Gender : " + Gender + "\n\n" +
                        "Nationality : " + nationality + "\n\n" +
                        "Date of Birth : " + Birth + "\n\n" +
                        "Address : " + address + "\n\n" +
                        "Phone : " + phoneNum + "\n\n" +
                        "Addition information : " + extra + "\n\n"
        );
        text2.setAlignment(Pos.TOP_LEFT);
        text2.setPrefSize(450,680);
        rightvbox.getChildren().add(patientInformation);
        rightvbox.getChildren().add(text2);
        tophBox.getChildren().add(rightvbox);

        HBox bottomhBox = new HBox(20, createButton, cancelButton);
        bottomhBox.setAlignment(Pos.CENTER);

        VBox con = new VBox(20);
        con.setAlignment(Pos.CENTER);
        con.getChildren().add(tophBox);
        con.getChildren().add(bottomhBox);


        scene.setRoot(con);
        scene.getStylesheets().add("css/styles.css");
    }

}