package blockchain.block;

import blockchain.interfaces.Raw;
import blockchain.utility.BlockChainSecurityHelper;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;

import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;

public class PatientInfo implements Raw {

    private long timestamp; //8 bytes
    //<= encryptedinfo length (4 bytes)
    private byte[] encryptedInfo; // variable length
    private ECPublicKey publicKey; // 33 bytes
    private byte[] signature; // 64 bytes

    public PatientInfo(long timestamp, ECPublicKey patientPublicKey, byte[] encryptedInfo, byte[] signature)
    {
        this.setTimestamp(timestamp);
        this.setEncryptedInfo(encryptedInfo);
        this.setPublicKey(patientPublicKey);
        this.setSignature(signature);

    }

    public PatientInfo()
    {
    }

    public byte[] getPatientIdentifier()
    {
        return BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(publicKey);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ECPublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(ECPublicKey publicKey) {
        this.publicKey = publicKey;
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

    public byte[] getRaw()
    {
        return GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp),GeneralHelper.intToBytes(getEncryptedInfo().length)
                , getEncryptedInfo(), SecurityHelper.getCompressedRawECPublicKey( getPublicKey(),Configuration.ELIPTIC_CURVE),getSignature());

    }
    public static PatientInfo parse(ByteArrayReader byteArrayReader) throws BlockChainObjectParsingException {
        PatientInfo patientInfo = new PatientInfo();

        patientInfo.setTimestamp(GeneralHelper.bytesToLong(byteArrayReader.readBytes(Long.BYTES)));

        int encryptedInfoLength = GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES));
        patientInfo.setEncryptedInfo(byteArrayReader.readBytes(encryptedInfoLength));
        try {
            patientInfo.setPublicKey(SecurityHelper.getECPublicKeyFromCompressedRaw(byteArrayReader.readBytes(Configuration.RAW_PUBLICKEY_LENGTH),Configuration.ELIPTIC_CURVE));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            throw new BlockChainObjectParsingException();
        }
        patientInfo.setSignature(byteArrayReader.readBytes(Configuration.SIGNATURE_LENGTH));

        return patientInfo;
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



    public boolean verify()
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
        PatientInfo that = (PatientInfo) o;
        return timestamp == that.timestamp &&
                Objects.equals(publicKey, that.publicKey) &&
                Arrays.equals(encryptedInfo, that.encryptedInfo) &&
                Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(timestamp, publicKey);
        result = 31 * result + Arrays.hashCode(encryptedInfo);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
}
