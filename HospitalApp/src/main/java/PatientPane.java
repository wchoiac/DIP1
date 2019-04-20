import blockchain.BlockChainSecurityHelper;
import exception.BadRequest;
import exception.NotFound;
import exception.ServerError;
import exception.UnAuthorized;
import general.utility.GeneralHelper;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pojo.*;
import viewmodel.Config;
import xyz.medirec.medirec.pojo.KeyTime;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.sql.SQLException;
import java.util.*;

import general.utility.Helper;

public class PatientPane {
    public static Scene scene = new Scene(new Group(), Config.WIDTH, Config.HEIGHT);
    private static KeyTime keyTime = null;
    private static final Button[] userButtons1 = {new Button("Generate Selected Timestamps")};
    private static final Button[] userButtons2 = {new Button("Confirm"), new Button("Cancel")};
    private static final Image Tick =
            new Image("images/menu/tick.png", 22,22, true, true);

    private static final Image QRcode =
            new Image("images/menu/QRcode.png", 300,300, true, true);
    private static final ImageView QR = new ImageView(QRcode);
    private static final Image[] userImages = {
            new Image("images/menu/registration.png", Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true)
    };
    private static ArrayList<Long> recordtimestamps = new ArrayList<Long>();
    private static ArrayList<Long> patienttimestamps = new ArrayList<Long>();
    private static long[] Timelist = null;
    private static String Name = null;
    private static String Gender = null;
    private static String Birth = null;
    private static String ID = null;
    private static String address = null;
    private static String phoneNum = null;
    private static String nationality = null;
    private static String extra = null;

    private static ECPublicKey PKEY = null;

    private static ArrayList<String> Records = new ArrayList<String>();
    private static ArrayList<Long> Timestamp = new ArrayList<Long>();
    private static ArrayList<String> MedName = new ArrayList<String>();

    public static void setKeyTime(KeyTime input) {
        keyTime = input;
    }

    public static void eraseData(){
        Name = null;
        Gender = null;
        Birth = null;
        ID = null;
        address = null;
        phoneNum = null;
        nationality = null;
        extra = null;
        Records.clear();
        Timestamp.clear();
        MedName.clear();
        recordtimestamps.clear();
        patienttimestamps.clear();
        PKEY = null;
        Timelist = null;
    }

    public static boolean checkRegistered(String nameID){
        System.out.println("checking");
        String[] list = SceneManager.CheckPatient();
        for (int i = 0; i < list.length; i ++)
            if( list[i].contains(nameID))
            {
                Alert alert = new Alert(Alert.AlertType.NONE);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("Patient already registered!");
                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.getStylesheets().add(Config.CSS_STYLES);
                Stage stage = (Stage) dialogPane.getScene().getWindow();
                stage.getIcons().add(
                        new Image("images/icon.png"));
                alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
                alert.showAndWait();
                return true;
            }
        return false;
    }

    public static boolean retrieveData(){
        byte[] encoded_public_key = keyTime.getPubKeyEncoded();
        byte[][] encoded_secret_key = keyTime.getSecretKeysEncoded();
        Timelist = keyTime.getTimeList();

        KeyFactory keyFactory = null;
        SecretKey AESkey = null;
        try {
            keyFactory = KeyFactory.getInstance("EC");
            PKEY = (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(encoded_public_key));
        } catch (Exception e1) {
        }
        PatientShortInfoPojo[] Patientinfo = null;
        RecordShortInfoPojo[] Recordinfo = null;
        try {
            Patientinfo = GlobalVar.fullNodeRestClient.getPatientShortInfoList(PKEY);
            System.out.println("PatientInfo pojo size: " + Patientinfo.length);
            Recordinfo = GlobalVar.fullNodeRestClient.getRecordShortInfoList(PKEY);
            System.out.println("Recordinfo pojo size: " + Recordinfo.length);
            ArrayList<LocationPojo> patient_location_pojo = new ArrayList<LocationPojo>();
            ArrayList<LocationPojo> record_location_pojo = new ArrayList<LocationPojo>();

            for (int i = 0; i < Recordinfo.length; i++)
            {
                System.out.println("Add Recordinfo");
                System.out.println(Recordinfo[i].getTimestamp());
                recordtimestamps.add(Recordinfo[i].getTimestamp());
                record_location_pojo.add(Recordinfo[i].getLocationPojo());
            }
            for (int i = 0; i < Patientinfo.length; i++)
            {
                System.out.println("Add PatientInfo");
                System.out.println(Patientinfo[i].getTimestamp());
                patienttimestamps.add(Patientinfo[i].getTimestamp());
                patient_location_pojo.add(Patientinfo[i].getLocation());
            }

            System.out.println("List all timestamp of AES************");
            for (int i = 0; i < Timelist.length; i++){
                System.out.println(Timelist[i]);
            }

            PatientInfoContentPojo[] patientinfo_content = GlobalVar.fullNodeRestClient.getPatientInfoContentsList(patient_location_pojo);
            String patient_info_string = "Patient Infomation : \n";
            for (int i = 0; i < Patientinfo.length; i++) {
                for (int j = 0; j < Timelist.length; j++) {
                    System.out.println(" - - - - - - - - - - - - - -- - - -- -");
                    System.out.println(Timelist[j]);
                    System.out.println(encoded_secret_key[j]);
                    if (Timelist[j] == (Patientinfo[i].getTimestamp())) {
                        System.out.println("RUNNING Patient AES ************* " + i);
                        AESkey = new SecretKeySpec(encoded_secret_key[j], "AES");

                        System.out.println("******************************* //////");
                        System.out.println(AESkey);
                        System.out.println(Arrays.toString(encoded_secret_key[j]));
                        System.out.println("******************************* //////");

                        byte[] decrypted_data = Helper.AESdecrypt(patientinfo_content[i].getEncryptedInfo(), AESkey);
                        String orginial_data = new String(decrypted_data);
                        patient_info_string = patient_info_string + orginial_data + '\n';
                        System.out.println(patient_info_string);
                    }
                }
            }

            if (patient_info_string.equals("Patient Infomation : \n"))
                return false;

            Name = patient_info_string.substring(
                    patient_info_string.lastIndexOf("name\":") + 7,
                    patient_info_string.indexOf('"', patient_info_string.lastIndexOf("name\":") + 7)
            );
            System.out.println("Name : " + Name);

            Gender = patient_info_string.substring(
                    patient_info_string.lastIndexOf("gender\":") + 9,
                    patient_info_string.indexOf('"', patient_info_string.lastIndexOf("gender\":") + 9)
            );
            System.out.println("Gender : " + Gender);

            Birth = patient_info_string.substring(
                    patient_info_string.lastIndexOf("birthDate\":") + 12,
                    patient_info_string.indexOf('"', patient_info_string.lastIndexOf("birthDate\":") + 12)
            );
            System.out.println("Birth : " + Birth);

            ID = patient_info_string.substring(
                    patient_info_string.lastIndexOf("identificationNumber\":") + 23,
                    patient_info_string.indexOf('"', patient_info_string.lastIndexOf("identificationNumber\":") + 23)
            );
            System.out.println("ID : " + ID);

            address = patient_info_string.substring(
                    patient_info_string.lastIndexOf("address\":") + 10,
                    patient_info_string.indexOf('"', patient_info_string.lastIndexOf("address\":") + 10)
            );
            System.out.println("address : " + address);

            phoneNum = patient_info_string.substring(
                    patient_info_string.lastIndexOf("phoneNum\":") + 11,
                    patient_info_string.indexOf('"', patient_info_string.lastIndexOf("phoneNum\":") + 11)
            );
            System.out.println("phoneNum : " + phoneNum);

            nationality = patient_info_string.substring(
                    patient_info_string.lastIndexOf("nationality\":") + 14,
                    patient_info_string.indexOf('"', patient_info_string.lastIndexOf("nationality\":") + 14)
            );
            System.out.println("nationality : " + nationality);

            extra = patient_info_string.substring(
                    patient_info_string.lastIndexOf("extra\":") + 8,
                    patient_info_string.indexOf('"', patient_info_string.lastIndexOf("extra\":") + 8)
            );
            System.out.println("extra : " + extra);


            if(checkRegistered(Name + " - " + ID) == true)
            {
                eraseData();
                SceneManager.showMainMenuScene();
                return false;
            }

            RecordContentPojo[] record_content = GlobalVar.fullNodeRestClient.getRecordContentsList(record_location_pojo);
            for (int i = 0; i < Recordinfo.length; i++) {
                for (int j = 0; j < Timelist.length; j++) {
                    if (Timelist[j] == Recordinfo[i].getTimestamp()) {
                        System.out.println("RUNNING AES ************* " + i);
                        AESkey = new SecretKeySpec(encoded_secret_key[j], "AES");
                        byte[] decrypted_data = Helper.AESdecrypt(record_content[i].getEncryptedRecord(), AESkey);
                        String orginial_data = new String(decrypted_data);
                        Timestamp.add(Recordinfo[i].getTimestamp());
                        Records.add(orginial_data);
                        MedName.add(Recordinfo[i].getMedicalOrgName());
                        System.out.println( "From : " +
                                Recordinfo[i].getMedicalOrgName() + "   " +
                                "\n" +
                                orginial_data + '\n');
                    }
                }
            }

        } catch (UnAuthorized e1) {
            System.out.println("Patient UnAuthorized error");return false;
        } catch (NotFound e2) {System.out.println("Patient NotFound error");return false;}
        catch (BadRequest e3) {System.out.println("Patient BadRequest error");return false;}
        catch (ServerError e4) {System.out.println("Patient ServerError error");return false;}
        catch (Exception e5){e5.printStackTrace(); return false;}
        return true;
    }

    public static void init()
    {

        HBox hBox1 = new HBox(50);
        hBox1.setAlignment(Pos.CENTER);

        VBox Record_List = new VBox(0);
        Record_List.setPrefSize(400,400);
        recordouterloop:
        for (int i = 0; i < recordtimestamps.size(); i++)
        {
            for (int j = 0; j < Timelist.length; j++) {
                if (Timelist[j] == recordtimestamps.get(i)) {
                    ToggleButton p1 = new ToggleButton("Medical Record " + recordtimestamps.get(i) + "     ", new ImageView(Tick));
                    p1.setStyle("-fx-pref-width:420;-fx-pref-height:60");
                    p1.setAlignment(Pos.CENTER_LEFT);
                    p1.setContentDisplay(ContentDisplay.RIGHT);
                    p1.setUserData(recordtimestamps.get(i));
                    p1.setSelected(false);
                    p1.setDisable(true);
                    Record_List.getChildren().add(p1);
                    continue recordouterloop;
                }
            }
            ToggleButton p1 = new ToggleButton("Medical Record " + recordtimestamps.get(i));
            p1.setStyle("-fx-pref-width: 420;-fx-pref-height: 60");
            p1.setContentDisplay(ContentDisplay.RIGHT);
            p1.setAlignment(Pos.CENTER_LEFT);
            p1.setUserData(recordtimestamps.get(i));
            Record_List.getChildren().add(p1);

        }

        patientouterloop:
        for (int i = 0; i < patienttimestamps.size(); i++)
        {
            for (int j = 0; j < Timelist.length; j++) {
                if (Timelist[j] == patienttimestamps.get(i)) {
                    ToggleButton p1 = new ToggleButton("Patient Information " + patienttimestamps.get(i) + "     ", new ImageView(Tick));
                    p1.setStyle("-fx-pref-width:420;-fx-pref-height:60");
                    p1.setAlignment(Pos.CENTER_LEFT);
                    p1.setContentDisplay(ContentDisplay.RIGHT);
                    p1.setDisable(true);
                    p1.setSelected(false);
                    p1.setUserData(patienttimestamps.get(i));
                    Record_List.getChildren().add(p1);
                    continue patientouterloop;
                }
            }
            ToggleButton p1 = new ToggleButton("Patient Information " + patienttimestamps.get(i));
            p1.setStyle("-fx-pref-width:420;-fx-pref-height: 60");
            p1.setContentDisplay(ContentDisplay.RIGHT);
            p1.setAlignment(Pos.CENTER_LEFT);
            p1.setUserData(patienttimestamps.get(i));
            Record_List.getChildren().add(p1);
        }

        Record_List.setAlignment(Pos.CENTER);


        ScrollPane scrollPane_List = new ScrollPane();
        scrollPane_List.setStyle("-fx-background-color:transparent");
        scrollPane_List.setContent(Record_List);
        scrollPane_List.setFitToWidth(true);
        scrollPane_List.setPrefHeight(680.0);
        VBox vbox_with_Label_ScrollPane = new VBox(20);
        Label Record_label = new Label("Records");
        Record_label.setAlignment(Pos.CENTER);
        vbox_with_Label_ScrollPane.getChildren().add(Record_label);
        vbox_with_Label_ScrollPane.getChildren().add(scrollPane_List);
        vbox_with_Label_ScrollPane.setAlignment(Pos.CENTER);
        vbox_with_Label_ScrollPane.setMaxSize(420,700);
        hBox1.getChildren().add(vbox_with_Label_ScrollPane);

        VBox vbox2 = new VBox(20);
        vbox2.setMaxSize(400,700);
        Label label2 = new Label("Patient information");
        label2.setAlignment(Pos.CENTER);
        vbox2.setAlignment(Pos.CENTER);
        Label text2 = new Label(
                "\n\n\n\n\nName : "+ Name + "\n\n" +
                "Gender : " + Gender +"\n\n" +
                "Date of Birth : " + Birth + "\n\n" +
                "ID Number : " + ID + "\n\n" +
                "Address : " + address + "\n\n"+
                "Phone number : " + phoneNum + "\n\n"+
                "Nationality : " + nationality + "\n\n"+
                "Additional information : " + extra + "\n\n"
        );
        text2.setAlignment(Pos.TOP_LEFT);
        text2.setPrefSize(400,680);
        vbox2.getChildren().add(label2);
        vbox2.getChildren().add(text2);
        hBox1.getChildren().add(vbox2);

        VBox vbox3 = new VBox(20);
        vbox3.setMaxSize(400,680);
        vbox3.setPrefSize(400,680);
        Label label3 = new Label("QR code of Timestamps");
        label3.setAlignment(Pos.CENTER);
        vbox3.setAlignment(Pos.CENTER);
        vbox3.getChildren().add(label3);
        vbox3.getChildren().add(QR);

        HBox hbox3n1 = new HBox(20);
        hbox3n1.setAlignment(Pos.CENTER);
        userButtons1[0].setOnAction(event -> {
            String str = "";
            for (int i = 0; i < Record_List.getChildren().size(); i++)
                if (((ToggleButton) Record_List.getChildren().get(i)).isSelected()) {
                    str = str + Record_List.getChildren().get(i).getUserData() + ",";
                }
                str = str.substring(0,str.length() - 1);
                str = "[" + str + "]";
                System.out.println("Draw QR code : **************************");
                System.out.println(str);
            KTHelper.drawQRCode(QR, str);
        });
        userButtons1[0].setStyle("-fx-pref-width: 400;");
        hbox3n1.getChildren().add(userButtons1[0]);
        vbox3.getChildren().add(hbox3n1);

        HBox hbox3n2 = new HBox(20);
        hbox3n2.setAlignment(Pos.CENTER);

        userButtons2[0].setOnAction(event -> {
            String patientID = GeneralHelper.bytesToStringHex(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(PKEY));
            SceneManager.addPatient(Name, ID, keyTime.getPubKeyEncoded(), patientID);
            Date date= new Date();
            long time = date.getTime();

            String SQL = "if (select TOP 1 patientIdentifier from Customer where patientIdentifier = '" + patientID + "' " +
            "and Records is NULL and PatientName = '" + Name + "' and ID = '" + ID + "') is null " +
            "begin insert into Customer(PatientName,Gender,ID,[Date of birth],address,phoneNum,nationality,extra, patientIdentifier, Timestamp)" +
            "values ('"+ Name +"', '"+ Gender +"','"+ ID +"','" + Birth + "','" + address + "','"+ phoneNum +"','"+ nationality +"','"+ extra +"','" +
                    patientID + "','" + time + "' ) end " +
            "else " +
            "begin " +
            "delete Customer where PatientName = '" + Name + "' and ID = '" + ID + "' and patientIdentifier = '" + patientID + "' and Records is NULL " +
            "insert into Customer(PatientName,Gender,ID,[Date of birth],address,phoneNum,nationality,extra,patientIdentifier, Timestamp)" +
            "values ('"+ Name +"', '"+ Gender +"','"+ ID +"','" + Birth + "','" + address + "','"+ phoneNum +"','"+ nationality +"','"+ extra +"','" +
                    patientID +  "','" + time + "' ) end ";
            System.out.println(SQL);
            try {
                GlobalVar.statement.executeUpdate(SQL);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < Timestamp.size(); i ++)
            {
                SQL = "if (select TOP 1 Timestamp from Customer where patientIdentifier = '" + patientID + "' and Timestamp = '" +
                        Timestamp.get(i).toString() +
                        "') is null " +
                        "begin insert into Customer (patientIdentifier, MedName, Records, NewRecords, Timestamp) " +
                        "values ('" + patientID + "','" + MedName.get(i) + "','" + Records.get(i) + "',0 ,'" + Timestamp.get(i).toString() + "') " + "END";
                System.out.println(SQL);
                try {
                    GlobalVar.statement.executeUpdate(SQL);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            eraseData();
            SceneManager.showMainMenuScene();
        });

        userButtons2[1].setOnAction(event -> {
            eraseData();
            SceneManager.showMainMenuScene();
        });

        hbox3n2.getChildren().addAll(userButtons2[0],userButtons2[1]);
        vbox3.getChildren().add(hbox3n2);

        VBox vbox3n2 = new VBox(20, label3, vbox3);
        vbox3n2.setMaxSize(400,700);
        vbox3n2.setAlignment(Pos.CENTER);


        hBox1.getChildren().add(vbox3n2);

        scene.setRoot(hBox1);
    }

}