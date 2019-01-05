package blockchain.manager;

import blockchain.block.Vote;
import exception.BlockChainObjectParsingException;
import config.Configuration;
import general.utility.GeneralHelper;

import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class MyVoteManager {

    // my vote could be dropped if the voting about the vote's beneficiary already processed
    public static void drop(byte[] blockHash, byte[] myIdentifier, Vote vote) throws IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        String myIdentifierString = GeneralHelper.bytesToStringHex(myIdentifier);


        File droppedVotesFolder = new File(Configuration.DROPPED_VOTE_FOLDER, myIdentifierString);
        File droppedVoteFile = new File(droppedVotesFolder, blockHashString);

        if (!droppedVotesFolder.exists())
            droppedVotesFolder.mkdirs();

        try (FileOutputStream os = new FileOutputStream(droppedVoteFile)) {
            os.write(vote.getRaw());
        }
    }

    public static Vote loadDroppedVote(byte[] blockHash, byte[] myIdentifier) throws BlockChainObjectParsingException, IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        String myIdentifierString = GeneralHelper.bytesToStringHex(myIdentifier);

        File droppedVoteFile = new File(Configuration.DROPPED_VOTE_FOLDER, myIdentifierString + "/" + blockHashString);

        if (!droppedVoteFile.exists())
            return null;

        Vote loadedVote = null;

        byte[] voteAllBytes = Files.readAllBytes(droppedVoteFile.toPath());
        loadedVote = Vote.parse(voteAllBytes);

        return loadedVote;
    }
}
