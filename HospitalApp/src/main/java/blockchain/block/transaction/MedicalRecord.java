package blockchain.block.transaction;

import java.security.PrivateKey;
import java.security.PublicKey;

public class MedicalRecord {

    public final byte[] medicalOrgIdentifier;
    public final long timeStamp;
    public final byte[] encryptedContent;
    public final byte[] encryptedAESKey;
    public final byte[] patientSignature;
    public final byte[] patientIdentifier;
    public final byte[] medicalOrgSignature;

    public MedicalRecord(byte[] medicalOrgIdentifier, long timeStamp, byte[] encryptedContent, byte[] encryptedAESKey, byte[] patientSignature, byte[] patientIdentifier, PrivateKey medicalOrgPrivateKey) {
        this.medicalOrgIdentifier = medicalOrgIdentifier;
        this.timeStamp = timeStamp;
        this.encryptedContent = encryptedContent;
        this.encryptedAESKey = encryptedAESKey;
        this.patientSignature = patientSignature;
        this.patientIdentifier = patientIdentifier;
        this.medicalOrgSignature = null;
    }
}