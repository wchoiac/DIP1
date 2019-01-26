package blockchain;

import blockchain.block.AuthorityInfo;
import blockchain.internal.Voting;
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

public class StatusTest {
    @Test
    void rawAndParseTest() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException {

        byte[] latestBlockHash = SecurityHelper.hash(new byte[]{1},Configuration.BLOCKCHAIN_HASH_ALGORITHM);

        Status testStatus = new Status(7,latestBlockHash);

        byte[] raw = testStatus.getRaw();

        Status parsedStatus = Status.parse(raw);

        assertEquals(parsedStatus,testStatus);
    }

}
