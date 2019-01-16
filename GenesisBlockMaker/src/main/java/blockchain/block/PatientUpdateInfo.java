package blockchain.block;

import blockchain.interfaces.Raw;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;

import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Objects;

public class PatientUpdateInfo implements Raw {

    private long timestamp; //8 bytes
    //<= encryptedinfo length (4 bytes)
    private byte[] encryptedInfo; // variable length
    private byte[] identifier; // 20 bytes
    private byte[] signature; // 64 bytes

    public PatientUpdateInfo(long timestamp, byte[] patientIdentifier, byte[] encryptedInfo, byte[] signature)
    {
        this.setTimestamp(timestamp);
        this.setEncryptedInfo(encryptedInfo);
        this.setIdentifier(patientIdentifier);
        this.setSignature(signature);

    }

    public PatientUpdateInfo()
    {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getEncryptedInfo() {
        return encryptedInfo;
    }

    public void setEncryptedInfo(byte[] encryptedInfo) {
        this.encryptedInfo = encryptedInfo;
    }

    public byte[] getPatientIdentifier() {
        return identifier;
    }

    public void setIdentifier(byte[] identifier) {
        this.identifier = identifier;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }


    public byte[] getRaw()
    {
        return GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp),GeneralHelper.intToBytes(getEncryptedInfo().length)
                , getEncryptedInfo(), getPatientIdentifier(),getSignature());

    }
    public static PatientUpdateInfo parse(ByteArrayReader byteArrayReader) {
        PatientUpdateInfo patientUpdateInfo = new PatientUpdateInfo();

        patientUpdateInfo.setTimestamp(GeneralHelper.bytesToLong(byteArrayReader.readBytes(Long.BYTES)));

        int encryptedInfoLength = GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES));
        patientUpdateInfo.setEncryptedInfo(byteArrayReader.readBytes(encryptedInfoLength));
        patientUpdateInfo.setIdentifier(byteArrayReader.readBytes(Configuration.IDENTIFIER_LENGTH));
        patientUpdateInfo.setSignature(byteArrayReader.readBytes(Configuration.SIGNATURE_LENGTH));

        return patientUpdateInfo;
    }

    public byte[] calculateInfoHash()
    {
        try {
            return SecurityHelper.hash(encryptedInfo, Configuration.BLOCKCHAIN_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;// not expected
    }

    public byte[] getSignatureCoverage()
    {
        return GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp),getEncryptedInfo());
    }



    public boolean verify(ECPublicKey publicKey)
    {
        try {
            return SecurityHelper.verifyRawECDSASignatureWithContent(publicKey,getSignatureCoverage(),signature,Configuration.BLOCKCHAIN_HASH_ALGORITHM, Configuration.ELIPTIC_CURVE);
} catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        }
        return false;// not expected
        }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatientUpdateInfo that = (PatientUpdateInfo) o;
        return timestamp == that.timestamp &&
                Arrays.equals(encryptedInfo, that.encryptedInfo) &&
                Arrays.equals(identifier, that.identifier) &&
                Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(timestamp);
        result = 31 * result + Arrays.hashCode(encryptedInfo);
        result = 31 * result + Arrays.hashCode(identifier);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
}
