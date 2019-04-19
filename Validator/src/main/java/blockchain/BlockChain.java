package blockchain;

import blockchain.block.*;
import blockchain.block.transaction.Transaction;
import blockchain.internal.AuthorityInfoForInternal;
import blockchain.internal.ChainInfo;
import blockchain.internal.MedicalOrgInfoForInternal;
import blockchain.internal.Voting;
import blockchain.manager.*;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import node.validator.Validator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

// better blockchain
// 1. greatest total score
// 2. faster arrival
// revocation, authorization, etc are effective after the block containing such data

public class BlockChain {

    private ArrayList<Voting> currentVotingList = new ArrayList<>();
    private int totalScore = 0;
    private ArrayList<byte[]> currentOverallAuthorityIdentifierList = new ArrayList<>();
    private ArrayList<Block> cachedCurrentChain = new ArrayList<>(); // always at least one block inside
    private TreeMap<byte[], MedicalOrgInfoForInternal> cachedCurrentMedicalOrgList = new TreeMap<>(new GeneralHelper.byteArrayComparator());
    private TreeMap<byte[], AuthorityInfoForInternal> cachedCurrentAuthorityList = new TreeMap<>(new GeneralHelper.byteArrayComparator());


    public Block getLatestBlock() {

        if (cachedCurrentChain.isEmpty())
            return null;

        return cachedCurrentChain.get(cachedCurrentChain.size() - 1);
    }

    public byte[] getLatestBlockHash() {
        if (cachedCurrentChain.isEmpty())
            return null;

        return cachedCurrentChain.get(cachedCurrentChain.size() - 1).calculateHash();
    }

    public int getCurrentLatestBlockNumber() {
        if (cachedCurrentChain.isEmpty())
            return -1;
        return cachedCurrentChain.get(cachedCurrentChain.size() - 1).getHeader().getBlockNumber();
    }

    public long getLatestBlockTimeStamp() {
        if (cachedCurrentChain.isEmpty())
            return -1;

        return cachedCurrentChain.get(cachedCurrentChain.size() - 1).getHeader().getTimestamp();
    }

    // because it keeps validating new blocks even if not synced
    public long getTimeStampForValidatorSyncCheck(byte[] myIdentifier) throws IOException, BlockChainObjectParsingException {
        if (cachedCurrentChain.isEmpty())
            return -1;


        if (Arrays.equals(cachedCurrentChain.get(cachedCurrentChain.size() - 1).getHeader().getValidatorIdentifier(), myIdentifier)) // obviously not genesis block
        {
            if (cachedCurrentChain.size() == 1) {
                cachedCurrentChain.add(0, BlockManager.loadBlock(cachedCurrentChain.get(0).getHeader().getPrevHash()));
            }

            return cachedCurrentChain.get(cachedCurrentChain.size() - 2).getHeader().getTimestamp();
        } else {
            return cachedCurrentChain.get(cachedCurrentChain.size() - 1).getHeader().getTimestamp();
        }

    }


    public int getTotalScore() {
        return totalScore;
    }

    public byte[] getCurrentChainHashLocator(byte[] highestBlockHash, int length) throws BlockChainObjectParsingException, IOException {

        BlockHeader highestBlockHeader;
        if(highestBlockHash==null) {
            highestBlockHeader = cachedCurrentChain.get(cachedCurrentChain.size() - 1).getHeader();
        }
        else
        {
            highestBlockHeader = BlockManager.loadBlockHeader(highestBlockHash);
            if(highestBlockHeader==null)
                return null;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BlockHeader processingHeader = highestBlockHeader;

        for (int i=0;i< length;++i) {
            byteArrayOutputStream.write(processingHeader.calculateHash());
            if(processingHeader.getBlockNumber()!=0)
                processingHeader = BlockManager.loadBlockHeader(processingHeader.getPrevHash());
            else
                break;
        }

        return byteArrayOutputStream.toByteArray();
    }

    public Status getCurrentStatus() {

        return new Status(totalScore, getLatestBlockHash());
    }

    public ArrayList<Voting> getCurrentVotingList() {
        return currentVotingList;
    }

    public int getValidationInterval() {
        return currentOverallAuthorityIdentifierList.size() / 2 + 1;
    }

    public int getTotalAuthorities() {
        return currentOverallAuthorityIdentifierList.size();
    }

    public ArrayList<AuthorityInfo> getCurrentOverallAuthorityInfoList() throws IOException, BlockChainObjectParsingException {
        ArrayList<AuthorityInfo> currentOverallAuthorityList = new ArrayList<>();

        for (byte[] identifier : currentOverallAuthorityIdentifierList) {
            currentOverallAuthorityList.add(getAuthority(identifier));
        }

        return currentOverallAuthorityList;
    }

    public AuthorityInfo getAuthority(byte[] identifier) throws IOException, BlockChainObjectParsingException {
        if (cachedCurrentAuthorityList.containsKey(identifier))
            return cachedCurrentAuthorityList.get(identifier).getAuthorityInfo();
        else {
            AuthorityInfoForInternal internalInfo = AuthorityInfoManager.load(getLatestBlockHash(), identifier);
            if (internalInfo == null || internalInfo.getUntrustedBlock() != null)
                return null;
            else {
                cachedCurrentAuthorityList.put(internalInfo.getAuthorityInfo().getIdentifier(), internalInfo);
                return cachedCurrentAuthorityList.get(identifier).getAuthorityInfo();
            }
        }

    }

    public AuthorityInfoForInternal getAuthorityInfoForInternal(byte[] identifier) throws IOException, BlockChainObjectParsingException {
        if (cachedCurrentAuthorityList.containsKey(identifier))
            return cachedCurrentAuthorityList.get(identifier);
        else {
            AuthorityInfoForInternal internalInfo = AuthorityInfoManager.load(getLatestBlockHash(), identifier);
            if (internalInfo == null || internalInfo.getUntrustedBlock() != null)
                return null;
            else {
                cachedCurrentAuthorityList.put(internalInfo.getAuthorityInfo().getIdentifier(), internalInfo);
                return cachedCurrentAuthorityList.get(identifier);
            }
        }

    }


    public boolean hasBlock(Block block) {
        return cachedCurrentChain.contains(block);
    }

    public boolean hasAuthority(byte[] identifier) throws IOException, BlockChainObjectParsingException {
        if (cachedCurrentAuthorityList.containsKey(identifier))
            return true;
        else {
            AuthorityInfoForInternal internalInfo = AuthorityInfoManager.load(getLatestBlockHash(), identifier);
            if (internalInfo == null || internalInfo.getUntrustedBlock() != null)
                return false;
            else {
                cachedCurrentAuthorityList.put(internalInfo.getAuthorityInfo().getIdentifier(), internalInfo);
                return true;
            }
        }

    }

    public boolean isAuthorityUntrusted(byte[] identifier) throws IOException, BlockChainObjectParsingException {
        if (hasAuthority(identifier))
            return false;
        else {
            AuthorityInfoForInternal internalInfo = AuthorityInfoManager.load(getLatestBlockHash(), identifier);
            return internalInfo != null;
        }

    }

    public int authorityIndex(byte[] authorityIdentifier) {
        return GeneralHelper.getIndexFromArrayList(authorityIdentifier, currentOverallAuthorityIdentifierList);
    }


    public MedicalOrgInfoForInternal getMedicalOrgInfoForInternal(byte[] identifier) throws IOException, BlockChainObjectParsingException, FileCorruptionException {
        if (cachedCurrentMedicalOrgList.containsKey(identifier))
            return cachedCurrentMedicalOrgList.get(identifier);
        else {
            MedicalOrgInfoForInternal internalInfo = MedicalOrgInfoManager.load(getLatestBlockHash(), identifier);
            if (internalInfo == null)
                return null;
            else {
                cachedCurrentMedicalOrgList.put(internalInfo.getMedicalOrgInfo().getIdentifier(), internalInfo);
                return cachedCurrentMedicalOrgList.get(identifier);
            }
        }

    }

    public boolean isMedicalOrgRevoked(byte[] identifier) throws IOException, BlockChainObjectParsingException {
        if (cachedCurrentMedicalOrgList.containsKey(identifier))
            return false;
        else {
            return MedicalOrgInfoManager.isRevoked(getLatestBlockHash(), identifier);
        }
    }

    public boolean hasMedicalOrg(byte[] identifier) throws IOException, BlockChainObjectParsingException, FileCorruptionException {
        if (cachedCurrentMedicalOrgList.containsKey(identifier))
            return true;
        else {
            MedicalOrgInfoForInternal internalInfo = MedicalOrgInfoManager.load(getLatestBlockHash(), identifier);
            if (internalInfo == null)
                return false;
            else {
                cachedCurrentMedicalOrgList.put(internalInfo.getAuthorityIdentifier(), internalInfo);
                return true;
            }
        }
    }


    public boolean canSignNext(byte[] identifier) throws IOException, BlockChainObjectParsingException {

        if (!hasAuthority(identifier))
            return false;


        boolean isNextInOrder = isNextBlockInOrder(identifier);


        if (isNextInOrder) {
            if (System.currentTimeMillis() - getLatestBlockTimeStamp() < Configuration.BLOCK_PERIOD)
                return false;
        } else {
            if (System.currentTimeMillis() - getLatestBlockTimeStamp() < Configuration.MIN_OUT_ORDER_BLOCK_PERIOD)
                return false;
        }


        return (getAuthorityInfoForInternal(identifier).getLastSignedBlockNumber() == -1 || (getCurrentLatestBlockNumber() + 1) - getAuthorityInfoForInternal(identifier).getLastSignedBlockNumber() >= getValidationInterval());


    }

    // checking if next block is inoder for pk
    public boolean isNextBlockInOrder(byte[] identifier) {
        int nextHeight = getCurrentLatestBlockNumber() + 1;
        return nextHeight % getTotalAuthorities() == authorityIndex(identifier);
    }


    public int generateRandomOffset() {

        Random rand = new Random();
        return (Configuration.MIN_OUT_ORDER_BLOCK_PERIOD - Configuration.BLOCK_PERIOD) + rand.nextInt(500 * getValidationInterval());
    }

    public boolean isNextBlockHeader(BlockHeader blockheader) {
        if (blockheader.getBlockNumber() != getCurrentLatestBlockNumber() + 1) {
            return false;
        }

        return Arrays.equals(blockheader.calculateHash(), getLatestBlockHash());
    }

    public boolean isNextBlock(Block block) {

        int latestBlockNumber = getCurrentLatestBlockNumber();
        if (latestBlockNumber == -1)
            return true;

        if (block.getHeader().getBlockNumber() != getCurrentLatestBlockNumber() + 1) {
            return false;
        }

        return Arrays.equals(block.getHeader().getPrevHash(), getLatestBlockHash());
    }


    public int checkHeaders(BlockHeader[] headers) throws IOException, BlockChainObjectParsingException {

        if (headers == null || headers.length == 0)
            return -1;
        if (!isNextBlockHeader(headers[0])) {
            return -1;
        }

        TreeMap<byte[], AuthorityInfoForInternal> tempAuthorityList = (TreeMap<byte[], AuthorityInfoForInternal>) cachedCurrentAuthorityList.clone();
        ArrayList<byte[]> tempOverallAuthorityList = (ArrayList<byte[]>) currentOverallAuthorityIdentifierList.clone();
        ArrayList<Voting> tempVotingList = (ArrayList<Voting>) currentVotingList.clone();

        long prevBlockTimeStamp = getLatestBlockTimeStamp();
        int tempTotalScore = getTotalScore();

        for (BlockHeader header : headers) {
            int tempValidationInterval = tempAuthorityList.size() / 2 + 1;
            int index = GeneralHelper.getIndexFromArrayList(header.getValidatorIdentifier(), tempOverallAuthorityList);
            if (index == -1)
                return -1;
            else if (!tempAuthorityList.containsKey(header.getValidatorIdentifier()))
                if (hasAuthority(header.getValidatorIdentifier())) {
                    AuthorityInfoForInternal authorityInfoForInternal = getAuthorityInfoForInternal(header.getValidatorIdentifier());
                    tempAuthorityList.put(header.getValidatorIdentifier(), new AuthorityInfoForInternal(authorityInfoForInternal.getAuthorityInfo(),
                            authorityInfoForInternal.getLastSignedBlockNumber(), null));

                }

            if (header.getBlockNumber() % Configuration.CHECK_POINT_BLOCK_INTERVAL == 0)
                tempVotingList.clear();

            AuthorityInfoForInternal validatorInfoForInternal = tempAuthorityList.get(header.getValidatorIdentifier());


            try {
                if (!SecurityHelper.verifyRawECDSASignatureWithContent(validatorInfoForInternal.getAuthorityInfo().getPublicKey(), header.getSignatureCoverage(), header.getValidatorSignature(), Configuration.BLOCKCHAIN_HASH_ALGORITHM, Configuration.ELIPTIC_CURVE))
                    return -1;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            //=> skip contentHash check

            if (!validatorInfoForInternal.canSign(header.getBlockNumber(), tempAuthorityList.size()))
                return -1;

            boolean isInOrder = AuthorityInfoForInternal.isInOrder(header.getBlockNumber(), tempAuthorityList.size(), index);
            int expectedScore = isInOrder ? Configuration.IN_ORDER : Configuration.OUT_ORDER;
            if (expectedScore != header.getScore())
                return -1;

            if (header.getTimestamp()> System.currentTimeMillis()+Configuration.TIME_DIFFERENCE_ALLOWANCE||
                    header.getTimestamp() - prevBlockTimeStamp < (isInOrder ? Configuration.BLOCK_PERIOD : Configuration.MIN_OUT_ORDER_BLOCK_PERIOD))
                return -1;


            //check vote and process vote - could be changed later
            if (header.getVote() != null) {

                //check vote

                Voting changedAuthorityVoting = null;
                Voting votingToBeRemoved = null;

                if(header.getBlockNumber()%Configuration.CHECK_POINT_BLOCK_INTERVAL==0) // cannot have vote in a check point block
                    return -1;

                if (header.getVote().isAdd()) // return false if authorizing a validator but already in the validator list
                {
                    if (GeneralHelper.getIndexFromArrayList(header.getVote().getBeneficiary().getIdentifier(), tempOverallAuthorityList) != -1)
                        return -1;
                } else // return false if rovoke a validator but not in the validator list
                {
                    if (GeneralHelper.getIndexFromArrayList(header.getVote().getBeneficiary().getIdentifier(), tempOverallAuthorityList) == -1)
                        return -1;
                }
                // this ensures that only one voting about one beneficiary to exist

                boolean isForNewVoting = true;
                for (Voting v : tempVotingList) // duplicate voting check
                    if (v.getBeneficiary().equals(header.getVote().getBeneficiary())) {
                        if (v.isExistingVoter(header.getValidatorIdentifier()))
                            return -1;

                        //process vote
                        if (header.getVote().isAgree())
                            v.addAgree(header.getValidatorIdentifier());
                        else
                            v.addDisagree(header.getValidatorIdentifier());
                        changedAuthorityVoting = v;
                        isForNewVoting = false;
                        break;
                    }

                if (isForNewVoting) // the voting starter cannot disagree on what he started(not make sense)
                {
                    if (!header.getVote().isAgree())
                        return -1;
                    else {
                        //process vote
                        Voting newVoting = new Voting(header.getVote().getBeneficiary(), header.getVote().isAdd());
                        newVoting.addAgree(header.getValidatorIdentifier());
                        tempVotingList.add(newVoting);
                        changedAuthorityVoting = newVoting;
                    }

                }

                //process vote
                if (changedAuthorityVoting.getNumAgree() >= tempValidationInterval) {
                    if (changedAuthorityVoting.isAdd()) {
                        tempOverallAuthorityList.add(changedAuthorityVoting.getBeneficiary().getIdentifier());
                        tempAuthorityList.put(changedAuthorityVoting.getBeneficiary().getIdentifier(), new AuthorityInfoForInternal(changedAuthorityVoting.getBeneficiary(),-1,null));
                    } else {
                        tempOverallAuthorityList.remove(GeneralHelper.getIndexFromArrayList(changedAuthorityVoting.getBeneficiary().getIdentifier(), tempOverallAuthorityList));
                        tempAuthorityList.remove(changedAuthorityVoting.getBeneficiary().getIdentifier());
                    }
                    votingToBeRemoved = changedAuthorityVoting;
                } else if (changedAuthorityVoting.getNumDisagree() >= tempValidationInterval) {
                    votingToBeRemoved = changedAuthorityVoting;
                }

                if (votingToBeRemoved != null) {
                    tempVotingList.remove(votingToBeRemoved);
                }
            }

            tempTotalScore += expectedScore;
            prevBlockTimeStamp = header.getTimestamp();
        }

        return tempTotalScore;
    }


    public boolean checkNextBlock(Block block, ArrayList<Transaction> transactionPool) throws BlockChainObjectParsingException, IOException, FileCorruptionException {

        if (block == null)
            return false;

        if (!isNextBlock(block)) {
            return false;
        }

        byte[] latestBlockHash = getLatestBlockHash();

        //*****header check start
        if (!hasAuthority(block.getHeader().getValidatorIdentifier()))
            return false;

        AuthorityInfoForInternal validatorInfo = getAuthorityInfoForInternal(block.getHeader().getValidatorIdentifier());
        try {
            if (!SecurityHelper.verifyRawECDSASignatureWithContent(validatorInfo.getAuthorityInfo().getPublicKey(), block.getHeader().getSignatureCoverage(), block.getHeader().getValidatorSignature(), Configuration.BLOCKCHAIN_HASH_ALGORITHM, Configuration.ELIPTIC_CURVE))
                return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (!Arrays.equals(block.getHeader().getContentHash(), block.getContent().calculateHash()))
            return false;

        if (!validatorInfo.canSign(block.getHeader().getBlockNumber(), getTotalAuthorities()))
            return false;

        boolean isInOrder = AuthorityInfoForInternal.isInOrder(block.getHeader().getBlockNumber(), getTotalAuthorities(), authorityIndex(block.getHeader().getValidatorIdentifier()));
        int expectedScore = isInOrder ? Configuration.IN_ORDER : Configuration.OUT_ORDER;
        if (expectedScore != block.getHeader().getScore())
            return false;


        if (block.getHeader().getTimestamp()> System.currentTimeMillis()+Configuration.TIME_DIFFERENCE_ALLOWANCE||
                block.getHeader().getTimestamp() - getLatestBlockTimeStamp() < (isInOrder ? Configuration.BLOCK_PERIOD : Configuration.MIN_OUT_ORDER_BLOCK_PERIOD))
            return false;


        if (block.getHeader().getVote() != null) {

            if (block.getHeader().getBlockNumber() % Configuration.CHECK_POINT_BLOCK_INTERVAL == 0) // check point cannot have vote
                return false;

            if (!checkVote(validatorInfo.getAuthorityInfo().getIdentifier(), block.getHeader().getVote()))
                return false;
        }

        //*****header check end

        //*****content check start

        //want to authorize, but already authorized
        if (block.getContent().getMedicalOrgAuthorizationList() != null) {
            if (block.getContent().getMedicalOrgAuthorizationList().length > Configuration.MAX_AUTHORIZATION)
                return false;

            Set<MedicalOrgInfo> set = new HashSet<>(); //for duplicate check
            for (MedicalOrgInfo medicalOrgInfo : block.getContent().getMedicalOrgAuthorizationList()) {
                if (hasMedicalOrg(medicalOrgInfo.getIdentifier()))
                    return false;

                if (isMedicalOrgRevoked(medicalOrgInfo.getIdentifier()))
                    return false;

                if (set.contains(medicalOrgInfo))
                    return false;
                else {
                    set.add(medicalOrgInfo);
                }
            }
        }

        //want to addToRevocationList but not issued such key, invalid

        if (block.getContent().getMedicalOrgRevocationList() != null) {

            if (block.getContent().getMedicalOrgRevocationList().length > Configuration.MAX_REVOCATION)
                return false;

            SortedSet<byte[]> set = new TreeSet<>(new GeneralHelper.byteArrayComparator()); //for duplicate check
            for (byte[] revokedIdentifier : block.getContent().getMedicalOrgRevocationList()) {
                if (!hasMedicalOrg(revokedIdentifier))
                    return false;
                MedicalOrgInfoForInternal temp = getMedicalOrgInfoForInternal(revokedIdentifier);
                if (!Arrays.equals(temp.getAuthorityIdentifier(), block.getHeader().getValidatorIdentifier()))
                    return false;
                if (set.contains(revokedIdentifier))
                    return false;
                else {
                    set.add(revokedIdentifier);
                }
            }
        }

        if (block.getContent().getPatientInfoList() != null) {

            if (block.getContent().getPatientInfoList().length > Configuration.MAX_PATIENT_INFO)
                return false;

            Set<PatientInfo> set = new HashSet<>(); //for duplicate check

            for (PatientInfo patientInfo : block.getContent().getPatientInfoList()) {

                if(patientInfo.getTimestamp()>block.getHeader().getTimestamp())
                    return false;

                if (!patientInfo.verify())
                    return false;

                if (PatientInfoManager.patientInfoExists(latestBlockHash, patientInfo.getPatientIdentifier(), patientInfo.calculateInfoHash()))
                    return false;

                if (set.contains(patientInfo))
                    return false;
                else {
                    set.add(patientInfo);
                }
            }
        }

        if (block.getContent().getTransactions() != null) {

            if (block.getContent().getTransactions().length > Configuration.MAX_RECORD)
                return false;

            Set<Transaction> set = new HashSet<>(); //for duplicate check

            for (Transaction transaction : block.getContent().getTransactions()) {
                // if already in transaction pool, it is already verified => most case
                if (!transactionPool.contains(transaction)) {
                    if (!hasMedicalOrg(transaction.getMedicalOrgIdentifier()))
                        return false;

                    if(transaction.getTimestamp()>block.getHeader().getTimestamp())
                        return false;

                    if (!TransactionManager.isTransactionUnique(latestBlockHash, transaction.calculateHash(), transaction.getPatientIdentifier()))
                        return false;

                    if (set.contains(transaction))
                        return false;
                    else {
                        set.add(transaction);
                    }

                    PatientInfo patientInfo = PatientInfoManager.load(latestBlockHash, transaction.getPatientIdentifier());
                    MedicalOrgInfoForInternal medicalOrgInfoForInternal = getMedicalOrgInfoForInternal(transaction.getMedicalOrgIdentifier());

                    if (!transaction.verify(patientInfo.getPublicKey(), medicalOrgInfoForInternal.getMedicalOrgInfo().getPublicKey()))
                        return false;
                }
            }
        }

        //*****content check end

        return true;

    }


    // validity check has to be done outside (usig this class's method)
    //return revoked authority
    public AuthorityInfo addBlock(Block block, ArrayList<Transaction> transactionPool, ArrayList<Vote> myVotes, ArrayList<MedicalOrgInfo> authorizationList, SortedSet<byte[]> revocationList, ArrayList<PatientInfo> patientInfoList, byte[] myIdentifier) throws IOException, BlockChainObjectParsingException {

        if (!isNextBlock(block))
            return null;


        AuthorityInfo processedAuthority = null;
        if (block.getHeader().getVote() != null) {
            if (Arrays.equals(block.getHeader().getValidatorIdentifier(), myIdentifier))
                myVotes.remove(block.getHeader().getVote()); // my vote processed, so remove
            processedAuthority = processVote(block.getHeader().getValidatorIdentifier(), block.getHeader().getVote(), myVotes, myIdentifier,block.calculateHash());
        } else if (block.getHeader().getBlockNumber() % Configuration.CHECK_POINT_BLOCK_INTERVAL == 0) {
            ArrayList<Vote> droppedVotes = new ArrayList<>();
            Iterator<Vote> iterator = myVotes.iterator();
            while (iterator.hasNext()) {
                Vote vote = iterator.next();
                if (!vote.isAgree()) { // since every voting is removed => nothing to disagree
                    droppedVotes.add(vote);
                    iterator.remove();
                }
            }
            if(!droppedVotes.isEmpty())
                MyVoteDropManager.checkPointDrop(block.calculateHash(), myIdentifier, droppedVotes);
            currentVotingList.clear();
        }


        if (block.getContent().getMedicalOrgAuthorizationList() != null)
            processAuthorization(block.getHeader().getValidatorIdentifier(), block.getContent().getMedicalOrgAuthorizationList());
        if (block.getContent().getMedicalOrgRevocationList() != null)
            processRevocation(block.getContent().getMedicalOrgRevocationList(), transactionPool);
        if (block.getContent().getTransactions() != null)
            removeFromTransactionPool(block.getContent().getTransactions(), transactionPool);

        if (Arrays.equals(block.getHeader().getValidatorIdentifier(), myIdentifier)) {
            if (block.getContent().getMedicalOrgAuthorizationList() != null)
                removeFromAuthorizationList(block.getContent().getMedicalOrgAuthorizationList(), authorizationList);
            if (block.getContent().getMedicalOrgRevocationList() != null)
                removeFromRevocationList(block.getContent().getMedicalOrgRevocationList(), revocationList);
            if (block.getContent().getPatientInfoList() != null)
                removeFromPatientInfoList(block.getContent().getPatientInfoList(), patientInfoList);
        } else {
            if (block.getContent().getMedicalOrgAuthorizationList() != null) {
                if (removeFromAuthorizationList(block.getContent().getMedicalOrgAuthorizationList(), authorizationList))
                    Validator.getRunningValidator().getBlockChainLogger()
                            .info("In block(" + GeneralHelper.bytesToStringHex(block.calculateHash()) + "), authority("
                                    + getAuthority(block.getHeader().getValidatorIdentifier())
                                    + ") has authorized medical organization(s) who is/are supposed to be authorized by this authority");
            }
        }


        cachedCurrentChain.add(block);
        if (cachedCurrentChain.size() > Configuration.MAX_BLOCK_ON_MEMEORY)
            cachedCurrentChain.remove(0);

        if (block.getHeader().getValidatorIdentifier() != null) {

            AuthorityInfoForInternal temp = getAuthorityInfoForInternal(block.getHeader().getValidatorIdentifier());

            if(temp!=null) // might be disqualified with this block (vote for self-disqualification)
                temp.setLastSignedBlockNumber(block.getHeader().getBlockNumber());
        }

        totalScore += block.getHeader().getScore();

        if (block.getHeader().getVote() == null)
            return null;
        return block.getHeader().getVote().isAdd() ? null : processedAuthority;
    }

    public boolean checkVote(byte[] voterIdentifier, Vote vote) throws IOException, BlockChainObjectParsingException {

        if (vote.getBeneficiary().getName().length() > Configuration.MAX_NAME_LENGTH)
            return false;

        byte[] beneficiaryIdentifier = vote.getBeneficiary().getIdentifier();
        if (vote.isAdd()) // return false if authorizing a validator but already in the validator list
        {
            if (hasAuthority(beneficiaryIdentifier) || isAuthorityUntrusted(beneficiaryIdentifier)) // if untrusted => change public key
                return false;
        } else // return false if deauthorizing a validator but not in the validator list
        {
            if (!hasAuthority(beneficiaryIdentifier))
                return false;
        }
        // this ensures that only one voting about one beneficiary to exist

        boolean isForNewVoting = true;
        for (Voting v : currentVotingList) // duplicate voting check
            if (v.getBeneficiary().equals(vote.getBeneficiary())) {
                if (v.isExistingVoter(voterIdentifier))
                    return false;
                isForNewVoting = false;
            }

        return !isForNewVoting || vote.isAgree();

    }


    public boolean checkTransaction(Transaction transaction) throws BlockChainObjectParsingException, IOException, FileCorruptionException {
        byte[] latestBlockHash = getLatestBlockHash();

        if (transaction == null || transaction.getMedicalOrgIdentifier() == null || transaction.getPatientSignature() == null || transaction.getEncryptedRecord() == null
                || transaction.getPatientIdentifier() == null || transaction.getMedicalOrgSignature() == null)
            return false;

        if (!hasMedicalOrg(transaction.getMedicalOrgIdentifier()))
            return false;

        MedicalOrgInfoForInternal medicalOrgInfoForInternal = getMedicalOrgInfoForInternal(transaction.getMedicalOrgIdentifier());

        if (!TransactionManager.isTransactionUnique(latestBlockHash, transaction.calculateHash(), transaction.getPatientIdentifier()))
            return false;
        PatientInfo patientInfo = PatientInfoManager.load(latestBlockHash, transaction.getPatientIdentifier());
        if (patientInfo == null)
            return false;

        return transaction.verify(patientInfo.getPublicKey(), medicalOrgInfoForInternal.getMedicalOrgInfo().getPublicKey());

    }

    private void processAuthorization(byte[] validatorIdentifier, MedicalOrgInfo[] infos) {
        for (MedicalOrgInfo info : infos) {
            MedicalOrgInfoForInternal newAuthorizedMedOrg = new MedicalOrgInfoForInternal(info, validatorIdentifier);
            cachedCurrentMedicalOrgList.put(info.getIdentifier(), newAuthorizedMedOrg);
        }
    }

    private void processRevocation(byte[][] revokedIdentifiers, ArrayList<Transaction> transactionPool) {
        for (byte[] revokedIdentifier : revokedIdentifiers) {
            cachedCurrentMedicalOrgList.remove(revokedIdentifier);
            Iterator<Transaction> iterator = transactionPool.iterator();
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                if (Arrays.equals(transaction.getMedicalOrgIdentifier(), revokedIdentifier)) {
                    iterator.remove();
                }

            }
        }
    }

    private AuthorityInfo processVote(byte[] voterIdentifier, Vote vote, ArrayList<Vote> myVotes, byte[] myIdentifier, byte[] blockHash) throws IOException {

        boolean isForNewVoting = true;
        Voting changedAuthorityVoting = null;

        if (vote == null)
            return null;

        for (Voting voting : currentVotingList) // if voting about the beneficiary already exists cast a vote
        {
            if (voting.getBeneficiary().equals(vote.getBeneficiary())) {
                isForNewVoting = false;
                if (vote.isAgree())
                    voting.addAgree(voterIdentifier);
                else
                    voting.addDisagree(voterIdentifier);
                changedAuthorityVoting = voting;
                break;
            }

        }

        // create new voting
        if (isForNewVoting) {
            Voting newVoting = new Voting(vote.getBeneficiary(), vote.isAdd());
            newVoting.addAgree(voterIdentifier);
            currentVotingList.add(newVoting);
            changedAuthorityVoting = newVoting;

        }

        boolean isVotingProcessed = changedAuthorityVoting.getNumAgree() >= getValidationInterval(); // processed means either a new authority is trusted or existing authority is untrusted
        boolean isVotingRemoved = false;
        if (isVotingProcessed) {
            isVotingRemoved = true;
            currentVotingList.remove(changedAuthorityVoting);
            if (changedAuthorityVoting.isAdd()) {
                currentOverallAuthorityIdentifierList.add(changedAuthorityVoting.getBeneficiary().getIdentifier());
            } else {
                currentOverallAuthorityIdentifierList.remove(GeneralHelper.getIndexFromArrayList(changedAuthorityVoting.getBeneficiary().getIdentifier(), currentOverallAuthorityIdentifierList));
                cachedCurrentAuthorityList.remove(changedAuthorityVoting.getBeneficiary().getIdentifier());
                for (Voting voting : currentVotingList) {
                    voting.removeDisagree(vote.getBeneficiary().getIdentifier());
                    voting.removeAgree(vote.getBeneficiary().getIdentifier());
                }
            }
        } else if (changedAuthorityVoting.getNumDisagree() > getTotalAuthorities() - getValidationInterval()) {
            isVotingRemoved = true;
            currentVotingList.remove(changedAuthorityVoting);
        }

        if (isVotingRemoved) {
            for (int i = 0; i < myVotes.size(); ++i) {
                if (myVotes.get(i).getBeneficiary().equals(changedAuthorityVoting.getBeneficiary())) {
                    if(isVotingProcessed||!myVotes.get(i).isAgree()) { // if the related voting is processed or it is removed and this authority tried to disagree with that.
                        MyVoteDropManager.drop(blockHash, myIdentifier, myVotes.get(i));
                        myVotes.remove(i);
                    }
                    break;
                }

            }
        }

        return isVotingProcessed ? changedAuthorityVoting.getBeneficiary() : null;
    }

    private void removeFromMyVotes(byte[] blockHash, byte[] myIdentifier, AuthorityInfo processedAuthorityInfo, ArrayList<Vote> myVotes) throws IOException {
        if (processedAuthorityInfo == null)
            return;

        for (int i = 0; i < myVotes.size(); ++i) {
            Vote myVote = myVotes.get(i);
            if (myVote.getBeneficiary().equals(processedAuthorityInfo)) {
                MyVoteDropManager.drop(blockHash, myIdentifier, myVote);
                myVotes.remove(i);
                return;
            }

        }
    }

    private boolean removeFromAuthorizationList(MedicalOrgInfo[] authorizedMedicalOrgInfos, ArrayList<MedicalOrgInfo> authorizationList) {
        boolean removed = false;
        for (MedicalOrgInfo medicalOrgInfo : authorizedMedicalOrgInfos) {
            removed = authorizationList.remove(medicalOrgInfo);
        }

        return removed;
    }

    private void removeFromRevocationList(byte[][] revokedMedicalOrgIdentifiers, SortedSet<byte[]> revocationList) {
        for (byte[] identifier : revokedMedicalOrgIdentifiers) {
            revocationList.remove(identifier);
        }
    }

    private void removeFromPatientInfoList(PatientInfo[] patientInfos, ArrayList<PatientInfo> patientInfoList) {
        for (PatientInfo patientInfo : patientInfos) {
            patientInfoList.remove(patientInfo);
        }
    }

    private void removeFromTransactionPool(Transaction[] transactions, ArrayList<Transaction> transactionPool) {
        for (Transaction transaction : transactions) {
            transactionPool.remove(transaction);
        }
    }

    public void initializeChainWithBestChain(ArrayList<Transaction> transactionPool, ArrayList<Vote> myVotes, ArrayList<MedicalOrgInfo> authorizationList, SortedSet<byte[]> revocationList, ArrayList<PatientInfo> patientInfoList, byte[] myIdentifier) throws Exception {

        if (cachedCurrentChain.size() != 0)
            throw new Exception("The chain is not empty");

        Status bestChainStatus = BestChainInfoManager.load();
        byte[] bestChainHeadBlockHash = bestChainStatus.getLatestBlockHash();
        Block bestChainHeadBlock = BlockManager.loadBlock(bestChainHeadBlockHash);
        currentOverallAuthorityIdentifierList = AuthorityInfoManager.loadOverall(bestChainHeadBlock.calculateHash());
        currentVotingList = VotingManager.load(bestChainHeadBlockHash);
        cachedCurrentChain.add(bestChainHeadBlock);
        totalScore = bestChainStatus.getTotalScore();


        for (int i = 0; i < transactionPool.size(); ++i) {
            if (!checkTransaction(transactionPool.get(i))) {
                transactionPool.remove(i);
                --i;
            }
        }


        for (int i = 0; i < myVotes.size(); ++i) {
            if (!checkVote(myIdentifier, myVotes.get(i))) {
                myVotes.remove(i);
                --i;
            }
        }

        for (int i = 0; i < authorizationList.size(); ++i) {
            if (hasMedicalOrg(authorizationList.get(i).getIdentifier())) {
                authorizationList.remove(i);
                --i;
            }
        }


        Iterator<byte[]> iterator = revocationList.iterator();
        while (iterator.hasNext()) {
            if (!hasMedicalOrg(iterator.next())) {
                iterator.remove();
            }
        }


        for (int i = 0; i < patientInfoList.size(); ++i) {
            if (PatientInfoManager.patientInfoExists(bestChainHeadBlockHash, patientInfoList.get(i).getPatientIdentifier(), patientInfoList.get(i).calculateInfoHash())) {
                patientInfoList.remove(i);
                --i;
            }
        }


    }


    public void loadCurrentBest(ArrayList<Transaction> transactionPool, ArrayList<Vote> myVotes, ArrayList<MedicalOrgInfo> authorizationList, SortedSet<byte[]> revocationList, ArrayList<PatientInfo> patientInfoList, byte[] myIdentifier) throws IOException, BlockChainObjectParsingException, FileCorruptionException {


        Status bestChainStatus = BestChainInfoManager.load();

        byte[] bestChainHeadBlockHash = bestChainStatus.getLatestBlockHash();
        byte[] myLatestBlockHash = getLatestBlockHash();

        if (Arrays.equals(bestChainHeadBlockHash, myLatestBlockHash))
            return;

        Block bestChainHeadBlock = BlockManager.loadBlock(bestChainHeadBlockHash);
        if (isNextBlock(bestChainHeadBlock))     // maybe delete ****
        {
            if (checkNextBlock(bestChainHeadBlock, transactionPool))
                addBlock(bestChainHeadBlock, transactionPool, myVotes, authorizationList, revocationList, patientInfoList, myIdentifier);
        }


        ChainInfo myLatestBlockChainInfo = ChainInfoManager.load(myLatestBlockHash);

        if (myLatestBlockChainInfo.isBestChain()) {
            Stack<Block> blockCollection = new Stack<>();


            Block processingBlock = bestChainHeadBlock;
            blockCollection.push(bestChainHeadBlock);

            while (!Arrays.equals(processingBlock.getHeader().getPrevHash(), myLatestBlockHash)) {
                processingBlock = BlockManager.loadBlock(processingBlock.getHeader().getPrevHash());
                blockCollection.push(processingBlock);
            }

            while (!blockCollection.empty()) {
                addBlock(blockCollection.pop(), transactionPool, myVotes, authorizationList, revocationList, patientInfoList, myIdentifier);
            }
        } else {

            //roll back
            Block processingBlock = getLatestBlock();
            ChainInfo processingBlockChainInfo = myLatestBlockChainInfo;

            while (!processingBlockChainInfo.isBestChain()) {

                if (processingBlock.getHeader().getVote() != null) {
                    if (Arrays.equals(processingBlock.getHeader().getValidatorIdentifier(), myIdentifier))
                        myVotes.add(processingBlock.getHeader().getVote());
                    else {
                        Vote myDroppedVote = MyVoteDropManager.load(myIdentifier, processingBlock.calculateHash());
                        if (myDroppedVote != null)
                            myVotes.add(myDroppedVote);
                    }
                } else if (processingBlock.getHeader().getBlockNumber() % Configuration.CHECK_POINT_BLOCK_INTERVAL == 0) {
                    ArrayList<Vote> myDroppedVotes = MyVoteDropManager.checkPointLoad(processingBlock.calculateHash(), myIdentifier);
                    if (myDroppedVotes == null)
                        throw new FileCorruptionException();
                    for (Vote myDroppedVote : myDroppedVotes)
                        myVotes.add(myDroppedVote);
                }

                if (processingBlock.getContent().getMedicalOrgAuthorizationList() != null) {
                    if (Arrays.equals(processingBlock.getHeader().getValidatorIdentifier(), myIdentifier))
                        authorizationList.addAll(Arrays.asList(processingBlock.getContent().getMedicalOrgAuthorizationList()));
                }
                if (processingBlock.getContent().getMedicalOrgRevocationList() != null) {
                    if (Arrays.equals(processingBlock.getHeader().getValidatorIdentifier(), myIdentifier))
                        revocationList.addAll(Arrays.asList(processingBlock.getContent().getMedicalOrgRevocationList()));
                }
                if (processingBlock.getContent().getPatientInfoList() != null) {
                    if (Arrays.equals(processingBlock.getHeader().getValidatorIdentifier(), myIdentifier))
                        patientInfoList.addAll(Arrays.asList(processingBlock.getContent().getPatientInfoList()));
                }


                if (processingBlock.getContent().getTransactions() != null) {
                    transactionPool.addAll(Arrays.asList(processingBlock.getContent().getTransactions()));
                }


                totalScore -= processingBlock.getHeader().getScore();
                byte[] nextProcessingBlockHash = processingBlock.getHeader().getPrevHash();
                processingBlockChainInfo = ChainInfoManager.load(nextProcessingBlockHash);
                processingBlock = BlockManager.loadBlock(nextProcessingBlockHash);
            }


            currentOverallAuthorityIdentifierList = AuthorityInfoManager.loadOverall(processingBlock.calculateHash());
            currentVotingList = VotingManager.load(processingBlock.calculateHash());
            cachedCurrentAuthorityList.clear();
            cachedCurrentMedicalOrgList.clear();
            cachedCurrentChain.clear();


            //at this point processingBlock holds the point where fork happens - it is already processed
            //add back
            while (processingBlockChainInfo.getNextBlockHash() != null) {
                byte[] nextProcessingBlockHash = processingBlockChainInfo.getNextBlockHash(); //for readibility
                processingBlock = BlockManager.loadBlock(nextProcessingBlockHash);
                processingBlockChainInfo = ChainInfoManager.load(nextProcessingBlockHash);

                addBlock(processingBlock, transactionPool, myVotes, authorizationList, revocationList, patientInfoList, myIdentifier);

            }

        }
    }

}
