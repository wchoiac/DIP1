package rest.pojo;

public class TransactionPojo {

    private long timestamp;
    private byte[] encryptedRecord;
    private byte[] patientSignature;
    private byte[] patientIdentifier;
    private boolean isSignatureDEREncoded; // true if DER encoded, false if raw (To not, simple calling getEncoded() gives DER encoded signature)

    public TransactionPojo() {
    }

    public TransactionPojo(long timestamp, byte[] encryptedRecord, boolean isSignatureDEREncoded, byte[] patientSignature, byte[] patientIdentifier) {
        this.timestamp = timestamp;
        this.encryptedRecord = encryptedRecord;
        this.isSignatureDEREncoded = isSignatureDEREncoded;
        this.patientSignature = patientSignature;
        this.patientIdentifier = patientIdentifier;
    }

    public boolean isSignatureDEREncoded() {
        return isSignatureDEREncoded;
    }

    public void setSignatureDEREncoded(boolean signatureDEREncoded) {
        isSignatureDEREncoded = signatureDEREncoded;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setEncryptedRecord(byte[] encryptedRecord) {
        this.encryptedRecord = encryptedRecord;
    }

    public void setPatientSignature(byte[] patientSignature) {
        this.patientSignature = patientSignature;
    }

    public void setPatientIdentifier(byte[] patientIdentifier) {
        this.patientIdentifier = patientIdentifier;
    }


    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getEncryptedRecord() {
        return encryptedRecord;
    }

    public byte[] getPatientSignature() {
        return patientSignature;
    }

    public byte[] getPatientIdentifier() {
        return patientIdentifier;
    }
}
