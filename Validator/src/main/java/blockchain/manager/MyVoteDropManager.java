package blockchain.manager;

import blockchain.block.Vote;
import blockchain.utility.ByteArrayReader;
import exception.BlockChainObjectParsingException;
import config.Configuration;
import general.utility.GeneralHelper;

import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

public class MyVoteDropManager {

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

    // my vote could be dropped if the voting about the vote's beneficiary already processed
    public static void checkPointDrop(byte[] blockHash, byte[] myIdentifier, ArrayList<Vote> droppedVotes) throws IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        String myIdentifierString = GeneralHelper.bytesToStringHex(myIdentifier);


        File droppedVotesFolder = new File(Configuration.DROPPED_VOTE_FOLDER, myIdentifierString);
        File droppedVoteFile = new File(droppedVotesFolder, blockHashString);

        if (!droppedVotesFolder.exists())
            droppedVotesFolder.mkdirs();


        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(droppedVoteFile))) {
            for (Vote vote : droppedVotes) {
                os.write(vote.getRaw());
            }
        }
    }

    public static Vote load(byte[] blockHash, byte[] myIdentifier) throws BlockChainObjectParsingException, IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        String myIdentifierString = GeneralHelper.bytesToStringHex(myIdentifier);

        File droppedVoteFile = new File(Configuration.DROPPED_VOTE_FOLDER, myIdentifierString + "/" + blockHashString);

        if (!droppedVoteFile.exists())
            return null;

        Vote loadedVote;

        byte[] voteAllBytes = Files.readAllBytes(droppedVoteFile.toPath());
        loadedVote = Vote.parse(voteAllBytes);

        return loadedVote;
    }


    public static ArrayList<Vote> checkPointLoad(byte[] blockHash, byte[] myIdentifier) throws IOException, BlockChainObjectParsingException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);
        String myIdentifierString = GeneralHelper.bytesToStringHex(myIdentifier);

        File droppedVoteFile = new File(Configuration.DROPPED_VOTE_FOLDER, myIdentifierString + "/" + blockHashString);

        if (!droppedVoteFile.exists())
            return null;

        ArrayList<Vote> loadedDroppedVotes = new ArrayList<>();

        byte[] voteAllBytes = Files.readAllBytes(droppedVoteFile.toPath());

        ByteArrayReader byteArrayReader = new ByteArrayReader();

        byteArrayReader.set(voteAllBytes);
        while (!byteArrayReader.isFinished()) {
            loadedDroppedVotes.add(Vote.parse(byteArrayReader));
        }

        return loadedDroppedVotes;
    }
}
