package rest.pojo;


public class PatientInfoPojo {

    private long timestamp;
    private byte[] ecPublicKey;
    private byte[] encryptedInfo;
    private byte[] signature;
    private boolean isKeyDEREncoded; // true if DER encoded, false if raw (To not, simple calling getEncoded() gives DER encoded key)
    private boolean isSignatureDEREncoded; // true if DER encoded, false if raw (To not, simple calling getEncoded() gives DER encoded signature)

    public PatientInfoPojo() {
    }

    public PatientInfoPojo(long timestamp, byte[] ecPublicKey, byte[] encryptedInfo, byte[] signature, boolean isKeyDEREncoded, boolean isSignatureDEREncoded) {
        this.timestamp = timestamp;
        this.ecPublicKey = ecPublicKey;
        this.encryptedInfo = encryptedInfo;
        this.signature = signature;
        this.isKeyDEREncoded = isKeyDEREncoded;
        this.isSignatureDEREncoded = isSignatureDEREncoded;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getEcPublicKey() {
        return ecPublicKey;
    }

    public void setEcPublicKey(byte[] ecPublicKey) {
        this.ecPublicKey = ecPublicKey;
    }

    public byte[] getEncryptedInfo() {
        return encryptedInfo;
    }

    public void setEncryptedInfo(byte[] encryptedInfo) {
        this.encryptedInfo = encryptedInfo;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public boolean isKeyDEREncoded() {
        return isKeyDEREncoded;
    }

    public void setKeyDEREncoded(boolean keyDEREncoded) {
        isKeyDEREncoded = keyDEREncoded;
    }

    public boolean isSignatureDEREncoded() {
        return isSignatureDEREncoded;
    }

    public void setSignatureDEREncoded(boolean signatureDEREncoded) {
        isSignatureDEREncoded = signatureDEREncoded;
    }
}
