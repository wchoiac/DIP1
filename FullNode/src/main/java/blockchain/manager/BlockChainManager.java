package blockchain.manager;

import blockchain.Status;
import blockchain.block.*;
import blockchain.block.transaction.Transaction;
import blockchain.internal.*;
import blockchain.internal.Voting;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class BlockChainManager {

    //return total score, -1 if invalid
    // check for received header list(received from header request)
    public static int checkBlockHeaders(BlockHeader[] headers) throws IOException, BlockChainObjectParsingException {

        if(headers==null||headers.length==0)
            return -1;


        byte[] initPrevBlockHash = headers[0].getPrevHash();
        StateInfo initPrevBlockStateInfo = StateInfoManager.load(headers[0].getPrevHash());
        BlockHeader initPrevBlockHeader = BlockManager.loadBlockHeader(headers[0].getPrevHash());

        if (initPrevBlockStateInfo==null) {
            return -1;
        }

        TreeMap<byte[], AuthorityInfoForInternal> tempAuthorityList = new TreeMap<>(new GeneralHelper.byteArrayComparator());
        ArrayList<byte[]> tempOverallAuthorityList = AuthorityInfoManager.loadOverall(initPrevBlockHash);
        ArrayList<Voting> tempVotingList = VotingManager.load(initPrevBlockHash);

        int tempTotalScore= initPrevBlockStateInfo.getTotalScore();
        long prevBlockTimeStemp = initPrevBlockHeader.getTimestamp();

        for(BlockHeader header: headers)
        {
            int tempValidationInterval = tempOverallAuthorityList.size()/2 +1;
            int index = GeneralHelper.getIndexFromArrayList(header.getValidatorIdentifier(),tempOverallAuthorityList);
            if(index==-1)
                return -1;
            else {
                if (!tempAuthorityList.containsKey(header.getValidatorIdentifier())) {

                    AuthorityInfoForInternal authorityInfoForInternal = AuthorityInfoManager.load(initPrevBlockHash, header.getValidatorIdentifier());
                    tempAuthorityList.put(header.getValidatorIdentifier(), authorityInfoForInternal);
                }
            }


            AuthorityInfoForInternal validatorInfoForInternal = tempAuthorityList.get(header.getValidatorIdentifier());

            try {
                if(!SecurityHelper.verifyRawECDSASignatureWithContent(validatorInfoForInternal.getAuthorityInfo().getPublicKey(), header.getSignatureCoverage(),header.getValidatorSignature(), Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE))
                    return -1;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }


            //=> skip contentHash check

            if(!validatorInfoForInternal.canSign(header.getBlockNumber(),tempOverallAuthorityList.size()))
                return -1;


            boolean isInOrder = AuthorityInfoForInternal.isInOrder(header.getBlockNumber(),tempOverallAuthorityList.size(), index);
            int expectedScore = isInOrder?2:1;
            if(expectedScore!= header.getScore())
                return -1;

            if(header.getTimestamp()> System.currentTimeMillis()||
                    header.getTimestamp() -prevBlockTimeStemp<(isInOrder? Configuration.BLOCK_PERIOD: Configuration.MIN_OUT_ORDER_BLOCK_PERIOD) )
                return -1;


            //check vote and process vote - could be changed later
            if(header.getVote() !=null) {

                //check vote

                Voting changedAuthorityVoting= null;

                if(header.getBlockNumber()%Configuration.CHECK_POINT_BLOCK_INTERVAL==0)
                    return -1;

                if (header.getVote().isAdd()) // return false if authorizing a validator but already in the validator list
                {
                    if (GeneralHelper.getIndexFromArrayList(header.getVote().getBeneficiary().getIdentifier(), tempOverallAuthorityList) != -1)
                        return -1;
                } else // return false if deauthorizing a validator but not in the validator list
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
                        changedAuthorityVoting=v;
                        isForNewVoting = false;
                        break;
                    }

                if (isForNewVoting ) // the voting starter cannot disagree on what he started(not make sense)
                {
                    if(!header.getVote().isAgree())
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
                    if(changedAuthorityVoting.isAdd()) {
                        tempOverallAuthorityList.add(changedAuthorityVoting.getBeneficiary().getIdentifier());
                    }
                    else
                    {
                        tempOverallAuthorityList.remove(GeneralHelper.getIndexFromArrayList(changedAuthorityVoting.getBeneficiary().getIdentifier(),tempOverallAuthorityList));
                        tempAuthorityList.remove(changedAuthorityVoting.getBeneficiary().getIdentifier());
                    }
                    tempVotingList.remove(changedAuthorityVoting);
                }
                else if(changedAuthorityVoting.getNumDisagree() >= tempValidationInterval)
                {
                    tempVotingList.remove(changedAuthorityVoting);
                }

            }

            tempTotalScore+=expectedScore;
            prevBlockTimeStemp= header.getTimestamp();
        }

        return tempTotalScore;

    }

    public static boolean checkBlock(Block block) throws BlockChainObjectParsingException, IOException, FileCorruptionException {
        ArrayList<byte[]> tempOverallAuthorityList = AuthorityInfoManager.loadOverall(block.getHeader().getPrevHash());
        AuthorityInfoForInternal validatorInfoForInternal = AuthorityInfoManager.load(block.getHeader().getPrevHash(), block.getHeader().getValidatorIdentifier());
        StateInfo prevBlockStateInfo =StateInfoManager.load(block.getHeader().getPrevHash());
        byte[] prevBlockHash = block.getHeader().getPrevHash();

        if(prevBlockStateInfo==null||validatorInfoForInternal==null)
            return false;
        try {
            if(!SecurityHelper.verifyRawECDSASignatureWithContent(validatorInfoForInternal.getAuthorityInfo().getPublicKey(), block.getHeader().getSignatureCoverage(),block.getHeader().getValidatorSignature(), Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE))
                return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if(!Arrays.equals(block.getHeader().getContentHash(), block.getContent().calculateHash()))
            return false;

        if(!validatorInfoForInternal.canSign(block.getHeader().getBlockNumber(), prevBlockStateInfo.getTotalAuthorities()))
            return false;

        boolean isInOrder =AuthorityInfoForInternal.isInOrder(block.getHeader().getBlockNumber(), prevBlockStateInfo.getTotalAuthorities(), GeneralHelper.getIndexFromArrayList(block.getHeader().getValidatorIdentifier(),tempOverallAuthorityList));
        int expectedScore = isInOrder?2:1;
        if(expectedScore!= block.getHeader().getScore())
            return false;

        BlockHeader prevBlockHeader = BlockManager.loadBlockHeader(block.getHeader().getPrevHash());
        if(prevBlockHeader==null)
            return false;

        if(block.getHeader().getTimestamp()> System.currentTimeMillis()||
                block.getHeader().getTimestamp() - prevBlockHeader.getTimestamp() <(isInOrder? Configuration.BLOCK_PERIOD: Configuration.MIN_OUT_ORDER_BLOCK_PERIOD) )
            return false;

        if(block.getHeader().getVote() !=null) {

            if(block.getHeader().getBlockNumber()%Configuration.CHECK_POINT_BLOCK_INTERVAL==0) // check point cannot have vote
                return false;

            if (!checkVote(block.getHeader().getPrevHash(), validatorInfoForInternal.getAuthorityInfo(), block.getHeader().getVote()))
                return false;
        }

        //*****header check end


        //*****content check start

        //want to authorize, but already authorized
        if(block.getContent().getMedicalOrgAuthorizationList() !=null) {
            if(block.getContent().getMedicalOrgAuthorizationList().length>Configuration.MAX_AUTHORIZATION)
                return false;

            Set<MedicalOrgInfo> set = new HashSet<>(); //for duplicate check
            for (MedicalOrgInfo medicalOrgInfo : block.getContent().getMedicalOrgAuthorizationList()) {

                if(medicalOrgInfo.getName().length()>Configuration.MAX_NAME_LENGTH)
                    return false;

                if (MedicalOrgInfoManager.load(prevBlockHash, medicalOrgInfo.getIdentifier()) != null)
                    return false;
                if(MedicalOrgInfoManager.isRevoked(prevBlockHash,medicalOrgInfo.getIdentifier()))
                    return false;

                if(set.contains(medicalOrgInfo))
                    return false;
                else
                {
                    set.add(medicalOrgInfo);
                }
            }
        }

        if(block.getContent().getMedicalOrgRevocationList() !=null) {

            if(block.getContent().getMedicalOrgRevocationList().length>Configuration.MAX_REVOCATION)
                return false;

            SortedSet<byte[]> set = new TreeSet<>(new GeneralHelper.byteArrayComparator()); //for duplicate check

            //want to addToRevocationList but not issued such key, invalid
            for (byte[] revokedIdentifier : block.getContent().getMedicalOrgRevocationList()) {
                MedicalOrgInfoForInternal medicalOrgInfoForInternal = MedicalOrgInfoManager.load(prevBlockHash, revokedIdentifier);
                if (medicalOrgInfoForInternal == null)
                    return false;
                if (!Arrays.equals(medicalOrgInfoForInternal.getAuthorityIdentifier(), block.getHeader().getValidatorIdentifier()))
                    return false;

                if(set.contains(revokedIdentifier))
                    return false;
                else
                {
                    set.add(revokedIdentifier);
                }
            }
        }

        if(block.getContent().getPatientInfoList() !=null) {

            if(block.getContent().getPatientInfoList().length>Configuration.MAX_PATIENT_INFO)
                return false;

            Set<PatientInfo> set = new HashSet<>(); //for duplicate check

            for (PatientInfo patientInfo : block.getContent().getPatientInfoList()) {

                if(patientInfo.getTimestamp()>block.getHeader().getTimestamp())
                    return false;

                if(!patientInfo.verify())
                    return false;

                if (PatientInfoManager.patientInfoExists(block.getHeader().getPrevHash(), patientInfo.getPatientIdentifier(), patientInfo.calculateInfoHash())) {
                    return false;

                }

                if(set.contains(patientInfo))
                    return false;
                else
                {
                    set.add(patientInfo);
                }

            }
        }


        if(block.getContent().getTransactions() !=null) {

            if(block.getContent().getTransactions().length>Configuration.MAX_RECORD)
                return false;

            Set<Transaction> set = new HashSet<>(); //for duplicate check

            for (Transaction transaction : block.getContent().getTransactions()) {
                MedicalOrgInfoForInternal medicalOrgInfoForInternal = MedicalOrgInfoManager.load(prevBlockHash, transaction.getMedicalOrgIdentifier());
                if (medicalOrgInfoForInternal == null)
                    return false;

                if(transaction.getTimestamp()>block.getHeader().getTimestamp())
                    return false;

                if (!TransactionManager.isTransactionUnique(block.getHeader().getPrevHash(), transaction.calculateHash(), transaction.getPatientIdentifier()))
                    return false;

                if(set.contains(transaction))
                    return false;
                else
                {
                    set.add(transaction);
                }

                PatientInfo patientInfo = PatientInfoManager.load(block.getHeader().getPrevHash(), transaction.getPatientIdentifier());

                if (!transaction.verify(patientInfo.getPublicKey(), medicalOrgInfoForInternal.getMedicalOrgInfo().getPublicKey()))
                    return false;
            }
        }

        //*****content check end

        return true;
    }

    public static boolean checkVote(byte[] prevBlockHash, AuthorityInfo voter,Vote vote) throws BlockChainObjectParsingException, IOException {

        if(vote.getBeneficiary().getName().length()>Configuration.MAX_NAME_LENGTH)
            return false;

        byte[] beneficiaryIdentifier = vote.getBeneficiary().getIdentifier();
        AuthorityInfoForInternal validatorInfoForInternal = AuthorityInfoManager.load(prevBlockHash,beneficiaryIdentifier);
        ArrayList<Voting> prevVotingList = VotingManager.load(prevBlockHash);

        if (vote.isAdd()) // return false if authorizing a validator but already in the validator list
        {
            if(validatorInfoForInternal != null) // even if untrusted
                return false;
        } else // return false if deauthorizing a validator but not in the validator list
        {
            if(validatorInfoForInternal == null|| validatorInfoForInternal.getUntrustedBlock()!=null) // if not exists ( never trusted or untrusted)
                return false;
        }
        // this ensures that only one voting about one beneficiary to exist

        boolean isForNewVoting = true;
        for (Voting v : prevVotingList) // duplicate voting check
            if (v.getBeneficiary().equals(vote.getBeneficiary())) {
                if (v.isExistingVoter(voter.getIdentifier()))
                    return false;
                isForNewVoting = false;
            }

        return !isForNewVoting || vote.isAgree();

    }

    // block should be checked beforehand
    public static int storeBlock(Block block) throws IOException, BlockChainObjectParsingException {
        byte[] blockHash = block.calculateHash();
        StateInfo prevBlockStateInfo=StateInfoManager.load(block.getHeader().getPrevHash());

        BlockManager.saveBlock(block);

        boolean authorityListChanged = false;
        boolean votingListChanged =false;

        if(block.getHeader().getVote() !=null) {
            votingListChanged=true;
            authorityListChanged = processVote(blockHash, block.getHeader().getPrevHash(), block.getHeader().getValidatorIdentifier(),
                    block.getHeader().getVote(), prevBlockStateInfo.getTotalAuthorities());
        } else if(block.getHeader().getBlockNumber()%Configuration.CHECK_POINT_BLOCK_INTERVAL==0)
        {
            votingListChanged=true;
            VotingManager.saveCheckPoint(blockHash);
        }



        StateInfoManager.save(block,authorityListChanged,votingListChanged);
        ChainInfoManager.save(block);



        if(block.getContent().getMedicalOrgAuthorizationList() !=null)
            processAuthorization(blockHash, block.getHeader().getValidatorIdentifier(), block.getContent().getMedicalOrgAuthorizationList());
        if(block.getContent().getMedicalOrgRevocationList() !=null)
            processRevocation(blockHash, block.getContent().getMedicalOrgRevocationList());
        if(block.getContent().getPatientInfoList() !=null)
            processRegistration(blockHash, block.getContent().getPatientInfoList());
        if(block.getContent().getTransactions() !=null)
            processTransactions(blockHash, block.getContent().getTransactions());



        int newTotalScore = prevBlockStateInfo.getTotalScore() + block.getHeader().getScore();

        Status bestChainStatus = BestChainInfoManager.load();

        if(newTotalScore> bestChainStatus.getTotalScore())
        {
            changeBestChain(blockHash);
            BestChainInfoManager.save(new Status(newTotalScore, blockHash));
        }

        return newTotalScore;

    }


    private static void processAuthorization(byte[] blockHash, byte[] authorityIdentifier, MedicalOrgInfo[] medicalOrgAuthorizationList) throws IOException {
        for(MedicalOrgInfo medicalOrgInfo:medicalOrgAuthorizationList)
            MedicalOrgInfoManager.authorize(blockHash,medicalOrgInfo,authorityIdentifier);
    }

    private static void processRevocation(byte[] blockHash, byte[][] medicalOrgRevocationList) throws IOException {
        for(byte[] revokedIdentifier:medicalOrgRevocationList)
            MedicalOrgInfoManager.revoke(blockHash,revokedIdentifier);
    }

    private static void processRegistration(byte[] blockHash, PatientInfo[] patientRegistrationList) throws IOException {
        for(PatientInfo patientInfo :patientRegistrationList)
            PatientInfoManager.save(blockHash, patientInfo);
    }

    private static void processTransactions(byte[] blockHash, Transaction[] transactions) throws BlockChainObjectParsingException, IOException {
        SortedMap<byte[], String> tempMedicalOrgNames=new TreeMap<>(new GeneralHelper.byteArrayComparator());
        for(Transaction transaction: transactions) {

            String name;
            if (!tempMedicalOrgNames.containsKey(transaction.getMedicalOrgIdentifier()))
            {
                name=MedicalOrgInfoManager.loadName(blockHash,transaction.getMedicalOrgIdentifier());
                tempMedicalOrgNames.put(transaction.getMedicalOrgIdentifier(),name);
            }
            else
            {
                name=tempMedicalOrgNames.get(transaction.getMedicalOrgIdentifier());
            }
            TransactionManager.save(blockHash, transaction,name);
        }
    }

    // return true if the vote is processed( more than half agreed )
    // Also, store the changed voting list
    private static boolean processVote(byte[] blockHash, byte[] prevBlockHash,byte[] voterIdentifier, Vote vote, int prevTotalAuthorities) throws IOException, BlockChainObjectParsingException {

        int validationInterval= (prevTotalAuthorities/2) +1;
        ArrayList<Voting> votingList =VotingManager.load(prevBlockHash);

        boolean isForNewVoting = true;
        Voting changedAuthorityVoting= null;

        for (Voting voting : votingList) // if voting about the beneficiary already exists cast a vote
        {
            if (voting.getBeneficiary().equals(vote.getBeneficiary())) {
                isForNewVoting=false;
                if (vote.isAgree())
                    voting.addAgree(voterIdentifier);
                else
                    voting.addDisagree(voterIdentifier);
                changedAuthorityVoting=voting;
                break;
            }

        }

        // create new voting
        if(isForNewVoting) {
            Voting newVoting = new Voting(vote.getBeneficiary(), vote.isAdd());
            newVoting.addAgree(voterIdentifier);
            votingList.add(newVoting);
            changedAuthorityVoting=newVoting;

        }


        boolean isVotingProcessed= changedAuthorityVoting.getNumAgree() >= validationInterval;
        if (isVotingProcessed) {
            votingList.remove(changedAuthorityVoting);
            if(changedAuthorityVoting.isAdd()) {
                AuthorityInfoManager.trust(blockHash, prevBlockHash, vote.getBeneficiary());
            }
            else
            {
                AuthorityInfoManager.untrust(blockHash,prevBlockHash, vote.getBeneficiary());
                for(Voting voting: votingList)
                {
                    voting.removeDisagree(vote.getBeneficiary().getIdentifier());
                    voting.removeAgree(vote.getBeneficiary().getIdentifier());
                }
            }
        }
        else if(changedAuthorityVoting.getNumDisagree() > prevTotalAuthorities-validationInterval)
        {
            votingList.remove(changedAuthorityVoting);
        }

        VotingManager.save(blockHash,votingList);

        return isVotingProcessed;
    }

    public static boolean hasBlock(byte[] blockHash) throws BlockChainObjectParsingException, IOException {
        return BlockManager.loadBlockHeader(blockHash)!=null;
    }

    public static void changeBestChain(byte[] newHeadBlockHash) throws IOException, BlockChainObjectParsingException {

        byte[] processingBlockHash= newHeadBlockHash;
        byte[] nextBlockhash = null;


            while(true) {
                ChainInfo processingBlockChainInfoBeforeChange =ChainInfoManager.load(processingBlockHash);
                boolean fromPrevMainChain= processingBlockChainInfoBeforeChange.isBestChain();
                ChainInfoManager.changeChainInfo(processingBlockHash,true,nextBlockhash);
                if(fromPrevMainChain)
                    break;

                nextBlockhash=processingBlockHash;
                processingBlockHash= processingBlockChainInfoBeforeChange.getPrevBlockHash();
            }

            byte[] targetBlockHash = processingBlockHash;
            processingBlockHash= BestChainInfoManager.load().getLatestBlockHash();
            while(!Arrays.equals(targetBlockHash,processingBlockHash))
            {
                ChainInfo processingBlockChainInfoBeforeChange =ChainInfoManager.load(processingBlockHash);
                ChainInfoManager.changeChainInfo(processingBlockHash,false,null);

                processingBlockHash= processingBlockChainInfoBeforeChange.getPrevBlockHash();
            }

    }

    //checks if the checking block is behind the baseblock (or the same block) and in the same chain
    //use the main chain for effective checking( using the fact that branches won't be too long from the main chain)
    public static boolean isThisBlockOnTheChain(byte[] baseBlockHash, byte[] checkingBlockHash) throws BlockChainObjectParsingException, IOException {

        if(Arrays.equals(baseBlockHash,checkingBlockHash))
            return true;

        ChainInfo baseBlockChainInfo = ChainInfoManager.load(baseBlockHash);
        ChainInfo checkingBlockChainInfo = ChainInfoManager.load(checkingBlockHash);

        boolean isBaseBlockMainChain = baseBlockChainInfo.isBestChain();
        boolean isCheckingBlockInMainChain = checkingBlockChainInfo.isBestChain();

        int baseBlockNumber = BlockManager.loadBlockHeader(baseBlockHash).getBlockNumber();
        int checkingBlockNumber = BlockManager.loadBlockHeader(checkingBlockHash).getBlockNumber();


        boolean result;

        if(baseBlockNumber<=checkingBlockNumber) // same block number but different hash obviously different chain
            result= false;
        else if(isBaseBlockMainChain && isCheckingBlockInMainChain)
            result= true;
        else if(isBaseBlockMainChain)
            result= false;
        else
        {
            ChainInfo processingBlockChainInfo = baseBlockChainInfo;
            byte[] processingBlockHash = baseBlockHash;
            while(!processingBlockChainInfo.isBestChain())
            {
                processingBlockHash = processingBlockChainInfo.getPrevBlockHash();
                processingBlockChainInfo = ChainInfoManager.load(processingBlockChainInfo.getPrevBlockHash());
            }

            BlockHeader firstInChainBaseBlockHeader = BlockManager.loadBlockHeader(processingBlockHash);
            if(isCheckingBlockInMainChain) {
                result= firstInChainBaseBlockHeader.getBlockNumber() >= checkingBlockNumber;
            }
            else
            {
                ChainInfo processingCheckingBlockChainInfo = checkingBlockChainInfo;
                while(processingCheckingBlockChainInfo.isBestChain())
                {
                    processingCheckingBlockChainInfo = ChainInfoManager.load(processingCheckingBlockChainInfo.getPrevBlockHash());
                }

                BlockHeader firstInChainCheckingBlockHeader = BlockManager.loadBlockHeader(processingCheckingBlockChainInfo.getPrevBlockHash());

                result= firstInChainBaseBlockHeader.getBlockNumber() >= firstInChainCheckingBlockHeader.getBlockNumber();
            }
        }

        return result;


    }
}
