package blockchain.manager;

import blockchain.internal.StateInfo;
import blockchain.block.Block;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;

import java.io.*;
import java.nio.file.Files;
//String format =  latest changed voting list | latest changed validator list | did medical organization get revoked | total score

public class StateInfoManager {

    public static void save(Block block, boolean authorityChange) throws BlockChainObjectParsingException, IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(block.calculateHash());
        String prevBlockHashString = GeneralHelper.bytesToStringHex(block.getHeader().getPrevHash());

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File stateInfoFile = new File(blockFolder, "stateInfo");

        File prevBlockStateInfoFile = new File(Configuration.BLOCK_FOLDER, prevBlockHashString.charAt(0) + "/" + prevBlockHashString.charAt(1) + "/"
                + prevBlockHashString.charAt(2) + "/" + prevBlockHashString.charAt(3) + "/" + prevBlockHashString.charAt(4) + "/" + prevBlockHashString + "/stateInfo");

        if (!blockFolder.exists())
            blockFolder.mkdirs();
        if (!prevBlockStateInfoFile.exists())
            throw new BlockChainObjectParsingException();


        byte[] prevStateInfoAllBytes = Files.readAllBytes(prevBlockStateInfoFile.toPath());
        StateInfo prevBlockStateInfo = StateInfo.parse(prevStateInfoAllBytes);

        boolean votingChange = block.getHeader().getVote() != null;
        byte[] latestVotingListBlockHash = votingChange ? block.calculateHash() : prevBlockStateInfo.getLatestVotingListBlockHash();
        byte[] latestAuthorityListBlockHash = authorityChange ? block.calculateHash() : prevBlockStateInfo.getLatestAuthorityListBlockHash();
        int totalScore = prevBlockStateInfo.getTotalScore() + block.getHeader().getScore();
        int totalAuthority = prevBlockStateInfo.getTotalAuthorities() + (authorityChange ? (block.getHeader().getVote().isAdd() ? 1 : 0) : 0);

        StateInfo blockStateInfo = new StateInfo(latestVotingListBlockHash, latestAuthorityListBlockHash, totalScore, totalAuthority);
        try (FileOutputStream os = new FileOutputStream(stateInfoFile)) {
            os.write(blockStateInfo.getRaw());
        }
    }

    public static StateInfo load(byte[] blockHash) throws IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        File stateInfoFile = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/stateInfo");

        if (!stateInfoFile.exists())
            return null;

        byte[] stateInfoAllBytes = Files.readAllBytes(stateInfoFile.toPath());
        return StateInfo.parse(stateInfoAllBytes);
    }

}
