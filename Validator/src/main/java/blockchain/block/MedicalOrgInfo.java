package blockchain.block;

import blockchain.interfaces.Identifiable;
import blockchain.interfaces.Raw;
import blockchain.utility.BlockChainSecurityHelper;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;
import general.security.SecurityHelper;

import java.io.Serializable;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;

public class MedicalOrgInfo implements Identifiable, Raw {
    private String name; // max size =255
    private ECPublicKey publicKey;

    public MedicalOrgInfo(String name, ECPublicKey publicKey)
    {
        this.setName(name);
        this.setPublicKey(publicKey);
    }
    public MedicalOrgInfo()
    {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ECPublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(ECPublicKey publicKey) {
        this.publicKey = publicKey;
    }


    public byte[] getRaw() {
        return GeneralHelper.mergeByteArrays(new byte[]{(byte)name.length()},getName().getBytes()
                ,SecurityHelper.getCompressedRawECPublicKey(getPublicKey(),Configuration.ELIPTIC_CURVE));
    }

    public static MedicalOrgInfo parse(ByteArrayReader byteArrayReader) throws BlockChainObjectParsingException {
        MedicalOrgInfo medicalOrgInfo = new MedicalOrgInfo();

        int nameLength = byteArrayReader.readByte();
        medicalOrgInfo.setName(new String(byteArrayReader.readBytes(nameLength)));
        try {
            medicalOrgInfo.setPublicKey(SecurityHelper.getECPublicKeyFromCompressedRaw(
                    byteArrayReader.readBytes(Configuration.RAW_PUBLICKEY_LENGTH),Configuration.ELIPTIC_CURVE));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            throw new BlockChainObjectParsingException();
        }

        return medicalOrgInfo;
    }

    public static MedicalOrgInfo parse(byte[] raw) throws BlockChainObjectParsingException {
        ByteArrayReader byteArrayReader = new ByteArrayReader();
        byteArrayReader.set(raw);
        MedicalOrgInfo medicalOrgInfo = parse(byteArrayReader);

        if(!byteArrayReader.isFinished())
            throw new BlockChainObjectParsingException();

        return medicalOrgInfo;
    }

    public byte[] getIdentifier()
    {
        return BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(publicKey);
    }


    // identified by publickey only
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedicalOrgInfo that = (MedicalOrgInfo) o;
        return Arrays.equals(
                BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(getPublicKey())
                ,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey( that.getPublicKey()));
    }

    // identified by publickey only
    @Override
    public int hashCode() {
        return Objects.hash(getPublicKey());
    }

}
