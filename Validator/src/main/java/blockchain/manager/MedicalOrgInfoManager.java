package blockchain.manager;

import blockchain.block.Block;
import exception.BlockChainObjectParsingException;
import blockchain.internal.AuthorityInfoForInternal;
import blockchain.internal.MedicalOrgInfoForInternal;
import blockchain.block.MedicalOrgInfo;
import blockchain.manager.datastructure.MedicalOrgShortInfo;
import config.Configuration;
import exception.FileCorruptionException;
import general.utility.GeneralHelper;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class MedicalOrgInfoManager {


    // blockHash(32 bytes) || authorityIdentifier(20 bytes) || medicalOrg name length(1 byte) || medicalOrg name (length byte) -authorization file

    // 1 (1 byte -representing authorization) || blockHash(32 bytes)||  authority identifier (20 bytes)||medicalOrg identifier (20 bytes)
    // || medicalOrgName length(1 byte) || medicalOrgName(length byte) - record list
    public static void authorize(byte[] blockHash, MedicalOrgInfo medicalOrgInfo, byte[] authorityIdentifier) throws IOException {

        String medicalOrgIdentifierString = GeneralHelper.bytesToStringHex(medicalOrgInfo.getIdentifier());

        File medicalOrgFolder = new File(Configuration.MEDICAL_ORGANIZATION_FOLDER, medicalOrgIdentifierString.charAt(0) + "/" + medicalOrgIdentifierString.charAt(1) + "/"
                + medicalOrgIdentifierString.charAt(2) + "/" + medicalOrgIdentifierString.charAt(3) + "/" + medicalOrgIdentifierString.charAt(4) + "/" + medicalOrgIdentifierString + "/");
        File authorizationFile = new File(medicalOrgFolder, "authorization");

        if (!medicalOrgFolder.exists()) {
            medicalOrgFolder.mkdirs();
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(authorizationFile, true));
             BufferedOutputStream bos2 = new BufferedOutputStream(new FileOutputStream(Configuration.AUTHORIZATION_AND_REVOCATION_RECORD_FILE, true))) {

            //write to authorize
            bos.write(blockHash);
            bos.write(authorityIdentifier);
            bos.write(medicalOrgInfo.getName().length());
            bos.write(medicalOrgInfo.getName().getBytes());
            bos.close();

            bos2.write(1);
            bos2.write(blockHash);
            bos2.write(authorityIdentifier);
            bos2.write(medicalOrgInfo.getIdentifier());
            bos2.write(medicalOrgInfo.getName().length());
            bos2.write(medicalOrgInfo.getName().getBytes());
        }
    }

    // blockHash(32 bytes) -revocation file
    // 0 (0 byte -representing revocation) || blockHash(32 bytes)||medicalOrg identifier (20 bytes)- record list
    //check whether it could be revoked or not
    public static void revoke(byte[] blockHash, byte[] medicalOrgIdentifier) throws IOException {
        String medicalOrgIdentifierString = GeneralHelper.bytesToStringHex(medicalOrgIdentifier);

        File revocationFile = new File(Configuration.MEDICAL_ORGANIZATION_FOLDER, medicalOrgIdentifierString.charAt(0) + "/" + medicalOrgIdentifierString.charAt(1) + "/"
                + medicalOrgIdentifierString.charAt(2) + "/" + medicalOrgIdentifierString.charAt(3) + "/" + medicalOrgIdentifierString.charAt(4) + "/" + medicalOrgIdentifierString + "/revocation");

        try (FileOutputStream os = new FileOutputStream(revocationFile, true);
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(Configuration.AUTHORIZATION_AND_REVOCATION_RECORD_FILE, true))) {
            //write to revocation file
            os.write(blockHash);

            //write to record file
            bos.write(0);
            bos.write(blockHash);
            bos.write(medicalOrgIdentifier);
        }

    }

    public static ArrayList<MedicalOrgShortInfo> loadEveryShortInfo(byte[] blockHash, byte[] authorityIdentifier) throws BlockChainObjectParsingException, IOException {


        ArrayList<MedicalOrgShortInfo> medicalOrgShortInfos = new ArrayList<>();

        if (!Configuration.AUTHORIZATION_AND_REVOCATION_RECORD_FILE.exists())
            return medicalOrgShortInfos;

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(Configuration.AUTHORIZATION_AND_REVOCATION_RECORD_FILE))) {


            // 1 (1 byte -representing authorization) || blockHash(32 bytes)||  authority identifier (20 bytes)||medicalOrg identifier (20 bytes)
            // || medicalOrgName length(1 byte) || medicalOrgName(length byte) - record list
            byte[] authorizationReadArray = new byte[Configuration.HASH_LENGTH + 2 * Configuration.IDENTIFIER_LENGTH + 1];

            // 0 (0 byte -representing revocation) || blockHash(32 bytes)||medicalOrg identifier (20 bytes)- record list
            byte[] revocationReadArray = new byte[Configuration.HASH_LENGTH + Configuration.IDENTIFIER_LENGTH];

            byte authorize;
            while ((authorize = (byte) bis.read()) != -1) {
                if (authorize == 1) {
                    if (bis.read(authorizationReadArray) != authorizationReadArray.length)
                        throw new BlockChainObjectParsingException();
                    byte[] processingBlockHash = Arrays.copyOfRange(authorizationReadArray, 0, Configuration.HASH_LENGTH);
                    byte[] processingAuthorityIdentifier = Arrays.copyOfRange(authorizationReadArray, Configuration.HASH_LENGTH, Configuration.HASH_LENGTH + Configuration.IDENTIFIER_LENGTH);
                    byte[] processingMedicalOrgIdentifier = Arrays.copyOfRange(authorizationReadArray, Configuration.HASH_LENGTH + Configuration.IDENTIFIER_LENGTH, authorizationReadArray.length - 1);
                    byte[] processingMedicalOrgNameBytes = new byte[authorizationReadArray[authorizationReadArray.length - 1]];
                    if (bis.read(processingMedicalOrgNameBytes) != processingMedicalOrgNameBytes.length)
                        throw new BlockChainObjectParsingException();
                    String processingMedicalOrgName = new String(processingMedicalOrgNameBytes);

                    if (Arrays.equals(processingAuthorityIdentifier, authorityIdentifier) && BlockChainManager.isThisBlockOnTheChain(blockHash, processingBlockHash)) {
                        medicalOrgShortInfos.add(new MedicalOrgShortInfo(processingMedicalOrgName, processingMedicalOrgIdentifier));
                    }
                } else {
                    if (bis.read(revocationReadArray) != revocationReadArray.length)
                        throw new BlockChainObjectParsingException();
                    byte[] processingBlockHash = Arrays.copyOfRange(revocationReadArray, 0, Configuration.HASH_LENGTH);
                    byte[] processingMedicalOrgIdentifier = Arrays.copyOfRange(revocationReadArray, Configuration.HASH_LENGTH, revocationReadArray.length);

                    MedicalOrgShortInfo processingMedicalOrgShortInfo = new MedicalOrgShortInfo("", processingMedicalOrgIdentifier);

                    if (medicalOrgShortInfos.contains(processingMedicalOrgShortInfo) && BlockChainManager.isThisBlockOnTheChain(blockHash, processingBlockHash)) {
                        medicalOrgShortInfos.remove(processingMedicalOrgShortInfo);
                    }
                }

            }
        }

        return medicalOrgShortInfos;
    }

    public static boolean isRevoked(byte[] blockHash, byte[] medicalOrgIdentifier) throws BlockChainObjectParsingException, IOException {

        String medicalOrgIdentifierString = GeneralHelper.bytesToStringHex(medicalOrgIdentifier);
        File medicalOrgFolder = new File(Configuration.MEDICAL_ORGANIZATION_FOLDER, medicalOrgIdentifierString.charAt(0) + "/" + medicalOrgIdentifierString.charAt(1) + "/"
                + medicalOrgIdentifierString.charAt(2) + "/" + medicalOrgIdentifierString.charAt(3) + "/" + medicalOrgIdentifierString.charAt(4) + "/" + medicalOrgIdentifierString + "/");
        File revocationFile = new File(medicalOrgFolder, "revocation");
        if (!revocationFile.exists()) {
            return false;
        }

        // if revocation exist within the same chain return null
        byte[] revocationAllBytes = Files.readAllBytes(revocationFile.toPath());
        if (revocationAllBytes.length % Configuration.HASH_LENGTH != 0)
            throw new BlockChainObjectParsingException();

        //blockHash(32 bytes)
        for (int i = revocationAllBytes.length / Configuration.HASH_LENGTH; i > 0; --i) {
            byte[] revokedBlockHash = Arrays.copyOfRange(revocationAllBytes, (i - 1) * Configuration.HASH_LENGTH, i * Configuration.HASH_LENGTH);
            if (BlockChainManager.isThisBlockOnTheChain(blockHash, revokedBlockHash))
                return true;
        }


        return false;
    }

    // revoked medicalOrg also returns null
    // if revoked because the authority got untrusted - has to check again(lazy revocation)
    // return {block hash of authorization, authorityidentifier}
    public static MedicalOrgInfoForInternal load(byte[] blockHash, byte[] medicalOrgIdentifier) throws BlockChainObjectParsingException, IOException, FileCorruptionException {
        String medicalOrgIdentifierString = GeneralHelper.bytesToStringHex(medicalOrgIdentifier);

        File medicalOrgFolder = new File(Configuration.MEDICAL_ORGANIZATION_FOLDER, medicalOrgIdentifierString.charAt(0) + "/" + medicalOrgIdentifierString.charAt(1) + "/"
                + medicalOrgIdentifierString.charAt(2) + "/" + medicalOrgIdentifierString.charAt(3) + "/" + medicalOrgIdentifierString.charAt(4) + "/" + medicalOrgIdentifierString + "/");
        File authorizationFile = new File(medicalOrgFolder, "authorization");

        if (!medicalOrgFolder.exists() || !authorizationFile.exists())
            return null;
        if (isRevoked(blockHash, medicalOrgIdentifier))
            return null;


        //read authorization file - if authorization exist within the chain return the info
        byte[] authorizationAllBytes = Files.readAllBytes(authorizationFile.toPath());

        int offset = 0;
        while (offset < authorizationAllBytes.length) {
            byte[] authorizedBlockHash = Arrays.copyOfRange(authorizationAllBytes, offset, offset + Configuration.HASH_LENGTH);
            if (BlockChainManager.isThisBlockOnTheChain(blockHash, authorizedBlockHash)) {
                byte[] authorityIdentifier = Arrays.copyOfRange(authorizationAllBytes, offset + Configuration.HASH_LENGTH
                        , offset + Configuration.HASH_LENGTH + Configuration.IDENTIFIER_LENGTH);

                //lazy revocation
                AuthorityInfoForInternal infoForInternal = AuthorityInfoManager.load(blockHash, authorityIdentifier);
                if (infoForInternal == null)
                    throw new FileCorruptionException();
                else if (infoForInternal.getUntrustedBlock() != null) {
                    MedicalOrgInfoManager.revoke(infoForInternal.getUntrustedBlock(), medicalOrgIdentifier);
                    return null;
                }
                Block curBlock = BlockManager.loadBlock(authorizedBlockHash);
                for (MedicalOrgInfo info : curBlock.getContent().getMedicalOrgAuthorizationList()) {
                    if (Arrays.equals(info.getIdentifier(), medicalOrgIdentifier))
                        return new MedicalOrgInfoForInternal(info, authorityIdentifier);
                }
            } else {
                // blockHash(32 bytes) || authorityIdentifier(20 bytes) || medicalOrg name length(1 byte) || medicalOrg name (length byte)
                offset += Configuration.HASH_LENGTH + Configuration.IDENTIFIER_LENGTH;
                offset += 1 + authorizationAllBytes[offset];
            }
        }

        return null;

    }

    // revoked medicalOrg also returns null
    // if revoked because the authority got untrusted - has to check again(lazy revocation)
    // return {block hash of authorization, authorityidentifier}
    public static String loadName(byte[] blockHash, byte[] medicalOrgIdentifier) throws BlockChainObjectParsingException, IOException {

        String medicalOrgIdentifierString = GeneralHelper.bytesToStringHex(medicalOrgIdentifier);

        File medicalOrgFolder = new File(Configuration.MEDICAL_ORGANIZATION_FOLDER, medicalOrgIdentifierString.charAt(0) + "/" + medicalOrgIdentifierString.charAt(1) + "/"
                + medicalOrgIdentifierString.charAt(2) + "/" + medicalOrgIdentifierString.charAt(3) + "/" + medicalOrgIdentifierString.charAt(4) + "/" + medicalOrgIdentifierString + "/");
        File authorizationFile = new File(medicalOrgFolder, "authorization");

        if (!medicalOrgFolder.exists() || !authorizationFile.exists())
            return null;


        //read authorization file - if authorization exist within the chain chain return the signer
        byte[] authorizationAllBytes = new byte[0];
        try {
            authorizationAllBytes = Files.readAllBytes(authorizationFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }


        int offset = 0;
        while (offset < authorizationAllBytes.length) {
            byte[] authorizedBlockHash = Arrays.copyOfRange(authorizationAllBytes, offset, offset + Configuration.HASH_LENGTH);

            if (BlockChainManager.isThisBlockOnTheChain(blockHash, authorizedBlockHash)) {
                offset += Configuration.HASH_LENGTH + Configuration.IDENTIFIER_LENGTH;
                int length = authorizationAllBytes[offset++];
                return new String(Arrays.copyOfRange(authorizationAllBytes, offset, offset + length));
            }
            else
            {
                offset += Configuration.HASH_LENGTH + Configuration.IDENTIFIER_LENGTH;
                int length = authorizationAllBytes[offset++];
                offset+=length;
            }

        }


        return null;

    }


}
