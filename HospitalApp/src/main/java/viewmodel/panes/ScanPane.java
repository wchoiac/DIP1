package viewmodel.panes;

import blockchain.block.transaction.MedicalContent;
import blockchain.block.transaction.MedicalRecord;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import main.Helper;
import xyz.medirec.medirec.PublicKeyProperties;
import viewmodel.SceneManager;

import javax.crypto.SecretKey;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;

public class ScanPane extends BorderPane {

    private class WebCamInfo {
        String webCamName;
        int webCamIndex;

        @Override
        public String toString() {
            return webCamName;
        }
    }

    private HBox container;
    private Button backToMenu;
    private Webcam webcam;
    private ImageView imgWebCamCapturedImage;
    private boolean stopCamera = true;
    private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();
    private ComboBox<WebCamInfo> cameraOptions;
    private static Helper helper = Helper.getInstance();
    private boolean isSign = false;

    public ScanPane() {
        container = new HBox(20);
        backToMenu = new Button("Back to Menu");
        imgWebCamCapturedImage = new ImageView();
        cameraOptions = new ComboBox<>();

        createTopPanel();
        connectComponents();
        styleComponents();
        setCallbacks();

        Platform.runLater(this::setImageViewSize);
    }

    public void setIsSign(boolean isSign) {
        this.isSign = isSign;
    }

    public void startWebCam() {
        int index = cameraOptions.getSelectionModel().getSelectedIndex();
        if(index == -1)
            cameraOptions.getSelectionModel().selectFirst();
        else
            scan(index);
    }

    private void setImageViewSize() {
        double height = getHeight() * 0.9;
        double width = getWidth() * 0.9;
        imgWebCamCapturedImage.setFitHeight(height);
        imgWebCamCapturedImage.setFitWidth(width);
        imgWebCamCapturedImage.prefHeight(width);
        imgWebCamCapturedImage.prefWidth(height);
        imgWebCamCapturedImage.setPreserveRatio(true);
    }

    private void connectComponents() {
        container.getChildren().addAll(
                cameraOptions,
                backToMenu
        );
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(0, 0, 7, 0));
        this.setCenter(imgWebCamCapturedImage);
//        this.setCenter(imageCanvas);
        this.setBottom(container);
    }

    private void createTopPanel() {
        int webCamCounter = 0;
        ObservableList<WebCamInfo> options = FXCollections.observableArrayList();

        for (Webcam webcam : Webcam.getWebcams()) {
            WebCamInfo webCamInfo = new WebCamInfo();
            webCamInfo.webCamIndex = webCamCounter;
            webCamInfo.webCamName = webcam.getName();
            options.add(webCamInfo);
            webCamCounter++;
        }

        cameraOptions.setItems(options);
        cameraOptions.setPromptText("Choose Camera");
        cameraOptions.getSelectionModel().selectedItemProperty().addListener((list, oldValue, newValue) -> {
            if (newValue != null) {
                scan(newValue.webCamIndex);
            }
        });
    }

    private void styleComponents() {

    }

    private void setCallbacks() {
        backToMenu.setOnAction(e -> {
            disposeWebCamCamera();
            SceneManager.getInstance().showMainMenuScene();
        });
    }

    private void scan(int webCamIndex) {
        Thread webCamThread = new Thread(() -> {
            if (webcam != null)
                disposeWebCamCamera();
            stopCamera = false;

            webcam = Webcam.getWebcams().get(webCamIndex);
            webcam.setCustomViewSizes(WebcamResolution.HD.getSize());
            webcam.setViewSize(WebcamResolution.HD.getSize());
            webcam.open();

            //            final AtomicReference<WritableImage> ref = new AtomicReference<>();
            BufferedImage img;
            String decode;
            while (!stopCamera) {
                if ((img = webcam.getImage()) != null) {
                    System.out.println(webcam.getFPS() + ", width: " + img.getWidth() + ", height: " + img.getHeight());

                    for (int row=0; row < img.getHeight();  ++row) {
                        for (int col = 0; col < img.getWidth() / 2; ++col) {
                            int temp = img.getRGB(col, row);
                            img.setRGB(col, row, img.getRGB(img.getWidth() - col - 1, row));
                            img.setRGB(img.getWidth() - col - 1, row, temp);
                        }
                    }
                    imageProperty.set(SwingFXUtils.toFXImage(img, null));

                    decode = decodeQRCode(img);
                    if(decode != null){
                        System.out.println(decode);
                        PublicKey publicKey = tryObtainPublicKey(decode);
                        if(publicKey != null){
                            //scan pane -> scan public Key -> Find record from blockchain -> show encrypted aes key
                            System.out.println("PUBLIC KEY OBTAINED");
                            if(isSign) {
                                MedicalRecord medicalRecord = findMedicalRecord(publicKey);
                                if (medicalRecord != null) {
                                    SceneManager.getInstance().getQRCodePane().drawQRCode(
                                            Helper.getInstance().encode(medicalRecord.encryptedAESKey));
                                    SceneManager.getInstance().showQRScene();
                                } else {
                                    System.out.println("INVALID PUBLIC KEY");
                                }
                            } else {
                                MedicalContent medicalContent = findMedicalContent(publicKey);
                                if(medicalContent != null) {
                                    try {
                                        String serialized = Helper.getInstance().serialize(medicalContent);
                                        String hash = Helper.getInstance().getHash(serialized);
                                        SceneManager.getInstance().getQRCodePane().drawQRCode(hash);
                                        SceneManager.getInstance().showQRScene();
                                    } catch (IOException | NoSuchAlgorithmException | NoSuchProviderException e) {
                                        e.printStackTrace();
                                        System.out.println("INVALID PUBLIC KEY");
                                    }
                                } else {
                                    System.out.println("INVALID PUBLIC KEY");
                                }
                            }
                        }else {
                            if(isSign) {
                                System.out.println("IT IS NOT AN ID");
                                continue;
                            }
                            //get encrypted aes key then make it into QR code -> scan by user -> return aes key
                            SecretKey aesKey = tryObtainAESKey(decode);
                            if(aesKey != null) {
                                System.out.println("AES KEY OBTAINED");
                                //scan pane -> scan aes key -> decode record using aes key -> save it into local database

                                SceneManager.getInstance().showMainMenuScene();
                            }else {
                                if(helper.decode(decode).length == 256) {
                                    System.out.println("SIGNATURE FOUND");

//                                    MedicalRecord mediRec = new MedicalRecord(,)
                                    //generate hash of the record into QR code -> scan by user -> return signature
                                    //scan pane -> scan signature -> put signature into the record then create record

                                } else {
                                    System.out.println("INVALID QR CODE");
                                    continue;
                                }
                            }
                        }
                        imageProperty.setValue(null);
                        disposeWebCamCamera();
                        break;
                    }
                    img.flush();
                }
            }
        });
        imgWebCamCapturedImage.imageProperty().bind(imageProperty);
        webCamThread.setDaemon(true);
        webCamThread.start();
    }

    private MedicalRecord findMedicalRecord(PublicKey publicKey) {

        return null;
    }

    private MedicalContent findMedicalContent(PublicKey publicKey) {

        return null;
    }

    private PublicKey tryObtainPublicKey(String str) {
        try {
            PublicKeyProperties prop = (PublicKeyProperties) helper.deserialize(str);
            return new PublicKey() {
                @Override public String getAlgorithm() { return prop.algorithm; }
                @Override public String getFormat() { return prop.format; }
                @Override public byte[] getEncoded() { return prop.encoded; }
            };
        } catch (Exception e) {
            return null;
        }
    }

    private SecretKey tryObtainAESKey(String str) {
        try {
            return (SecretKey) helper.deserialize(str);
        } catch (Exception e) {
            return null;
        }
    }

    private String decodeQRCode(BufferedImage qrCodeimage) {
        LuminanceSource source = new BufferedImageLuminanceSource(qrCodeimage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    public void disposeWebCamCamera() {
        stopCamera = true;
        webcam.close();
    }
}
