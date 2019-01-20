package helper;

import blockchain.Status;
import blockchain.block.AuthorityInfo;
import blockchain.block.Block;
import blockchain.block.BlockContent;
import blockchain.block.BlockHeader;
import blockchain.internal.StateInfo;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import general.utility.GeneralHelper;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BlockChainTestHelper {

    private static final ReentrantLock testLock= new ReentrantLock();

    public static byte[] startTest(AuthorityInfo[] initialAuthorities) throws BlockChainObjectParsingException, IOException, FileCorruptionException {
        testLock.lock();
        Block genesisBlock = new Block(null, null, initialAuthorities
                , 0, (byte) 0, new byte[Configuration.HASH_LENGTH], null
                , null, null, null,null);
        genesisBlock.getHeader().setTimestamp(0);
        genesisLoad(genesisBlock);

        return genesisBlock.calculateHash();
    }

    public static void endTest() throws IOException {
        if(Configuration.API_DATA_FOLDER.exists())
            deleteRecursive(Configuration.API_DATA_FOLDER);
        if(Configuration.BLOCKCHAIN_DATA_FOLDER.exists())
            deleteRecursive(Configuration.BLOCKCHAIN_DATA_FOLDER);
        testLock.unlock();
    }

    private static boolean deleteRecursive(File folder) throws FileNotFoundException{
        if (!folder.exists()) throw new FileNotFoundException(folder.getAbsolutePath());
        boolean ret = true;
        if (folder.isDirectory()){
            for (File f : folder.listFiles()){
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && folder.delete();
    }


    public static void genesisLoad(Block genesisBlock) throws IOException, BlockChainObjectParsingException, FileCorruptionException {
        // create index file with the genesisfile

        byte[] blockHash = genesisBlock.calculateHash();


        if (!Configuration.BLOCKCHAIN_DATA_FOLDER.exists()) {
            genesisBlockSave(genesisBlock);
            genesisAuthoritySave(blockHash, genesisBlock.getContent().getInitialAuthorities());
            genesisBestChainSave(new Status(0, blockHash));
            genesisChainInfoSave(genesisBlock);
            genesisStateInfoSave(genesisBlock);
            genesisVotingSave(blockHash);
        } else {
            System.out.println("Error: Data folder already exists, if genesis block needs to be loaded again, please delete the folder first.");
            return;
        }
    }


    public static void genesisBlockSave(Block block) throws IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(block.calculateHash());

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File blockHeaderFile = new File(blockFolder, "header");
        File blockContentFile = new File(blockFolder, "content");

        blockFolder.mkdirs();
        blockHeaderFile.createNewFile();
        blockContentFile.createNewFile();

        try (FileOutputStream os = new FileOutputStream(blockHeaderFile)) {
            os.write(block.getHeader().getRaw());
        }

        try (FileOutputStream os = new FileOutputStream(blockContentFile)) {
            os.write(block.getContent().getRaw());

        }
    }


    public static void genesisVotingSave(byte[] blockHash) throws IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File votingListFile = new File(blockFolder, "voting");

        blockFolder.mkdirs();
        votingListFile.createNewFile(); //empty file
    }

    public static void genesisStateInfoSave(Block block) throws IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(block.calculateHash());

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File stateInfoFile = new File(blockFolder, "stateInfo");

        if (!blockFolder.exists())
            blockFolder.mkdirs();
        StateInfo blockStateInfo = new StateInfo(block.calculateHash(), block.calculateHash(), 0, block.getContent().getInitialAuthorities().length);
        try (FileOutputStream os = new FileOutputStream(stateInfoFile)) {
            os.write(blockStateInfo.getRaw());
        }
    }

    private static void genesisChainInfoSave(Block block) throws IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(block.calculateHash());
        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File chainInfoFile = new File(blockFolder, "chainInfo");

        if (!blockFolder.exists())
            blockFolder.mkdirs();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(chainInfoFile))) {
            bos.write(block.getHeader().getPrevHash());
            bos.write(1); //true
        }

    }

    public static void genesisBestChainSave(Status status) throws IOException {

        if (!Configuration.BLOCKCHAIN_DATA_FOLDER.exists()) {
            Configuration.BLOCKCHAIN_DATA_FOLDER.mkdirs();
        }
        try (FileOutputStream os = new FileOutputStream(Configuration.BEST_CHAIN_FILE)) {
            os.write(status.getRaw());
        }
    }

    public static void genesisAuthoritySave(byte[] blockHash, AuthorityInfo[] initialAuthorityInfos) throws IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString);

        File authoritiesFile = new File(blockFolder, "authorities");

        if (!blockFolder.exists()) {
            blockFolder.mkdirs();
        }

        //write overall list
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(authoritiesFile));
        for (AuthorityInfo authorityInfo : initialAuthorityInfos) {
            bos.write(authorityInfo.getIdentifier());
        }
        bos.close();

        //process each authority
        for (AuthorityInfo authorityInfo : initialAuthorityInfos) {

            String authorityIdentifierString = GeneralHelper.bytesToStringHex(authorityInfo.getIdentifier());

            File authorityFolder = new File(Configuration.AUTHORITY_FOLDER, authorityIdentifierString.charAt(0) + "/" + authorityIdentifierString.charAt(1) + "/" + authorityIdentifierString + "/");
            File trustFile = new File(authorityFolder, "trust");

            if (!authorityFolder.exists()) {
                authorityFolder.mkdirs();
            }
            try (FileOutputStream os = new FileOutputStream(trustFile, true)) {
                os.write(blockHash);
            }
        }


    }
}
