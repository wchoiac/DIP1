package blockchain.internal;

import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StateInfoTest {

    @Test
    void rawAndParseTest() throws NoSuchAlgorithmException{


        byte[] testLatestVotingListBlockHash = SecurityHelper.hash(new byte[]{0}, Configuration.BLOCKCHAIN_HASH_ALGORITHM);
        byte[] testLatestAuthorityListBlockHash = SecurityHelper.hash(new byte[]{1},Configuration.BLOCKCHAIN_HASH_ALGORITHM);
        StateInfo testStateInfo = new StateInfo(testLatestVotingListBlockHash,testLatestAuthorityListBlockHash
                ,7,7);

        byte[] raw = testStateInfo.getRaw();

        StateInfo parsedStateInfo = StateInfo.parse(raw);

        assertEquals(parsedStateInfo,testStateInfo);
    }
}
