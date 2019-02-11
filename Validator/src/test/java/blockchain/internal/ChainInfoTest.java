package blockchain.internal;

import blockchain.block.AuthorityInfo;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainInfoTest {

    @Test
    void rawAndParseTest() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {


        byte[] testPrevHash = SecurityHelper.hash(new byte[]{0},Configuration.BLOCKCHAIN_HASH_ALGORITHM);
        byte[] testNextHash = SecurityHelper.hash(new byte[]{0},Configuration.BLOCKCHAIN_HASH_ALGORITHM);
        ChainInfo testChainInfo = new ChainInfo(testPrevHash,true,testNextHash);

        byte[] raw = testChainInfo.getRaw();

        ChainInfo parsedChainInfo = ChainInfo.parse(raw);

        assertEquals(parsedChainInfo,testChainInfo);
    }

}
