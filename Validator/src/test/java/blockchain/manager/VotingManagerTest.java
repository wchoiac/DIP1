package blockchain.manager;

import blockchain.block.AuthorityInfo;
import blockchain.block.Block;
import blockchain.block.BlockContent;
import blockchain.block.BlockHeader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import general.security.SecurityHelper;
import helper.BlockChainTestHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VotingManagerTest {

    @Test
    void saveAndLoadTest() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            BlockChainTestHelper.startTest(initialAuthorities);


            KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testAuthorityPublicKey = (ECPublicKey) testAuthorityKeyPair.getPublic();
            ECPrivateKey testAuthorityPrivateKey = (ECPrivateKey) testAuthorityKeyPair.getPrivate();
            AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);


            Block testBlock = new Block(testAuthorityPrivateKey, null, new AuthorityInfo[]{testAuthorityInfo}, 1
                    , (byte) 0, new byte[Configuration.HASH_LENGTH], testAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);

            BlockManager.saveBlock(testBlock);

            Block loadedBlock = BlockManager.loadBlock(testBlock.calculateHash());
            BlockHeader loadedBlockHeader = BlockManager.loadBlockHeader(testBlock.calculateHash());
            BlockContent loadedBlockContent = BlockManager.loadBlockContent(loadedBlockHeader.getStructureIndicator(),testBlock.calculateHash());

            assertEquals(loadedBlock, testBlock);
            assertEquals(loadedBlockHeader, testBlock.getHeader());
            assertEquals(loadedBlockContent, testBlock.getContent());
        }
        finally {
            BlockChainTestHelper.endTest();
        }
    }
}
