package pojo;

public class AuthorityInfoPojo {

    private String name;
    private byte[] ecPublicKey;
    private boolean isKeyDEREncoded; // true if DER encoded, false if raw (To not, simple calling getEncoded() gives DER encoded key)

    public AuthorityInfoPojo() {
    }

    public AuthorityInfoPojo(String name, byte[] ecPublicKey, boolean isKeyDEREncoded) {
        this.name = name;
        this.ecPublicKey = ecPublicKey;
        this.isKeyDEREncoded = isKeyDEREncoded;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getEcPublicKey() {
        return ecPublicKey;
    }

    public void setEcPublicKey(byte[] ecPublicKey) {
        this.ecPublicKey = ecPublicKey;
    }

    public boolean isKeyDEREncoded() {
        return isKeyDEREncoded;
    }

    public void setKeyDEREncoded(boolean keyDEREncoded) {
        isKeyDEREncoded = keyDEREncoded;
    }
}
