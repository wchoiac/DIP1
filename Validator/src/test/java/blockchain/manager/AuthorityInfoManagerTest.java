package blockchain.manager;

import blockchain.block.*;
import blockchain.internal.AuthorityInfoForInternal;
import blockchain.internal.StateInfo;
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
import static org.junit.jupiter.api.Assertions.assertNull;

public class AuthorityInfoManagerTest {

    @Test
    void trustAndLoadTest() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {
            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash =BlockChainTestHelper.startTest(initialAuthorities);

            //new authority
            KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testAuthorityPublicKey = (ECPublicKey) testAuthorityKeyPair.getPublic();
            ECPrivateKey testAuthorityPrivateKey = (ECPrivateKey) testAuthorityKeyPair.getPrivate();
            AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

            Vote vote = new Vote(testAuthorityInfo,true,true);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, vote, null, 1
                    , (byte) 0, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);

            BlockManager.saveBlock(testBlock);
            AuthorityInfoManager.trust(testBlock.calculateHash(),prevBlockHash,testAuthorityInfo);
            StateInfoManager.save(testBlock,true,true);
            ChainInfoManager.save(testBlock);

            AuthorityInfoForInternal authorityInfoForInternal =AuthorityInfoManager.load(testBlock.calculateHash(),testAuthorityInfo.getIdentifier());

            assertEquals(authorityInfoForInternal.getLastSignedBlockNumber(),-1);
            assertNull(authorityInfoForInternal.getUntrustedBlock());
            assertEquals(authorityInfoForInternal.getAuthorityInfo(), testAuthorityInfo);
        }
        finally {
            BlockChainTestHelper.endTest();
        }
    }
}
