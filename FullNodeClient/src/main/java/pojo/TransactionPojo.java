package pojo;

public class TransactionPojo {

    private long timestamp;
    private byte[] encryptedRecord;
    private byte[] patientSignature;
    private byte[] patientIdentifier;
    private boolean signatureDEREncoded; // true if DER encoded, false if raw (To not, simple calling getEncoded() gives DER encoded signature)

    public TransactionPojo() {
    }

    public TransactionPojo(long timestamp, byte[] encryptedRecord, byte[] patientSignature, byte[] patientIdentifier, boolean signatureDEREncoded) {
        this.timestamp = timestamp;
        this.encryptedRecord = encryptedRecord;
        this.patientSignature = patientSignature;
        this.patientIdentifier = patientIdentifier;
        this.signatureDEREncoded = signatureDEREncoded;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getEncryptedRecord() {
        return encryptedRecord;
    }

    public void setEncryptedRecord(byte[] encryptedRecord) {
        this.encryptedRecord = encryptedRecord;
    }

    public byte[] getPatientSignature() {
        return patientSignature;
    }

    public void setPatientSignature(byte[] patientSignature) {
        this.patientSignature = patientSignature;
    }

    public byte[] getPatientIdentifier() {
        return patientIdentifier;
    }

    public void setPatientIdentifier(byte[] patientIdentifier) {
        this.patientIdentifier = patientIdentifier;
    }

    public boolean isIgnatureDEREncoded() {
        return signatureDEREncoded;
    }

    public void setIgnatureDEREncoded(boolean signatureDEREncoded) {
        this.signatureDEREncoded = signatureDEREncoded;
    }
}
