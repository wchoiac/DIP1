import config.Configuration;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import general.utility.Helper;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import viewmodel.Config;

import javax.crypto.SecretKey;
public class QRCodePane {
    public static Scene scene = new Scene(new Group(), Config.WIDTH, Config.HEIGHT);
    private static ImageView QRview = new ImageView();
    private static final VBox container = new VBox(50);
    private static final Button backButton = new Button("Back");
    private static final Button scanButton = new Button("Scan Signature");
    private static String record = null;

    public void setRecord (String input) {
        record = input;
    }

    public void drawQRcode(SecretKey key, long timestamp){
        byte[] Medical_Identifier = null;
        byte[] encrypted_data = null;
        byte[] hash_Output = null;
        try {
            encrypted_data = Helper.AESencrypt(record.getBytes(), key);
            Medical_Identifier = GlobalVar.fullNodeRestClient.getMedicalOrgIdentifier();
            byte[] timestamp_byte = GeneralHelper.longToBytes(timestamp);
            byte[] combined_bytes = new byte[timestamp_byte.length + encrypted_data.length + Medical_Identifier.length];

            System.arraycopy(timestamp_byte, 0, combined_bytes, 0, timestamp_byte.length);
            System.arraycopy(encrypted_data, 0, combined_bytes, timestamp_byte.length, encrypted_data.length);
            System.arraycopy(Medical_Identifier, 0, combined_bytes, timestamp_byte.length + encrypted_data.length, Medical_Identifier.length);

            hash_Output = SecurityHelper.hash(combined_bytes, Configuration.BLOCKCHAIN_HASH_ALGORITHM);

            String QRoutput = GeneralHelper.bytesToStringHex(hash_Output);
            KTHelper.drawQRCode(QRview, QRoutput);
        } catch (Exception e){e.printStackTrace();}
    }


    public void init() {
        connectComponents();
        styleComponents();
        setCallbacks();
    }

    private void connectComponents(){
        HBox tempBox = new HBox(30, backButton, scanButton);
        tempBox.setAlignment(Pos.CENTER);
        container.getChildren().addAll(QRview, tempBox);
        container.setAlignment(Pos.CENTER);
        scene.setRoot(container);
    }

    private void styleComponents(){
    }

    private void setCallbacks(){
        backButton.setOnAction(event -> {
            SceneManager.showMainMenuScene();
        });

        scanButton.setOnAction(event -> {
            SceneManager.showScanScene(ScanPane.TYPE.SIGNATURE);
        });
    }
}