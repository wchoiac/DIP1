package blockchain.manager;

import blockchain.block.Block;
import blockchain.internal.ChainInfo;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class ChainInfoManager {

    //ChainInfo = prev blockHash(32 bytes) | 1/0 (isBestChain/not) | nex blockHash(32 bytes - if main chain)
    public static void save(Block block) throws IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(block.calculateHash());
        File processingBlockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File chainInfoFile = new File(processingBlockFolder, "chainInfo");

        if (!processingBlockFolder.exists())
            processingBlockFolder.mkdirs();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(chainInfoFile))) {
            bos.write(block.getHeader().getPrevHash());
            bos.write(0);
        }

    }

    public static ChainInfo load(byte[] blockHash) throws IOException, BlockChainObjectParsingException {
        ChainInfo chainInfo = null;

        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        File chainInfoFile = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/chainInfo");
        if (!chainInfoFile.exists())
            return null;


        byte[] chainInfoAllBytes = Files.readAllBytes(chainInfoFile.toPath());

        return ChainInfo.parse(chainInfoAllBytes);
    }

    public static void changeChainInfo(byte[] blockHash, boolean isBestChain, byte[] successorBlockHash) throws IOException {


        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        File chainInfoFile = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/chainInfo");

        byte[] chainInfoAllBytes = Files.readAllBytes(chainInfoFile.toPath());
        byte[] prevBlockHash = Arrays.copyOfRange(chainInfoAllBytes, 0, Configuration.HASH_LENGTH);

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(chainInfoFile))) {
            bos.write(prevBlockHash);
            bos.write(isBestChain ? 1 : 0);
            if (successorBlockHash != null)
                bos.write(successorBlockHash);
        }
    }
}
