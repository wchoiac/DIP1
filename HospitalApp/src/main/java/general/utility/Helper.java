package general.utility;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class Helper {
    private static final Helper instance = new Helper();

    private Helper(){
        addProvider();
    }

    public static Helper getInstance() {
        return instance;
    }

    public String getProvider() {
        return BouncyCastleProvider.PROVIDER_NAME;
    }


    private String addProvider() {
        Security.addProvider(new BouncyCastleProvider());
        return getProvider();
    }

    public KeyPair generateKeyPair(String algorithm) throws NoSuchProviderException, NoSuchAlgorithmException {
        return generateKeyPair(algorithm,"BC");
    }

    public KeyPair generateKeyPair(String algorithm, String provider) throws NoSuchProviderException, NoSuchAlgorithmException {
        var keyPair = KeyPairGenerator.getInstance(algorithm, provider);
        var keySize = (algorithm.equals("ECDSA") || algorithm.equals("EC"))? 256 : 2048;
        keyPair.initialize(keySize);
        return keyPair.generateKeyPair();
    }

    public String getKey(Key key) {
        return String.format("%064x", new BigInteger(1, key.getEncoded()));
    }

    public byte[] encrypt(String text, PublicKey key, String algorithm) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
        return encrypt(text, key, algorithm, "BC");
    }

    public byte[] encrypt(String text, PublicKey key, String algorithm, String provider) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        var cipher = Cipher.getInstance(algorithm, provider);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(text.getBytes());
    }

    public String decrypt(byte[] text, PrivateKey key, String algorithm) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidKeyException {
        return decrypt(text, key, algorithm, "BC");
    }

    public static String decrypt(byte[] text, PrivateKey key, String algorithm, String provider) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        var cipher = Cipher.getInstance(algorithm, provider);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(text));
    }

    public static byte[] AESencrypt(byte[] text, SecretKey key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(text);
    }
    public static byte[] AESdecrypt(byte[] text, SecretKey key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher.doFinal(text);
    }

    public static byte[] generateSignature(PrivateKey privateKey, byte[] input) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return generateSignature(privateKey, input, "RSA", "BC");
    }

    public static byte[] generateSignature(PrivateKey privateKey, byte[] input, String algorithm, String provider) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        var signature = Signature.getInstance(algorithm, provider);
        signature.initSign(privateKey);
        signature.update(input);
        return signature.sign();
    }

    public boolean verifySignature(PublicKey publicKey, byte[] input, byte[] encSignature) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return verifySignature(publicKey, input, encSignature, "RSA", "BC");
    }

    public boolean verifySignature(PublicKey publicKey, byte[] input, byte[] encSignature, String algorithm, String provider) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        var signature = Signature.getInstance(algorithm, provider);
        signature.initVerify(publicKey);
        signature.update(input);
        return signature.verify(encSignature);
    }

    public SecretKey makePbeKey(char[] password) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return makePbeKey(password, "BC");
    }

    public SecretKey makePbeKey(char[] password, String provider) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        var keyFact = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA384", provider);
        var hmacKey = keyFact.generateSecret(
                new PBEKeySpec(password, Hex.decode("0102030405060708090a0b0c0d0e0f10"), 1024, 256));
        return new SecretKeySpec(hmacKey.getEncoded(), "AES");
    }

    public String getHash(byte[] bytes) throws NoSuchProviderException, NoSuchAlgorithmException {
        return getHash(bytes, "BC");
    }

    public String getHash(byte[] bytes, String provider) throws NoSuchProviderException, NoSuchAlgorithmException {
        return byteToHex(MessageDigest.getInstance("SHA-256", provider).digest(bytes));
    }

    public String getHash(String string) throws NoSuchProviderException, NoSuchAlgorithmException {
        return getHash(string, "BC");
    }

    public String getHash(String string, String provider) throws NoSuchProviderException, NoSuchAlgorithmException {
        return getHash(string.getBytes(), provider);
    }

    public String byteToHex(byte[] byteArray) {
        return new String(Hex.encode(byteArray));
    }

    public static Object deserialize(String string) throws IOException, ClassNotFoundException {
        byte[] serializedMember = decode(string);
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedMember);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object objectMember = ois.readObject();

        return objectMember;
    }

    public String serialize(Object obj) throws IOException {
        byte[] serializedMember;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        serializedMember = baos.toByteArray();

        return encode(serializedMember);
    }

    public static String encode(byte[] byteArray) {
        return Base64.getEncoder().encodeToString(byteArray);
    }

    public static byte[] decode(String string) {
        return Base64.getDecoder().decode(string);
    }

    public boolean exportToFile(String path, String fileName, String input) {
        try {
            var directory = new File(path);
            if (!directory.exists())
                if(!directory.mkdir())
                    throw new IOException("Directory creation failed");
            var writer = new BufferedWriter(new FileWriter(path + "/" + fileName));
            writer.write(input);
            writer.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportToFile(String path, String fileName, Key input) {
        try {
            var directory = new File(path);
            if (!directory.exists())
                if(!directory.mkdir())
                    throw new IOException("Directory creation failed");
            var objectOutputStream = new ObjectOutputStream(new FileOutputStream(new File(path + "/" + fileName)));
            objectOutputStream.writeObject(input);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Key importObjectFile(String path) {
        try {
            var objectInputStream = new ObjectInputStream(new FileInputStream(new File(path)));
            return (Key) objectInputStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String importTextFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static void drawQRCode(ImageView qrView, String str) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        int width = 300;
        int height = 300;

        BufferedImage bufferedImage;
        try {
            BitMatrix byteMatrix = qrCodeWriter.encode(str, BarcodeFormat.QR_CODE, width, height);
            bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            bufferedImage.createGraphics();

            Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLACK);

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (byteMatrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1);
                    }
                }
            }
            JOptionPane.showMessageDialog(null, new ImageIcon(bufferedImage));
            //qrView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

            System.out.println("Success...");
        } catch (WriterException e) {
            System.out.println("Failed to create a QR Code");
        }
    }

    public static String eraseSymbol(String string){
        string = string.replace("/", "");
        string = string.replace("\\", "");
        string = string.replace("*", "");
        string = string.replace("?", "");
        string = string.replace(":", "");
        string = string.replace("|", "");
        string = string.replace("<", "");
        string = string.replace(">", "");
        string = string.replace("\"", "");
        return string;
    }

    public static String GetFinalPatientInfo (String patient_info_string){
        int name_index = patient_info_string.lastIndexOf("\"name\"");
        int name_end_index = patient_info_string.indexOf(",",name_index)+1;
        int gender_index = patient_info_string.lastIndexOf("\"gender\"");
        int gender_end_index = patient_info_string.indexOf(",",gender_index)+1;
        int birthDate_index = patient_info_string.lastIndexOf("\"birthDate\"");
        int birthDate_end_index = patient_info_string.indexOf(",",birthDate_index)+1;
        int identificationNumber_index = patient_info_string.lastIndexOf("\"identificationNumber\"");
        int identificationNumber_end_index = patient_info_string.indexOf(",",identificationNumber_index)+1;
        int bloodType_index = patient_info_string.lastIndexOf("\"bloodType\"");
        int bloodType_end_index = patient_info_string.indexOf(",",bloodType_index)+1;
        int weight_index = patient_info_string.lastIndexOf("\"weight\"");
        int weight_end_index = patient_info_string.indexOf(",",weight_index)+1;
        int height_index = patient_info_string.lastIndexOf("\"height\"");
        int height_end_index = patient_info_string.indexOf("}",height_index)+1;
        String final_patient_info = patient_info_string.substring(name_index,name_end_index) +
                patient_info_string.substring(gender_index,gender_end_index) +
                patient_info_string.substring(birthDate_index,birthDate_end_index) +
                patient_info_string.substring(identificationNumber_index,identificationNumber_end_index) +
                patient_info_string.substring(bloodType_index,bloodType_end_index) +
                patient_info_string.substring(weight_index,weight_end_index) +
                patient_info_string.substring(height_index,height_end_index) ;
        final_patient_info = final_patient_info.replace("\"","");
        final_patient_info = final_patient_info.replace(",","\n");
        final_patient_info = final_patient_info.replace("}","\n");
        final_patient_info = final_patient_info.replace(":"," : ");
        return final_patient_info;
    }
}
