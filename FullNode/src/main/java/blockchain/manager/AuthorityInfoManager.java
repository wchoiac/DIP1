package blockchain.manager;

import blockchain.block.AuthorityInfo;
import blockchain.block.Block;
import blockchain.block.BlockHeader;
import blockchain.internal.AuthorityInfoForInternal;
import blockchain.internal.StateInfo;
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

public class AuthorityInfoManager {

    // blockHash(32 bytes)
    public static void trust(byte[] blockHash, byte[] prevBlockHash, AuthorityInfo authorityInfo) throws IOException, BlockChainObjectParsingException {

        storeOverall(blockHash, prevBlockHash, authorityInfo, true);

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

    //check whether it could be revoked or not
    // blockHash(32 bytes)
    public static void untrust(byte[] blockHash, byte[] prevBlockHash, AuthorityInfo authorityInfo) throws IOException, BlockChainObjectParsingException {

        storeOverall(blockHash, prevBlockHash, authorityInfo, false);

        String authorityIdentifierString = GeneralHelper.bytesToStringHex(authorityInfo.getIdentifier());

        File authorityFolder = new File(Configuration.AUTHORITY_FOLDER, authorityIdentifierString.charAt(0) + "/" + authorityIdentifierString.charAt(1) + "/" + authorityIdentifierString + "/");
        File untrustFile = new File(authorityFolder, "untrust");
        if (!authorityFolder.exists()) {
            authorityFolder.mkdirs();
        }

        try (FileOutputStream os = new FileOutputStream(untrustFile, true)) {
            os.write(blockHash);
        }


    }

    private static void storeOverall(byte[] blockHash, byte[] prevBlockHash, AuthorityInfo authorityInfo, boolean add) throws IOException, BlockChainObjectParsingException {

        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);

        StateInfo stateInfo = StateInfoManager.load(prevBlockHash);
        String latestAuthorityListBlockHash = GeneralHelper.bytesToStringHex(stateInfo.getLatestAuthorityListBlockHash());

        File prevAuthoritiesFile = new File(Configuration.BLOCK_FOLDER, latestAuthorityListBlockHash.charAt(0) + "/" + latestAuthorityListBlockHash.charAt(1) + "/"
                + latestAuthorityListBlockHash.charAt(2) + "/" + latestAuthorityListBlockHash.charAt(3) + "/" + latestAuthorityListBlockHash.charAt(4) + "/" + latestAuthorityListBlockHash + "/authorities");

        File processingBlockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString);
        File processingAuthoritiesFile = new File(processingBlockFolder, "authorities");

        if (!processingBlockFolder.exists()) {
            processingBlockFolder.mkdirs();
        }

        byte[] authoritiesAllBytes = Files.readAllBytes(prevAuthoritiesFile.toPath());
        if (authoritiesAllBytes.length % Configuration.IDENTIFIER_LENGTH != 0)
            throw new BlockChainObjectParsingException();

        byte[] processingAuthorityIdentifier = authorityInfo.getIdentifier();

        if (add) {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(processingAuthoritiesFile));
            bos.write(authoritiesAllBytes);
            bos.write(processingAuthorityIdentifier);
            bos.close();
        } else {
            int offset = 0;
            while (offset < authoritiesAllBytes.length) {
                if (Arrays.equals(processingAuthorityIdentifier, Arrays.copyOfRange(authoritiesAllBytes, offset, offset + Configuration.IDENTIFIER_LENGTH)))
                    break;
                offset += Configuration.IDENTIFIER_LENGTH;
            }

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(processingAuthoritiesFile));
            bos.write(Arrays.copyOfRange(authoritiesAllBytes, 0, offset));
            if (offset + Configuration.IDENTIFIER_LENGTH != authoritiesAllBytes.length)
                bos.write(Arrays.copyOfRange(authoritiesAllBytes, offset + Configuration.IDENTIFIER_LENGTH, authoritiesAllBytes.length));
            bos.close();

        }

    }

    public static ArrayList<byte[]> loadOverall(byte[] blockHash) throws IOException {
        StateInfo stateInfo = StateInfoManager.load(blockHash);
        String latestAthorityListBlockHash = GeneralHelper.bytesToStringHex(stateInfo.getLatestAuthorityListBlockHash());

        File authoritiesFile = new File(Configuration.BLOCK_FOLDER, latestAthorityListBlockHash.charAt(0) + "/" + latestAthorityListBlockHash.charAt(1) + "/"
                + latestAthorityListBlockHash.charAt(2) + "/" + latestAthorityListBlockHash.charAt(3) + "/" + latestAthorityListBlockHash.charAt(4) + "/" + latestAthorityListBlockHash + "/authorities");

        ArrayList<byte[]> overallList = new ArrayList<>();
        byte[] authoritiesAllBytes = Files.readAllBytes(authoritiesFile.toPath());

        int offset = 0;
        while (offset < authoritiesAllBytes.length) {
            overallList.add(Arrays.copyOfRange(authoritiesAllBytes, offset, offset + Configuration.IDENTIFIER_LENGTH));
            offset += Configuration.IDENTIFIER_LENGTH;
        }

        return overallList;

    }

    // blockHash is current chain's head block
    public static AuthorityInfoForInternal load(byte[] blockHash, byte[] authorityIdentifier) throws IOException, BlockChainObjectParsingException {
        String authorityIdentifierString = GeneralHelper.bytesToStringHex(authorityIdentifier);

        File processingAuthorityFolder = new File(Configuration.AUTHORITY_FOLDER, authorityIdentifierString.charAt(0) + "/" + authorityIdentifierString.charAt(1) + "/" + authorityIdentifierString + "/");
        File trustFile = new File(processingAuthorityFolder, "trust");
        File untrustFile = new File(processingAuthorityFolder, "untrust");

        if (!processingAuthorityFolder.exists())
            return null;

        if (untrustFile.exists()) {
            // if revocation exist within the same chain return emptyInfo
            byte[] untrustAllBytes = Files.readAllBytes(untrustFile.toPath());

            if (untrustAllBytes.length % Configuration.HASH_LENGTH != 0)
                throw new BlockChainObjectParsingException();

            for (int i = untrustAllBytes.length / Configuration.HASH_LENGTH; i > 0; --i) {
                byte[] untrustedBlockHash = Arrays.copyOfRange(untrustAllBytes, (i - 1) * Configuration.HASH_LENGTH, i * Configuration.HASH_LENGTH);
                if (BlockChainManager.isThisBlockOnTheChain(blockHash, untrustedBlockHash))
                    return new AuthorityInfoForInternal(null, -1, untrustedBlockHash);
            }
        }

        //read authorization file - if authorization exist within the chain chain return the authority
        byte[] trustAllBytes = Files.readAllBytes(trustFile.toPath());
        if (trustAllBytes.length % Configuration.HASH_LENGTH != 0)
            throw new BlockChainObjectParsingException();
        for (int i = trustAllBytes.length / Configuration.HASH_LENGTH; i > 0; --i) {
            byte[] trustedBlockHash = Arrays.copyOfRange(trustAllBytes, (i - 1) * Configuration.HASH_LENGTH, i * Configuration.HASH_LENGTH);
            if (BlockChainManager.isThisBlockOnTheChain(blockHash, trustedBlockHash)) {

                BlockHeader trustedHeader = BlockManager.loadBlockHeader(trustedBlockHash);
                AuthorityInfo authorityInfo = null;
                if (trustedHeader.getBlockNumber() == 0) {
                    Block genesisBlock = BlockManager.loadBlock(trustedBlockHash);
                    for (AuthorityInfo tempAuthorityInfo : genesisBlock.getContent().getInitialAuthorities()) {
                        if (Arrays.equals(authorityIdentifier, tempAuthorityInfo.getIdentifier())) {
                            authorityInfo = tempAuthorityInfo;
                            break;
                        }
                    }
                } else {
                    authorityInfo = trustedHeader.getVote().getBeneficiary();
                }

                StateInfo stateInfo = StateInfoManager.load(trustedBlockHash);

                int totalAuthorities = stateInfo.getTotalAuthorities();
                int lastSignedBlockNumber = -1;
                int validationInterval = totalAuthorities / 2 + 1;
                BlockHeader processingHeader = BlockManager.loadBlockHeader(blockHash);

                for (int j = 0; j < validationInterval; ++j) {
                    if (Arrays.equals(processingHeader.getValidatorIdentifier(), authorityInfo.getIdentifier())) {
                        lastSignedBlockNumber = processingHeader.getBlockNumber();
                        break;
                    }
                    else if (processingHeader.getBlockNumber() != 0)
                        processingHeader = BlockManager.loadBlockHeader(processingHeader.getPrevHash());
                    else
                        break;
                }

                return new AuthorityInfoForInternal(authorityInfo, lastSignedBlockNumber, null);
            }
        }

        return null;


    }


}

