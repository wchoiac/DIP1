package blockchain.manager;

import blockchain.block.Block;
import blockchain.block.BlockContent;
import blockchain.block.BlockHeader;
import exception.BlockChainObjectParsingException;
import config.Configuration;
import general.utility.GeneralHelper;

import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class BlockManager {

    public static void saveBlockContent(byte[] blockHash, BlockContent blockContent) throws IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);

        File processingBlockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File blockContentFile = new File(processingBlockFolder, "content");

        if (!processingBlockFolder.exists())
            processingBlockFolder.mkdirs();

        try (FileOutputStream os = new FileOutputStream(blockContentFile);) {
            os.write(blockContent.getRaw());

        }

    }

    public static BlockContent loadBlockContent(byte structureIndicator,byte[] blockHash) throws BlockChainObjectParsingException, IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        BlockContent loadedContent = null;

        File blockContentFile = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/content");

        if (!blockContentFile.exists())
            return null;

        byte[] contentAllBytes = Files.readAllBytes(blockContentFile.toPath());
        loadedContent = BlockContent.parse(structureIndicator,contentAllBytes);

        return loadedContent;
    }

    public static void saveBlockHeader(BlockHeader blockHeader) throws IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(blockHeader.calculateHash());

        File processingBlockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File blockHeaderFile = new File(processingBlockFolder, "header");

        if (!processingBlockFolder.exists())
            processingBlockFolder.mkdirs();

        try (FileOutputStream os = new FileOutputStream(blockHeaderFile);) {
            os.write(blockHeader.getRaw());
        }

    }

    public static BlockHeader loadBlockHeader(byte[] blockHash) throws BlockChainObjectParsingException, IOException {


        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        BlockHeader loadedHeader = null;

        File blockHeaderFile = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/header");

        if (!blockHeaderFile.exists())
            return null;


        byte[] headerAllBytes = Files.readAllBytes(blockHeaderFile.toPath());

        loadedHeader = BlockHeader.parse(headerAllBytes);


        return loadedHeader;
    }


    public static void saveBlock(Block block) throws IOException {
        if (block == null)
            return;
        saveBlockContent(block.calculateHash(), block.getContent());
        saveBlockHeader(block.getHeader());
    }

    public static Block loadBlock(byte[] blockHash) throws BlockChainObjectParsingException, IOException {

        BlockHeader header = loadBlockHeader(blockHash);
        BlockContent content = loadBlockContent(header.getStructureIndicator(),blockHash);

        if (header == null || content == null)
            return null;

        Block block = new Block();
        block.setHeader(header);
        block.setContent(content);
        return block;
    }
}
