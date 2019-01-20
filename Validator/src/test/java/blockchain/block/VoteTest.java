package blockchain.block;

import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VoteTest {


    @Test
    void rawAndParseTest() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {

        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);


        Vote testVote = new Vote(testAuthorityInfo,true,true);

        byte[] raw = testVote.getRaw();

        Vote parsedVote = Vote.parse(raw);

        assertEquals(parsedVote,testVote);
    }

}
