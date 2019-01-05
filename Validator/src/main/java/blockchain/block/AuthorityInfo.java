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
import java.util.Objects;


public class AuthorityInfo implements Identifiable, Raw {

    //<= name length (1 byte)
    private String name; // max size =255
    private ECPublicKey publicKey;

    public AuthorityInfo(String name, ECPublicKey publicKey) {
        this.setPublicKey(publicKey);
        this.setName(name);

    }

    public AuthorityInfo() {
    }



    public byte[] getIdentifier()
    {
        return BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(publicKey);
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


    //start with name length (1 byte)
    public static AuthorityInfo parse(ByteArrayReader byteArrayReader) throws BlockChainObjectParsingException {
        AuthorityInfo authorityInfo = new AuthorityInfo();

        int nameLength =byteArrayReader.readByte();
        authorityInfo.setName(new String(byteArrayReader.readBytes(nameLength)));
        try {
            authorityInfo.setPublicKey(SecurityHelper.getECPublicKeyFromCompressedRaw(byteArrayReader.readBytes(Configuration.RAW_PUBLICKEY_LENGTH)
                    ,Configuration.ELIPTIC_CURVE));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            throw new BlockChainObjectParsingException();
        }

        return authorityInfo;
    }

    // identified by publickey only
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorityInfo that = (AuthorityInfo) o;
        return Objects.equals(getPublicKey(), that.getPublicKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash( getPublicKey());
    }

}
