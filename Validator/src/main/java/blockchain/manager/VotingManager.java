package blockchain.manager;

import blockchain.internal.StateInfo;
import blockchain.internal.Voting;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;

import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;


public class VotingManager implements Serializable {

    public static void save(byte[] blockHash, ArrayList<Voting> votingList) throws IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File votingListFile = new File(blockFolder, "voting");

        if (!blockFolder.exists())
            blockFolder.mkdirs();
        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(votingListFile))) {
            for (Voting voting : votingList) {
                os.write(voting.getRaw());
            }
        }


    }

    public static ArrayList<Voting> load(byte[] blockHash) throws IOException, BlockChainObjectParsingException {
        StateInfo stateInfo = StateInfoManager.load(blockHash);
        String latestVotingListBlockHashString = GeneralHelper.bytesToStringHex(stateInfo.getLatestVotingListBlockHash());

        File votingListFile = new File(Configuration.BLOCK_FOLDER, latestVotingListBlockHashString.charAt(0) + "/" + latestVotingListBlockHashString.charAt(1) + "/"
                + latestVotingListBlockHashString.charAt(2) + "/" + latestVotingListBlockHashString.charAt(3) + "/" + latestVotingListBlockHashString.charAt(4) + "/" + latestVotingListBlockHashString + "/voting");

        ArrayList<Voting> loadedVotingList = new ArrayList<>();
        byte[] votingAllBytes = Files.readAllBytes(votingListFile.toPath());

        ByteArrayReader byteArrayReader = new ByteArrayReader();
        byteArrayReader.set(votingAllBytes);
        while (!byteArrayReader.isFinished()) {
            loadedVotingList.add(Voting.parse(byteArrayReader));
        }

        return loadedVotingList;
    }
}
