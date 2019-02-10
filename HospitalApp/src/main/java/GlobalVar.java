
import pojo.LocationPojo;

import javax.crypto.SecretKey;
import javax.swing.*;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;

public class GlobalVar {
    public static String QRcode = "";
     public static FullNodeRestClient fullNodeRestClient = null;
    public static int patient_number = 0;
    public static int Scantype = 0;
    public static ArrayList<String> patient = new ArrayList<String>();
    public static ArrayList<String> QRoutput = new ArrayList<String>();
    public static ArrayList<byte[]> EncryptedRecord = new ArrayList<byte[]>();
    public static ArrayList<Long> TimeStamp = new ArrayList<Long>();
    public static ArrayList<byte[]> patient_PublicKeyList = new ArrayList<byte[]>();

    public static ArrayList<ECPublicKey> patient_PublicKeyList_EC = new ArrayList<ECPublicKey>();
    public static String cwd = "";
    public static JComboBox ListMenuString = new JComboBox();
}
