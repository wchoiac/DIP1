package blockchain.manager;

import blockchain.BlockChain;
import blockchain.block.*;
import blockchain.block.transaction.Transaction;
import blockchain.internal.AuthorityInfoForInternal;
import blockchain.internal.MedicalOrgInfoForInternal;
import blockchain.manager.datastructure.Location;
import blockchain.manager.datastructure.RecordShortInfo;
import blockchain.utility.BlockChainSecurityHelper;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import helper.BlockChainTestHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class OverallManagerTest {


    @Test
    void trustAndLoadTest() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {
            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new authority
            KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testAuthorityPublicKey = (ECPublicKey) testAuthorityKeyPair.getPublic();
            ECPrivateKey testAuthorityPrivateKey = (ECPrivateKey) testAuthorityKeyPair.getPrivate();
            AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

            Vote vote = new Vote(testAuthorityInfo, true, true);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, vote, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);

            assertTrue(BlockChainManager.checkBlock(testBlock));

            BlockChainManager.storeBlock(testBlock);

            AuthorityInfoForInternal authorityInfoForInternal = AuthorityInfoManager.load(testBlock.calculateHash(), testAuthorityInfo.getIdentifier());

            assertEquals(authorityInfoForInternal.getLastSignedBlockNumber(), -1);
            assertNull(authorityInfoForInternal.getUntrustedBlock());
            assertEquals(authorityInfoForInternal.getAuthorityInfo(), testAuthorityInfo);
        } finally {
            BlockChainTestHelper.endTest();
        }
    }


    @Test
    void untrustTest() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {
            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new authority
            KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testAuthorityPublicKey = (ECPublicKey) testAuthorityKeyPair.getPublic();
            ECPrivateKey testAuthorityPrivateKey = (ECPrivateKey) testAuthorityKeyPair.getPrivate();
            AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

            //trust authority
            Vote vote = new Vote(testAuthorityInfo, true, true);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, vote, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);
           testBlock.getHeader().changeTimestamp(Configuration.BLOCK_PERIOD, testInitialAuthorityPrivateKey);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock));
            BlockChainManager.storeBlock(testBlock);

            AuthorityInfoForInternal authorityInfoForInternal = AuthorityInfoManager.load(testBlock.calculateHash(), testAuthorityInfo.getIdentifier());

            assertEquals(authorityInfoForInternal.getLastSignedBlockNumber(), -1);
            assertNull(authorityInfoForInternal.getUntrustedBlock());
            assertEquals(authorityInfoForInternal.getAuthorityInfo(), testAuthorityInfo);

            // new authority validation
            Vote vote2 = new Vote(testAuthorityInfo, false, true);
            Block testBlock2 = new Block(testAuthorityPrivateKey, vote2, null, 2
                    , Configuration.OUT_ORDER, testBlock.calculateHash(), testAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);
            testBlock2.getHeader().changeTimestamp(Configuration.BLOCK_PERIOD+Configuration.MIN_OUT_ORDER_BLOCK_PERIOD, testAuthorityPrivateKey);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock2.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock2));
            BlockChainManager.storeBlock(testBlock2);

            //untrust authority

            Vote vote3 = new Vote(testAuthorityInfo, false, true);
            Block testBlock3 = new Block(testInitialAuthorityPrivateKey, vote3, null, 3
                    , Configuration.OUT_ORDER, testBlock2.calculateHash(), testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);
            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock3.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock3));
            BlockChainManager.storeBlock(testBlock3);

            AuthorityInfoForInternal authorityInfoForInternal2 = AuthorityInfoManager.load(testBlock3.calculateHash(), testAuthorityInfo.getIdentifier());

            assertNotNull(authorityInfoForInternal2.getUntrustedBlock());


        } finally {
            BlockChainTestHelper.endTest();
        }
    }

    //empty block
    @Test
    void validBlockTest() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {
            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new authority
            KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testAuthorityPublicKey = (ECPublicKey) testAuthorityKeyPair.getPublic();
            ECPrivateKey testAuthorityPrivateKey = (ECPrivateKey) testAuthorityKeyPair.getPrivate();
            AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

            Vote vote = new Vote(testAuthorityInfo, true, true);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, vote, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock));

        } finally {
            BlockChainTestHelper.endTest();
        }
    }

    //authorization
    @Test
    void validBlockTest2() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {

            long timestamp = System.currentTimeMillis();

            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            KeyPair testMedicalOrgKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeyPair.getPublic();
            ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey) testMedicalOrgKeyPair.getPrivate();
            MedicalOrgInfo testMedicalOrgInfo = new MedicalOrgInfo("test", testMedicalOrgPublicKey);

            Block testBlock = new Block(testInitialAuthorityPrivateKey, null, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , new MedicalOrgInfo[]{testMedicalOrgInfo}
                    , null
                    , null
                    , null);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock));


            BlockChainManager.storeBlock(testBlock);


            MedicalOrgInfoForInternal testMedicalOrgInfoForInternal =MedicalOrgInfoManager.load(testBlock.calculateHash(),testMedicalOrgInfo.getIdentifier());
            assertNotNull(testMedicalOrgInfoForInternal);
            assertEquals(testMedicalOrgInfoForInternal.getMedicalOrgInfo()
                    ,testMedicalOrgInfo);


        } finally {
            BlockChainTestHelper.endTest();
        }
    }

    //revocation
    @Test
    void validBlockTest3() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {
            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            KeyPair testMedicalOrgKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeyPair.getPublic();
            ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey) testMedicalOrgKeyPair.getPrivate();
            MedicalOrgInfo testMedicalOrgInfo = new MedicalOrgInfo("test", testMedicalOrgPublicKey);

            Block testBlock = new Block(testInitialAuthorityPrivateKey, null, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , new MedicalOrgInfo[]{testMedicalOrgInfo}
                    , null
                    , null
                    , null);
            testBlock.getHeader().changeTimestamp(Configuration.BLOCK_PERIOD, testInitialAuthorityPrivateKey);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock));
            BlockChainManager.storeBlock(testBlock);

            MedicalOrgInfoForInternal testMedicalOrgInfoForInternal =MedicalOrgInfoManager.load(testBlock.calculateHash(),testMedicalOrgInfo.getIdentifier());
            assertNotNull(testMedicalOrgInfoForInternal);
            assertEquals(testMedicalOrgInfoForInternal.getMedicalOrgInfo()
                    ,testMedicalOrgInfo);


            Block testBlock2 = new Block(testInitialAuthorityPrivateKey, null, null, 2
                    , Configuration.IN_ORDER, testBlock.calculateHash(), testInitialAuthorityPublicKey
                    , null
                    , new byte[][]{testMedicalOrgInfo.getIdentifier()}
                    , null
                    , null);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock2.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock2));
            BlockChainManager.storeBlock(testBlock2);

            assertTrue(MedicalOrgInfoManager.isRevoked(testBlock2.calculateHash(),testMedicalOrgInfo.getIdentifier()));
            assertNull(MedicalOrgInfoManager.load(testBlock2.calculateHash(),testMedicalOrgInfo.getIdentifier()));

        } finally {
            BlockChainTestHelper.endTest();
        }
    }


    //registration
    @Test
    void validBlockTest4() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {

            long timestamp = System.currentTimeMillis();

            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new patient
            KeyPair testPatientKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeyPair.getPublic();
            ECPrivateKey testPatientPrivateKey =(ECPrivateKey) testPatientKeyPair.getPrivate();

            byte[] encryptedInfo =new byte[10];
            byte[] signatureCoverage= GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp),encryptedInfo);
            byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                    , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);

            PatientInfo testPatientInfo = new PatientInfo(timestamp,testPatientPublicKey,encryptedInfo,signature);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, null, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , new PatientInfo[]{testPatientInfo}
                    , null);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock));

            BlockChainManager.storeBlock(testBlock);

            assertTrue(PatientInfoManager.patientExists(testBlock.calculateHash(),testPatientInfo.getPatientIdentifier()));
            assertTrue(PatientInfoManager.patientInfoExists(testBlock.calculateHash(),testPatientInfo.getPatientIdentifier(),testPatientInfo.calculateInfoHash()));
           assertEquals(PatientInfoManager.load(testBlock.calculateHash(),testPatientInfo.getPatientIdentifier()),testPatientInfo);


        } finally {
            BlockChainTestHelper.endTest();
        }
    }

    //All+ transaction
    @Test
    void validBlockTest5() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {
        try {


            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new patient
            KeyPair testPatientKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeyPair.getPublic();
            ECPrivateKey testPatientPrivateKey =(ECPrivateKey) testPatientKeyPair.getPrivate();

            //new medical org
            KeyPair testMedicalOrgKeypair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeypair.getPublic();
            ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey)testMedicalOrgKeypair.getPrivate();


            // authorize medical org and register patient
            byte[] encryptedInfo =new byte[10];
            byte[] signatureCoverage= GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(Configuration.BLOCK_PERIOD),encryptedInfo);
            byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                    , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);

            PatientInfo testPatientInfo = new PatientInfo(Configuration.BLOCK_PERIOD,testPatientPublicKey,encryptedInfo,signature);
            MedicalOrgInfo testMedicalOrgInfo = new MedicalOrgInfo("testMed", testMedicalOrgPublicKey);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, null, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , new MedicalOrgInfo[]{testMedicalOrgInfo}
                    , null
                    , new PatientInfo[]{testPatientInfo}
                    , null);
            testBlock.getHeader().changeTimestamp(Configuration.BLOCK_PERIOD, testInitialAuthorityPrivateKey);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock));
            BlockChainManager.storeBlock(testBlock);


            //test for transaction
            long timestamp = System.currentTimeMillis();
            byte[] content =new byte[10];
            byte[] signatureCoverage2=GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp)
                    , content,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey));
            byte[] signature2 = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage2
                    , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);


            Transaction testTransaction = new Transaction(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey)
                    , timestamp,content, signature2
                    ,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testPatientPublicKey)
                    ,testMedicalOrgPrivateKey);

            Block testBlock2 = new Block(testInitialAuthorityPrivateKey, null, null, 2
                    , Configuration.IN_ORDER, testBlock.calculateHash(), testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , new Transaction[]{testTransaction});

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock2.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock2));

            BlockChainManager.storeBlock(testBlock2);

            assertFalse(TransactionManager.isTransactionUnique(testBlock2.calculateHash(),testTransaction.calculateHash(),testPatientInfo.getPatientIdentifier()));
            assertEquals(TransactionManager.load(new Location(testBlock2.calculateHash(),testTransaction.calculateHash())),testTransaction);

            ArrayList<RecordShortInfo> recordShortInfos =TransactionManager.loadEveryRecordShortInfo(testBlock2.calculateHash(),testPatientInfo.getPatientIdentifier());

            assertEquals(recordShortInfos.size(),1);
            assertArrayEquals(recordShortInfos.get(0).getLocation().getBlockHash(),testBlock2.calculateHash());
            assertArrayEquals(recordShortInfos.get(0).getLocation().getTargetIdentifier(),testTransaction.calculateHash());
            assertEquals(recordShortInfos.get(0).getTimestamp(),testTransaction.getTimestamp());
            assertEquals(recordShortInfos.get(0).getMedicalOrgName(),testMedicalOrgInfo.getName());


        } finally {
            BlockChainTestHelper.endTest();
        }
    }




    //initial disagree
    @Test
    void invalidBlockTest() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {

            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new authority
            KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testAuthorityPublicKey = (ECPublicKey) testAuthorityKeyPair.getPublic();
            ECPrivateKey testAuthorityPrivateKey = (ECPrivateKey) testAuthorityKeyPair.getPrivate();
            AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);


            Vote vote = new Vote(testAuthorityInfo, true, false);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, vote, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);

            assertEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertFalse(BlockChainManager.checkBlock(testBlock));

        } finally {
            BlockChainTestHelper.endTest();
        }
    }

    //add existing authority
    @Test
    void invalidBlockTest2() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {

            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new authority
            KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testAuthorityPublicKey = (ECPublicKey) testAuthorityKeyPair.getPublic();
            ECPrivateKey testAuthorityPrivateKey = (ECPrivateKey) testAuthorityKeyPair.getPrivate();
            AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

            Vote vote = new Vote(testAuthorityInfo, false, true);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, vote, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);

            assertEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertFalse(BlockChainManager.checkBlock(testBlock));

        } finally {
            BlockChainTestHelper.endTest();
        }
    }



    //remove non-existing authority
    @Test
    void invalidBlockTest3() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {

            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new authority
            KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testAuthorityPublicKey = (ECPublicKey) testAuthorityKeyPair.getPublic();
            ECPrivateKey testAuthorityPrivateKey = (ECPrivateKey) testAuthorityKeyPair.getPrivate();
            AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

            //remove non-existing authority
            Vote vote = new Vote(testAuthorityInfo, false, false);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, vote, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);

            assertEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertFalse(BlockChainManager.checkBlock(testBlock));



        } finally {
            BlockChainTestHelper.endTest();
        }
    }



    //wrong signature registration
    @Test
    void invalidBlockTest4() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {

            long timestamp = System.currentTimeMillis();

            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new patient
            KeyPair testPatientKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeyPair.getPublic();
            ECPrivateKey testPatientPrivateKey =(ECPrivateKey) testPatientKeyPair.getPrivate();

            byte[] encryptedInfo =new byte[10];
            byte[] signatureCoverage= GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp),encryptedInfo);
            byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                    , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
            signature[0]-=1;

            PatientInfo testPatientInfo = new PatientInfo(timestamp,testPatientPublicKey,encryptedInfo,signature);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, null, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , new PatientInfo[]{testPatientInfo}
                    , null);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertFalse(BlockChainManager.checkBlock(testBlock));


        } finally {
            BlockChainTestHelper.endTest();
        }
    }

    //wrong signature transaction
    @Test
    void invalidBlockTest5() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {


            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new patient
            KeyPair testPatientKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeyPair.getPublic();
            ECPrivateKey testPatientPrivateKey =(ECPrivateKey) testPatientKeyPair.getPrivate();

            //new medical org
            KeyPair testMedicalOrgKeypair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeypair.getPublic();
            ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey)testMedicalOrgKeypair.getPrivate();


            // authorize medical org and register patient
            byte[] encryptedInfo =new byte[10];
            byte[] signatureCoverage= GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(Configuration.BLOCK_PERIOD),encryptedInfo);
            byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                    , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);

            PatientInfo testPatientInfo = new PatientInfo(Configuration.BLOCK_PERIOD,testPatientPublicKey,encryptedInfo,signature);
            MedicalOrgInfo testMedicalOrgInfo = new MedicalOrgInfo("testMed", testMedicalOrgPublicKey);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, null, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , new MedicalOrgInfo[]{testMedicalOrgInfo}
                    , null
                    , new PatientInfo[]{testPatientInfo}
                    , null);
            testBlock.getHeader().changeTimestamp(Configuration.BLOCK_PERIOD, testInitialAuthorityPrivateKey);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock));
            BlockChainManager.storeBlock(testBlock);


            //test for transaction
            long timestamp = System.currentTimeMillis();
            byte[] content =new byte[10];
            byte[] signatureCoverage2=GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(timestamp)
                    , content,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey));
            byte[] signature2 = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage2
                    , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
            signature2[0]-=1;

            Transaction testTransaction = new Transaction(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testMedicalOrgPublicKey)
                    , timestamp,content, signature2
                    ,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(testPatientPublicKey)
                    ,testMedicalOrgPrivateKey);

            Block testBlock2 = new Block(testInitialAuthorityPrivateKey, null, null, 2
                    , Configuration.IN_ORDER, testBlock.calculateHash(), testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , new Transaction[]{testTransaction});

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock2.getHeader()}),-1);
            assertFalse(BlockChainManager.checkBlock(testBlock2));

        } finally {
            BlockChainTestHelper.endTest();
        }
    }

    //not following validation interval
    @Test
    void invalidBlockTest6() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {
            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new authority
            KeyPair testAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testAuthorityPublicKey = (ECPublicKey) testAuthorityKeyPair.getPublic();
            ECPrivateKey testAuthorityPrivateKey = (ECPrivateKey) testAuthorityKeyPair.getPrivate();
            AuthorityInfo testAuthorityInfo = new AuthorityInfo("test", testAuthorityPublicKey);

            //trust authority
            Vote vote = new Vote(testAuthorityInfo, true, true);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, vote, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);
            testBlock.getHeader().changeTimestamp(Configuration.BLOCK_PERIOD, testInitialAuthorityPrivateKey);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock));
            BlockChainManager.storeBlock(testBlock);

            AuthorityInfoForInternal authorityInfoForInternal = AuthorityInfoManager.load(testBlock.calculateHash(), testAuthorityInfo.getIdentifier());

            assertEquals(authorityInfoForInternal.getLastSignedBlockNumber(), -1);
            assertNull(authorityInfoForInternal.getUntrustedBlock());
            assertEquals(authorityInfoForInternal.getAuthorityInfo(), testAuthorityInfo);

            //untrust authority

            Vote vote2 = new Vote(testAuthorityInfo, false, true);
            Block testBlock2 = new Block(testInitialAuthorityPrivateKey, vote2, null, 2
                    , Configuration.IN_ORDER, testBlock.calculateHash(), testInitialAuthorityPublicKey
                    , null
                    , null
                    , null
                    , null);

            assertEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock2.getHeader()}),-1);

        } finally {
            BlockChainTestHelper.endTest();
        }
    }

    //duplicate patient info
    @Test
    void invalidBlockTest7() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, BlockChainObjectParsingException, IOException, FileCorruptionException {

        try {


            // initial authority
            KeyPair testInitialAuthorityKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testInitialAuthorityPublicKey = (ECPublicKey) testInitialAuthorityKeyPair.getPublic();
            ECPrivateKey testInitialAuthorityPrivateKey = (ECPrivateKey) testInitialAuthorityKeyPair.getPrivate();
            AuthorityInfo testInitialAuthorityInfo = new AuthorityInfo("test", testInitialAuthorityPublicKey);
            AuthorityInfo[] initialAuthorities = new AuthorityInfo[]{testInitialAuthorityInfo};

            byte[] prevBlockHash = BlockChainTestHelper.startTest(initialAuthorities);

            //new patient
            KeyPair testPatientKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testPatientPublicKey =(ECPublicKey) testPatientKeyPair.getPublic();
            ECPrivateKey testPatientPrivateKey =(ECPrivateKey) testPatientKeyPair.getPrivate();

            //new medical org
            KeyPair testMedicalOrgKeypair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
            ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeypair.getPublic();
            ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey)testMedicalOrgKeypair.getPrivate();


            // authorize medical org and register patient
            byte[] encryptedInfo =new byte[10];
            byte[] signatureCoverage= GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(Configuration.BLOCK_PERIOD),encryptedInfo);
            byte[] signature = SecurityHelper.createRawECDSASignatureWithContent(testPatientPrivateKey, signatureCoverage
                    , Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);

            PatientInfo testPatientInfo = new PatientInfo(Configuration.BLOCK_PERIOD,testPatientPublicKey,encryptedInfo,signature);
            Block testBlock = new Block(testInitialAuthorityPrivateKey, null, null, 1
                    , Configuration.IN_ORDER, prevBlockHash, testInitialAuthorityPublicKey
                    , null
                    , null
                    , new PatientInfo[]{testPatientInfo}
                    , null);
            testBlock.getHeader().changeTimestamp(Configuration.BLOCK_PERIOD, testInitialAuthorityPrivateKey);

            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock.getHeader()}),-1);
            assertTrue(BlockChainManager.checkBlock(testBlock));
            BlockChainManager.storeBlock(testBlock);

            Block testBlock2 = new Block(testInitialAuthorityPrivateKey, null, null, 2
                    , Configuration.IN_ORDER, testBlock.calculateHash(), testInitialAuthorityPublicKey
                    , null
                    , null
                    , new PatientInfo[]{testPatientInfo}
                    , null);
            assertNotEquals(BlockChainManager.checkBlockHeaders(new BlockHeader[]{testBlock2.getHeader()}),-1);
            assertFalse(BlockChainManager.checkBlock(testBlock2));


        } finally {
            BlockChainTestHelper.endTest();
        }
    }

}