import blockchain.BlockChainSecurityHelper;
import general.utility.Helper;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import viewmodel.Config;
import xyz.medirec.medirec.pojo.KeyTime;

import javax.crypto.SecretKey;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;

public class SceneManager {
    private static Scene LogInScene = new LogInPane().scene;
    private static MainMenuPane MainMenuPane = new MainMenuPane();
    private static PatientPane PatientPane = new PatientPane();
    private static QRCodePane QrCodePane = new QRCodePane();
    private static Scene ScanScene = ScanPane.scanScene;
    private static Stage stage = null;
    public static String selected = null;
    private static byte[] encodedEC = null;
    private static String record_string = null;

    public static void init(){
        MainMenuPane.init();
        QrCodePane.init();
        Scene[] scenes = {LogInScene, MainMenuPane.scene, PatientPane.scene, ScanScene, QrCodePane.scene};
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
        showScene(LogInScene);
    }

    public static String[] CheckPatient(){
        return MainMenuPane.NameID_List();
    }

    public static void addPatient(String name, String ID , byte[] keyencoded){
        MainMenuPane.AddPatientToList(name, ID , keyencoded);
    }
    public static void showMainMenuScene(){
        showScene(MainMenuPane.scene);
    }

    public static void showPatientScene(KeyTime keyTime){
        PatientPane.setKeyTime(keyTime);
        if (PatientPane.retrieveData() == true)
        {
            PatientPane.init();
            showScene(PatientPane.scene);
        }
    }

    public static void showScanScene(ScanPane.TYPE type){
        showScene(ScanScene);
        ((ScanPane) ScanScene.getRoot()).startWebCam();
        ((ScanPane) ScanScene.getRoot()).type = type;
    }

    public static void showQRScene(SecretKey key, long timestamp){
        System.out.println("Timestamp1 : " + timestamp);
        QrCodePane.drawQRcode(key, timestamp);
        showScene(QrCodePane.scene);
    }

    public static void setRecord_string(String record) {
        record_string = record;
        QrCodePane.setRecord(record);
    }

    public static void setEncodedEC(byte[] bytearray) {
        encodedEC = bytearray;
    }

    public static void getSign(byte[] sign, SecretKey key, long timestamp) {
        //byte[] record = null;

        String name = selected.substring(0, selected.indexOf(" - "));
        String ID = selected.substring(selected.indexOf(" - ") + 3);
        System.out.println("Timestamp2 : " + timestamp);
        KeyFactory keyFactory = null;
        ECPublicKey PKEY = null;
        byte[] result = null;
        byte[] record= null;
        try {
            //record = Helper.AESencrypt(record_string.getBytes(), Key);
            keyFactory = KeyFactory.getInstance("EC");
            PKEY = (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(encodedEC));
            System.out.println("Timestamp : " + timestamp);
            System.out.println("PKEY : " + PKEY);
            record = Helper.AESencrypt(record_string.getBytes() , key);
            String testing = "";
            for (int i = 0; i < record.length; i++){
                int positive = record[i] & 0xff;
                testing = testing + positive + " ";
            }
            System.out.println("record : " + record_string);
            System.out.println("record : " + testing);
            testing = "";
            for (int i = 0; i < sign.length; i++){
                int positive = sign[i] & 0xff;
                testing = testing + positive + " ";
            }
            System.out.println("sign : " + testing);

            result = GlobalVar.fullNodeRestClient.addTransaction(timestamp,
                    record,
                    false,
                    sign,
                    BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(PKEY));
            String SQL = "update Customer set NewRecords = 0, Timestamp = '" +
                    timestamp +
                    "' where PatientName = '" + name +
                    "' and ID = '" + ID +
                    "' and MedName = 'local' and NewRecords = 1 ";
            GlobalVar.statement.executeUpdate(SQL);

            record_string = null;
            encodedEC = null;
            MainMenuPane.DelectPatient(selected);
            selected = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}