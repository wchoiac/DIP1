package blockchain.manager;

import blockchain.block.Block;
import blockchain.block.PatientInfo;
import blockchain.manager.datastructure.Location;
import blockchain.manager.datastructure.PatientShortInfo;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;


public class PatientInfoManager {


    public static void save(byte[] blockHash, PatientInfo patientInfo) throws IOException {
        String patientIdentifierString = GeneralHelper.bytesToStringHex(patientInfo.getPatientIdentifier());

        File patientFolder = new File(Configuration.PATIENT_FOLDER, patientIdentifierString.charAt(0) + "/" + patientIdentifierString.charAt(1) + "/"
                + patientIdentifierString.charAt(2) + "/" + patientIdentifierString.charAt(3) + "/" + patientIdentifierString.charAt(4) + "/" + patientIdentifierString + "/");
        File patientShortInfoFile = new File(patientFolder, "shortInfo");

        if (!patientFolder.exists())
            patientFolder.mkdirs();

        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(patientShortInfoFile, true))) {
            // infoHash (32 byte) || blockHash (32 byte)|| timestamp (8 byte)
            os.write(patientInfo.calculateInfoHash());
            os.write(blockHash);
            os.write(GeneralHelper.longToBytes(patientInfo.getTimestamp()));
        }
    }


    //blockHash here is the current blockchain head block's hash
    public static boolean patientInfoExists(byte[] blockHash, byte[] patientIdentifier, byte[] infoHash) throws BlockChainObjectParsingException, IOException {

        String patientIdentifierString = GeneralHelper.bytesToStringHex(patientIdentifier);

        File patientFolder = new File(Configuration.PATIENT_FOLDER, patientIdentifierString.charAt(0) + "/" + patientIdentifierString.charAt(1) + "/"
                + patientIdentifierString.charAt(2) + "/" + patientIdentifierString.charAt(3) + "/" + patientIdentifierString.charAt(4) + "/" + patientIdentifierString + "/");
        File patientShortInfoFile = new File(patientFolder, "shortInfo");

        if (!patientShortInfoFile.exists())
            return false;


        int expectedLength = Configuration.HASH_LENGTH * 2 + Long.BYTES;
        
        byte[] patientShortInfoAllBytes = Files.readAllBytes(patientShortInfoFile.toPath());
        if (patientShortInfoAllBytes.length % expectedLength != 0)
            throw new BlockChainObjectParsingException();

        int totalShortInfo = patientShortInfoAllBytes.length / expectedLength;

        for (int i = totalShortInfo; i > 0; --i) {
            // infoHash (32 byte) || blockHash (32 byte)|| timestamp (8 byte)
            byte[] processingInfoHash = Arrays.copyOfRange(patientShortInfoAllBytes, (i - 1) * expectedLength, (i - 1) * expectedLength + Configuration.HASH_LENGTH);
            byte[] processingBlockHash = Arrays.copyOfRange(patientShortInfoAllBytes, (i - 1) * expectedLength + Configuration.HASH_LENGTH, (i - 1) * expectedLength + 2 * Configuration.HASH_LENGTH);
            if (Arrays.equals(infoHash, processingInfoHash)&&BlockChainManager.isThisBlockOnTheChain(blockHash, processingBlockHash)) {
                    return true;
            }
        }


        return false;
    }

    //blockHash here is the current blockchain head block's hash
    public static boolean patientExists(byte[] blockHash, byte[] patientIdentifier) throws BlockChainObjectParsingException, IOException {

        String patientIdentifierString = GeneralHelper.bytesToStringHex(patientIdentifier);

        File patientShortInfoFile = new File(Configuration.PATIENT_FOLDER, patientIdentifierString.charAt(0) + "/" + patientIdentifierString.charAt(1) + "/"
                + patientIdentifierString.charAt(2) + "/" + patientIdentifierString.charAt(3) + "/" + patientIdentifierString.charAt(4) + "/" + patientIdentifierString + "/shortInfo");

        if (!patientShortInfoFile.exists())
            return false;


        byte[] patientShortInfoAllBytes = Files.readAllBytes(patientShortInfoFile.toPath());

        int expectedLength = Configuration.HASH_LENGTH * 2 + Long.BYTES;
        if (patientShortInfoAllBytes.length % expectedLength != 0)
            throw new BlockChainObjectParsingException();

        int totalInfo = patientShortInfoAllBytes.length / expectedLength;

        for (int i = totalInfo; i > 0; --i) {
            // infoHash (32 byte) || blockHash (32 byte)|| timestamp (8 byte)
            byte[] processingBlockHash = Arrays.copyOfRange(patientShortInfoAllBytes, (i - 1) * expectedLength + Configuration.HASH_LENGTH, (i - 1) * expectedLength + 2 * Configuration.HASH_LENGTH);
            if (BlockChainManager.isThisBlockOnTheChain(blockHash, processingBlockHash)) {
                return true;
            }
        }


        return false;
    }

    //blockHash here is the current blockchain head block's hash
    //load the last processed info of the patient - not necessarily the last timestamp
    public static PatientInfo load(byte[] blockHash, byte[] patientIdentifier) throws BlockChainObjectParsingException, IOException {

        String patientIdentifierString = GeneralHelper.bytesToStringHex(patientIdentifier);

        File patientFolder = new File(Configuration.PATIENT_FOLDER, patientIdentifierString.charAt(0) + "/" + patientIdentifierString.charAt(1) + "/"
                + patientIdentifierString.charAt(2) + "/" + patientIdentifierString.charAt(3) + "/" + patientIdentifierString.charAt(4) + "/" + patientIdentifierString);

        if (!patientFolder.exists())
            return null;

        File patientShortInfoFile = new File(patientFolder, "shortInfo");

        byte[] patientShortInfoAllBytes = Files.readAllBytes(patientShortInfoFile.toPath());

        int expectedLength = Configuration.HASH_LENGTH * 2 + Long.BYTES;
        if (patientShortInfoAllBytes.length % expectedLength != 0)
            throw new BlockChainObjectParsingException();

        int totalInfo = patientShortInfoAllBytes.length / expectedLength;

        for (int i = totalInfo; i > 0; --i) {
            // infoHash (32 byte) || blockHash (32 byte)|| timestamp (8 byte)
            byte[] processingInfoHash = Arrays.copyOfRange(patientShortInfoAllBytes, (i - 1) * expectedLength, (i - 1) * expectedLength + Configuration.HASH_LENGTH);
            byte[] processingBlockHash = Arrays.copyOfRange(patientShortInfoAllBytes, (i - 1) * expectedLength + Configuration.HASH_LENGTH, (i - 1) * expectedLength + 2 * Configuration.HASH_LENGTH);
            if (BlockChainManager.isThisBlockOnTheChain(blockHash, processingBlockHash)) {
                Block block = BlockManager.loadBlock(processingBlockHash);
                for (PatientInfo info : block.getContent().getPatientInfoList()) {
                    if (Arrays.equals(info.calculateInfoHash(), processingInfoHash))
                        return info;
                }
            }
        }

        return null;
    }

    //blockHash here is the current blockchain head block's hash
    public static byte[] loadEcnryptedInfo(Location location) throws BlockChainObjectParsingException, IOException {
        Block block = BlockManager.loadBlock(location.getBlockHash());

        if (block == null || block.getContent().getPatientInfoList() == null )
            return null;

        if (block.getContent().getPatientInfoList() != null) {
            for (PatientInfo patientInfo : block.getContent().getPatientInfoList()) {
                if (Arrays.equals(patientInfo.calculateInfoHash(), location.getTargetIdentifier()))
                    return patientInfo.getEncryptedInfo();
            }
        }

        return null;
    }

    //blockHash here is the current blockchain head block's hash
    public static ArrayList<PatientShortInfo> loadEveryShortInfo(byte[] blockHash, byte[] patientIdentifier) throws BlockChainObjectParsingException, IOException {
        String patientIdentifierString = GeneralHelper.bytesToStringHex(patientIdentifier);

        File patientFolder = new File(Configuration.PATIENT_FOLDER, patientIdentifierString.charAt(0) + "/" + patientIdentifierString.charAt(1) + "/"
                + patientIdentifierString.charAt(2) + "/" + patientIdentifierString.charAt(3) + "/" + patientIdentifierString.charAt(4) + "/" + patientIdentifierString + "/");
        File patientShortInfoFile = new File(patientFolder, "shortInfo");

        if (!patientShortInfoFile.exists())
            return null;


        int expectedLength = Configuration.HASH_LENGTH * 2 + Long.BYTES;
        ArrayList<PatientShortInfo> patientShortInfos = new ArrayList<>();

        byte[] patientShortInfoAllBytes = Files.readAllBytes(patientShortInfoFile.toPath());
        if (patientShortInfoAllBytes.length % expectedLength != 0)
            throw new BlockChainObjectParsingException();

        int totalShortInfo = patientShortInfoAllBytes.length / expectedLength;

        for (int i = totalShortInfo; i > 0; --i) {
            // infoHash (32 byte) || blockHash (32 byte)|| timestamp (8 byte)
            byte[] processingInfoHash = Arrays.copyOfRange(patientShortInfoAllBytes, (i - 1) * expectedLength, (i - 1) * expectedLength + Configuration.HASH_LENGTH);
            byte[] processingBlockHash = Arrays.copyOfRange(patientShortInfoAllBytes, (i - 1) * expectedLength + Configuration.HASH_LENGTH, (i - 1) * expectedLength + 2 * Configuration.HASH_LENGTH);
            long timestamp = GeneralHelper.bytesToLong(Arrays.copyOfRange(patientShortInfoAllBytes, (i - 1) * expectedLength + 2 * Configuration.HASH_LENGTH, i * expectedLength));
            if (BlockChainManager.isThisBlockOnTheChain(blockHash, processingBlockHash)) {
                patientShortInfos.add(new PatientShortInfo(new Location(processingBlockHash, processingInfoHash), timestamp));
            }
        }


        return patientShortInfos;
    }
}
