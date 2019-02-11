package blockchain.manager;

import blockchain.block.Block;
import blockchain.block.BlockContent;
import blockchain.block.transaction.Transaction;
import blockchain.manager.datastructure.Location;
import blockchain.manager.datastructure.RecordShortInfo;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;

public class TransactionManager {

    // blockHash(32 bytes) || timestamp(8 bytes) || name length (1 byte) || medicalOrg Name (variable bytes)
    public static void save(byte[] blockHash, Transaction transaction, String medicalOrgName) throws IOException {
        String patientIdentifierString = GeneralHelper.bytesToStringHex(transaction.getPatientIdentifier());
        String transactionHashString = GeneralHelper.bytesToStringHex(transaction.calculateHash());

        File patientTransactionFolder = new File(Configuration.PATIENT_FOLDER, patientIdentifierString.charAt(0) + "/" + patientIdentifierString.charAt(1) + "/"
                + patientIdentifierString.charAt(2) + "/" + patientIdentifierString.charAt(3) + "/" + patientIdentifierString.charAt(4) + "/" + patientIdentifierString + "/transactions");
        File patientTransactionFile = new File(patientTransactionFolder, transactionHashString);

        if (!patientTransactionFolder.exists())
            patientTransactionFolder.mkdirs();

        try(BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(patientTransactionFile, true))) {

            os.write(blockHash);
            os.write(GeneralHelper.longToBytes(transaction.getTimestamp()));
            byte[] medicalOrgNameBytes = medicalOrgName.getBytes();
            os.write(medicalOrgNameBytes.length);
            os.write(medicalOrgName.getBytes());
        }

    }

    //blockHash here is the current blockchain head block's hash
    public static Transaction load(Location location) throws BlockChainObjectParsingException, IOException {
        Block block = BlockManager.loadBlock(location.getBlockHash());
        if (block == null||block.getContent().getTransactions()==null)
            return null;

        for (Transaction transaction : block.getContent().getTransactions()) {
            if (Arrays.equals(transaction.calculateHash(), location.getTargetIdentifier()))
                return transaction;
        }
        return null;
    }

    //blockHash here is the current blockchain head block's hash
    public static boolean isTransactionUnique(byte[] blockHash, byte[] transactionHash, byte[] patientIdentifier) throws IOException, BlockChainObjectParsingException {
        String patientIdentifierString = GeneralHelper.bytesToStringHex(patientIdentifier);
        String transactionHashString = GeneralHelper.bytesToStringHex(transactionHash);

        File patientTransactionFolder = new File(Configuration.PATIENT_FOLDER, patientIdentifierString.charAt(0) + "/" + patientIdentifierString.charAt(1) + "/"
                + patientIdentifierString.charAt(2) + "/" + patientIdentifierString.charAt(3) + "/" + patientIdentifierString.charAt(4) + "/" + patientIdentifierString + "/transactions");
        File patientTransactionFile = new File(patientTransactionFolder, transactionHashString);

        if (!patientTransactionFolder.exists() || !patientTransactionFile.exists()) {
            return true;
        }


        byte[] allBytes = Files.readAllBytes(patientTransactionFile.toPath());
        int offset = 0;

        while (offset < allBytes.length) {
            if (BlockChainManager.isThisBlockOnTheChain(blockHash, Arrays.copyOfRange(allBytes, offset, offset + Configuration.HASH_LENGTH)))
                return false;
            offset += Long.BYTES + allBytes[offset + Long.BYTES] + 1; // blockHash(32 bytes) || timestamp(8 bytes) || name length (1 byte) || medicalOrg Name (name length bytes)
        }

        return true;
    }

    //blockHash here is the current blockchain head block's hash
    public static ArrayList<RecordShortInfo> loadEveryRecordShortInfo(byte[] blockHash, byte[] patientIdentifier) throws IOException, BlockChainObjectParsingException {
        String patientIdentifierString = GeneralHelper.bytesToStringHex(patientIdentifier);

        File patientFolder = new File(Configuration.PATIENT_FOLDER, patientIdentifierString.charAt(0) + "/" + patientIdentifierString.charAt(1) + "/"
                + patientIdentifierString.charAt(2) + "/" + patientIdentifierString.charAt(3) + "/" + patientIdentifierString.charAt(4) + "/" + patientIdentifierString);

        if (!patientFolder.exists()) {
            return null;
        }


        File patientTransactionsFolder = new File(patientFolder, "/transactions");

        ArrayList<RecordShortInfo> recordShortInfos = new ArrayList<>();

        if (!patientTransactionsFolder.exists()) {
            return recordShortInfos;
        }

        for (File transactionFile : patientTransactionsFolder.listFiles()) {
            // blockHash(32 bytes) || timestamp(8 bytes) || name length (1 byte) || medicalOrg Name (name length bytes)
            byte[] allBytes = Files.readAllBytes(transactionFile.toPath());

            int offset = 0;
            while (offset < allBytes.length) {
                byte[] processingBlockHash = Arrays.copyOfRange(allBytes, offset, offset + Configuration.HASH_LENGTH);
                if (BlockChainManager.isThisBlockOnTheChain(blockHash, processingBlockHash)) {
                    byte[] transactionHash = GeneralHelper.stringHexToByteArray(transactionFile.getName());
                    offset += Configuration.HASH_LENGTH;
                    long timestamp = GeneralHelper.bytesToLong(Arrays.copyOfRange(allBytes, offset, offset + Long.BYTES));
                    offset += Long.BYTES;
                    String name = new String(Arrays.copyOfRange(allBytes, offset + 1,offset+1+ allBytes[offset]));
                    recordShortInfos.add(new RecordShortInfo(new Location(processingBlockHash, transactionHash), timestamp, name));
                    break;
                }
                offset += Long.BYTES + allBytes[offset + Long.BYTES] + 1; // blockHash(32 bytes) || timestamp(8 bytes) || name length (1 byte) || medicalOrg Name (name length bytes)
            }
        }


        return recordShortInfos;
    }

}
