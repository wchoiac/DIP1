import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

import blockchain.BlockChainSecurityHelper;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import config.Configuration;
import exception.*;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import general.utility.Helper;
import pojo.*;
import xyz.medirec.medirec.pojo.KeyTime;
import xyz.medirec.medirec.pojo.SecretTime;
import xyz.medirec.medirec.pojo.SignatureKey;

public class WebcamScan extends JFrame implements Runnable, ThreadFactory {

    private Executor executor = Executors.newSingleThreadExecutor(this);

    private Webcam webcam = null;
    private WebcamPanel panel = null;
    private JTextArea textarea = null;
    private JButton triggerbutton = null;

    public WebcamScan() {
        super();

        setLayout(new FlowLayout());
        setTitle("Read QR / Bar Code With Webcam");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension size = WebcamResolution.QVGA.getSize();
        size.height = 640;
        size.width = 480;

        try{webcam = Webcam.getWebcams().get(0);
            webcam.setViewSize(size);

            panel = new WebcamPanel(webcam);
            panel.setPreferredSize(size);
            panel.setFPSDisplayed(true);



            triggerbutton = new JButton("Trigger");
            triggerbutton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    if(webcam.isOpen()) webcam.close();
                    else webcam.open();
                }
            });

            setUndecorated(true);
            add(panel);
            add(triggerbutton);

            pack();
            setVisible(false);

            executor.execute(this);
        } catch (Exception e){System.out.println("No CAM");}
    }

    @Override
    public void run() {

        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Result result = null;
            BufferedImage image = null;

            if (webcam.isOpen()) {

                if ((image = webcam.getImage()) == null) {
                    continue;
                }

                LuminanceSource source = new BufferedImageLuminanceSource(image);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                try {
                    result = new MultiFormatReader().decode(bitmap);
                } catch (NotFoundException e) {
                    // fall thru, it means there is no QR code in image
                }
            }

            if (result != null && GlobalVar.Scantype != 0) {
                GlobalVar.QRcode = result.getText();
                System.out.println("QRcode in string : " + GlobalVar.QRcode);
                switch(GlobalVar.Scantype){
                    case 1:
                        Getrecord();
                        GlobalVar.Scantype = 0;
                        setVisible(false);
                        break;
                    case 2:
                        GetAESkey();
                        GlobalVar.Scantype = 0;
                        setVisible(false);
                        break;
                    case 3:
                        GetSignature();
                        GlobalVar.Scantype = 0;
                        setVisible(false);
                    default:
                        break;

                }
            }

        } while (true);
    }

    public void Getrecord(){
        if (GlobalVar.QRcode != ""){
            Object KeyO = null;
            try{
                KeyO = Helper.deserialize(GlobalVar.QRcode);
            } catch (Exception e1) {
            }

            if (KeyO.getClass() == KeyTime.class) {
                byte[] encoded_public_key = ((KeyTime) KeyO).getPubKeyEncoded();
                byte[][] encoded_secret_key = ((KeyTime) KeyO).getSecretKeysEncoded();
                long[] Timelist = ((KeyTime) KeyO).getTimeList();
                KeyFactory keyFactory = null;
                ECPublicKey PKEY = null;
                SecretKey AESkey = null;
                byte[] hash_public_key = null;
                try {
                    keyFactory = KeyFactory.getInstance("EC");
                    PKEY = (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(encoded_public_key));
                    hash_public_key = SecurityHelper.hash(encoded_public_key, Configuration.BLOCKCHAIN_HASH_ALGORITHM);
                } catch (Exception e1) {
                }
                System.out.println("now : " + encoded_public_key);
                for(int i = 0; i < GlobalVar.patient_PublicKeyList_EC.size();i++) {
                    System.out.println("item " + i + " : " + GlobalVar.patient_PublicKeyList_EC.get(i));
                    if (GlobalVar.patient_PublicKeyList_EC.get(i).equals(PKEY)) return;
                }

                PatientShortInfoPojo[] Patientinfo = null;
                String patient_info_string = "Patient Infomation : \n";
                try {
                    Patientinfo = GlobalVar.fullNodeRestClient.getPatientShortInfoList(PKEY);
                    ArrayList<LocationPojo> patient_location_pojo = new ArrayList<LocationPojo>();
                    for (int i = 0; i < Patientinfo.length; i++)
                        patient_location_pojo.add(Patientinfo[i].getLocation());
                    PatientInfoContentPojo[] patientinfo_content = GlobalVar.fullNodeRestClient.getPatientInfoContentsList(patient_location_pojo);
                    for (int i = 0; i < Patientinfo.length; i++) {
                        for (int j = 0; j < Timelist.length; j++) {
                            if (Timelist[j] == Patientinfo[i].getTimestamp()) {
                                System.out.println("RUNING AES ************* " + i);
                                AESkey = new SecretKeySpec(encoded_secret_key[j], "AES");
                                byte[] decrypted_data = Helper.AESdecrypt(patientinfo_content[i].getEncryptedInfo(), AESkey);
                                String orginial_data = new String(decrypted_data);
                                patient_info_string = patient_info_string + orginial_data + '\n';
                            }
                        }
                    }

                    patient_info_string = Helper.GetFinalPatientInfo(patient_info_string);

                    int dialog = JOptionPane.showConfirmDialog(null,
                            "Please confirm the information of the patient\n" + patient_info_string,
                            "Patient information confirmation", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (dialog != JOptionPane.YES_OPTION){
                        JOptionPane.showMessageDialog(null,"Process is Canceled");
                        return;
                    }
                } catch (UnAuthorized e1) { System.out.println("Patient UnAuthorized error");
                } catch (NotFound e2) {System.out.println("Patient NotFound error");}
                catch (BadRequest e3) {System.out.println("Patient BadRequest error");}
                catch (ServerError e4) {System.out.println("Patient ServerError error");}
                catch (Exception e5){System.out.println("Patient AES error");}

                int name_int = patient_info_string.indexOf("name : ") + 7;
                int name_end = patient_info_string.indexOf("\n",name_int);
                int ID_int = patient_info_string.indexOf("identificationNumber : ") + 23;
                int ID_end = patient_info_string.indexOf("\n",ID_int);
                String NameID = patient_info_string.substring(name_int,name_end) +
                        " - " +
                        patient_info_string.substring(ID_int,ID_end);
                System.out.println(PKEY);
                GlobalVar.patient_PublicKeyList.add(encoded_public_key);
                GlobalVar.patient_PublicKeyList_EC.add(PKEY);
                GlobalVar.patient.add(NameID);
                GlobalVar.ListMenuString.addItem(NameID);
                GlobalVar.patient_number += 1;

                String public_key_name = Helper.encode(hash_public_key);
                public_key_name = Helper.eraseSymbol(public_key_name);
                String file_path = GlobalVar.cwd + "\\publicKey\\" ;
                String file_name = file_path + public_key_name + ".pem";
                File file = new File(file_name);
                try {
                    while (file.exists() && !file.isDirectory()) {
                        if (SecurityHelper.getPublicKeyFromPEM(file_name, "EC").equals(PKEY)) break;
                        hash_public_key = SecurityHelper.hash(hash_public_key, Configuration.BLOCKCHAIN_HASH_ALGORITHM);
                        public_key_name = Helper.encode(hash_public_key);
                        public_key_name = Helper.eraseSymbol(public_key_name);
                        file_name = file_path + public_key_name + ".pem";
                        file = new File(file_name);
                    }
                    if (!file.exists() && !file.isDirectory()) {
                        SecurityHelper.writePublicKeyToPEM(PKEY, file_name);
                    }
                } catch (Exception e1) {
                }

                RecordShortInfoPojo[] RecordInfo = null;
                String record_info_string = "Patient Record : \n";
                try {
                    RecordInfo = GlobalVar.fullNodeRestClient.getRecordShortInfoList(PKEY);
                    System.out.println("Record length : " + RecordInfo.length);
                    ArrayList<LocationPojo> record_location_pojo = new ArrayList<LocationPojo>();
                    for (int i = 0; i < RecordInfo.length; i++)
                    {
                        record_location_pojo.add(RecordInfo[i].getLocationPojo());
                        System.out.println("Record Time : " + RecordInfo[i].getTimestamp());
                    }
                    RecordContentPojo[] RecordInfo_content = GlobalVar.fullNodeRestClient.getRecordContentsList(record_location_pojo);
                    for (int i = 0; i < RecordInfo.length; i++) {
                        for (int j = 0; j < Timelist.length; j++) {
                            if (Timelist[j] == RecordInfo[i].getTimestamp()) {
                                String med_name = RecordInfo[i].getMedicalOrgName();
                                AESkey = new SecretKeySpec(encoded_secret_key[j], "AES");
                                byte[] decrypted_data = Helper.AESdecrypt(RecordInfo_content[i].getEncryptedRecord(), AESkey);
                                String orginial_data = new String(decrypted_data);
                                record_info_string = record_info_string + "from " + med_name + " : \n";
                                record_info_string = record_info_string + orginial_data + '\n';
                            }
                        }
                    }
                    System.out.println("Record info : " + record_info_string);
                } catch (UnAuthorized e1) { System.out.println("Record UnAuthorized error");
                } catch (NotFound e2) {System.out.println("Record NotFound error");}
                catch (BadRequest e3) {System.out.println("Record BadRequest error");
                    e3.printStackTrace();}
                catch (ServerError e4) {System.out.println("Record ServerError error");}
                catch (Exception e5){System.out.println("Patient AES error");}
            }
            GlobalVar.QRcode = "";
        }
    }

    public void GetAESkey(){
        if (GlobalVar.QRcode != "") {
            Object KeyO = null;
            try {
                KeyO = Helper.deserialize(GlobalVar.QRcode);
            } catch (Exception e1) {
            }

            if (KeyO.getClass() == SecretTime.class) {
                byte[] encoded_secret_key = ((SecretTime) KeyO).getSecretKeyEncoded();
                long Timestamp = ((SecretTime) KeyO).getTimestamp();
                int index = GlobalVar.ListMenuString.getSelectedIndex();
                GlobalVar.TimeStamp.add(index,Timestamp);
                SecretKey AESkey = null;
                AESkey = new SecretKeySpec(encoded_secret_key, "AES");
                String record_data = "testing data";
                //String record_data = Helper.importTextFile(GlobalVar.cwd + "\\NewRecord\\" + GlobalVar.patient.get(index));
                byte[] encrypted_data = null;
                byte[] Medical_Identifier = null;
                byte[] timestamp_byte = GeneralHelper.longToBytes(Timestamp);
                byte[] hash_Output = null;
                try {
                    encrypted_data = Helper.AESencrypt(record_data.getBytes(), AESkey);
                    Medical_Identifier = GlobalVar.fullNodeRestClient.getMedicalOrgIdentifier();

                    byte[] combined_bytes = new byte[timestamp_byte.length + encrypted_data.length + Medical_Identifier.length];

                    System.arraycopy(timestamp_byte, 0, combined_bytes, 0, timestamp_byte.length);
                    System.arraycopy(encrypted_data, 0, combined_bytes, timestamp_byte.length, encrypted_data.length);
                    System.arraycopy(Medical_Identifier, 0, combined_bytes, timestamp_byte.length + encrypted_data.length, Medical_Identifier.length);

                    hash_Output = SecurityHelper.hash(combined_bytes, Configuration.BLOCKCHAIN_HASH_ALGORITHM);

                    String QRoutput = GeneralHelper.bytesToStringHex(hash_Output);

                    GlobalVar.EncryptedRecord.add(index, encrypted_data);
                    GlobalVar.QRoutput.add(index, QRoutput);
                } catch(Exception e1){}
            }
            GlobalVar.QRcode = "";
        }
    }

    public void GetSignature(){
        if (GlobalVar.QRcode != "") {

            byte[] signature = Helper.decode(GlobalVar.QRcode);
            int index = GlobalVar.ListMenuString.getSelectedIndex();
            byte[] result = null;

            System.out.println(GlobalVar.QRcode);
            System.out.println(GlobalVar.QRcode.length());
            System.out.println("Length : " + signature.length);

            try{
                result = GlobalVar.fullNodeRestClient.addTransaction(GlobalVar.TimeStamp.get(index),
                        GlobalVar.EncryptedRecord.get(index),
                        false,
                        signature,
                        BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(GlobalVar.patient_PublicKeyList_EC.get(index)));
            } catch (UnAuthorized e) {System.out.println("UnAuthorized");}
            catch (NotFound e) {System.out.println("NotFound");}
            catch (BadRequest e) {System.out.println("BadRequest");
                System.out.println("getMessage : "+ e.getMessage());
                e.printStackTrace();}
            catch (ServerError e) {System.out.println("ServerError");}
            catch (Unsuccessful e) {System.out.println("Unsuccessful");
                System.out.println("getMessage : "+ e.getMessage());
                e.printStackTrace();}



            if (result != null){
                System.out.println(result);
                GlobalVar.patient.remove(index);
                GlobalVar.EncryptedRecord.remove(index);
                GlobalVar.patient_PublicKeyList.remove(index);
                GlobalVar.TimeStamp.remove(index);
                GlobalVar.QRoutput.remove(index);
                GlobalVar.ListMenuString.remove(index);
            }

            GlobalVar.QRcode = "";
        }

    }
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "example-runner");
        t.setDaemon(true);
        return t;
    }

}