package blockchain.internal;

import blockchain.block.AuthorityInfo;
import blockchain.utility.BlockChainSecurityHelper;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VotingTest {

    @Test
    void rawAndParseTest() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException {

        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

        Voting testVoting = new Voting(testAuthorityInfo,true);

        byte[] raw = testVoting.getRaw();

        Voting parsedVoting = Voting.parse(raw);

        assertEquals(parsedVoting,testVoting);
    }

    @Test
    void isExistingVoterTest() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException {

        KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testAuthorityPublicKey =(ECPublicKey) testAuthorityKeyPair.getPublic();
        ECPrivateKey testAuthorityPrivateKey =(ECPrivateKey) testAuthorityKeyPair.getPrivate();
        AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

        Voting testVoting = new Voting(testAuthorityInfo,true);


        KeyPair testAuthority2KeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        byte[] testAuthority2Identifier = BlockChainSecurityHelper.calculateIdentifierFromECPublicKey((ECPublicKey) testAuthority2KeyPair.getPublic());


        assertFalse(testVoting.isExistingVoter(testAuthority2Identifier));

        testVoting.addAgree(testAuthority2Identifier);
        assertTrue(testVoting.isExistingVoter(testAuthority2Identifier));

        testVoting.removeAgree(testAuthority2Identifier);
        assertFalse(testVoting.isExistingVoter(testAuthority2Identifier));

        testVoting.addDisagree(testAuthority2Identifier);
        assertTrue(testVoting.isExistingVoter(testAuthority2Identifier));

        testVoting.removeDisagree(testAuthority2Identifier);
        assertFalse(testVoting.isExistingVoter(testAuthority2Identifier));

    }
}
