package blockchain.block;


import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorityInfoTest {
    @Test
    void rawAndParseTest() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {


        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

        byte[] raw =testAuthorityInfo.getRaw();

        AuthorityInfo parsedAuthorityInfo =AuthorityInfo.parse(raw);


        assertEquals(testAuthorityInfo,parsedAuthorityInfo);
    }

}
