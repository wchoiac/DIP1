package rest.server;

import blockchain.block.PatientInfo;
import blockchain.block.transaction.Transaction;
import blockchain.manager.TransactionManager;
import blockchain.manager.datastructure.Location;
import blockchain.manager.datastructure.PatientShortInfo;
import blockchain.manager.datastructure.RecordShortInfo;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import general.security.SecurityHelper;
import node.fullnode.FullNode;
import rest.pojo.*;
import rest.server.exception.BadRequest;
import rest.server.exception.InvalidUserInfo;
import rest.server.exception.NotFound;
import rest.server.manager.SessionManager;

import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

public class FullNodeAPIResolver { // change to full node
    private FullNode fullNode;


    public FullNodeAPIResolver(FullNode fullNode)
    {
        this.fullNode = fullNode;
    }

    /*
     * return Secure token
     */
    public String apiLogin(UserInfoPojo userInfoPojo) throws IOException, BadRequest, InvalidUserInfo {

        if (userInfoPojo == null || userInfoPojo.getPassword() == null || userInfoPojo.getUsername() == null)
            throw new BadRequest();

        return SessionManager.login(userInfoPojo);
    }


    /*
     *return status code(1 byte) + transaction id(32 byte) (if successful):
     *
     * status code:
     * 0: successful
     * 1: being processed
     * 2: already exists or invalid (e.g. wrong signature)
     *
     * transaction id:
     * SHA256 hash of the transaction
     */
    public byte[] addTransaction(TransactionPojo transactionPojo) throws IOException, BlockChainObjectParsingException, BadRequest, FileCorruptionException {

        if(transactionPojo==null||transactionPojo.getEncryptedRecord()==null||
                transactionPojo.getPatientIdentifier()==null||transactionPojo.getPatientSignature()==null
                ||transactionPojo.getTimestamp()>System.currentTimeMillis()
        ||!transactionPojo.isSignatureDEREncoded()&&  transactionPojo.getPatientSignature().length!= Configuration.SIGNATURE_LENGTH)
            throw new BadRequest();


        byte[] rawSignature = transactionPojo.isSignatureDEREncoded()
                ? SecurityHelper.getRawFromDERECDSASignature(transactionPojo.getPatientSignature(),Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH)
                :transactionPojo.getPatientSignature();

        return fullNode.addToTransactionPool(transactionPojo.getTimestamp(),transactionPojo.getEncryptedRecord(),
                rawSignature,transactionPojo.getPatientIdentifier());

    }

    /*
    * return list of record's short information of the patient
     */
    public ArrayList<RecordShortInfoPojo> getRecordShortInfoList(byte[] patientIdentifier) throws BlockChainObjectParsingException, InvalidKeySpecException, IOException, BadRequest, NotFound {

        if(patientIdentifier==null||patientIdentifier.length!= Configuration.IDENTIFIER_LENGTH)
            throw new BadRequest();

        ArrayList<RecordShortInfo> recordShortInfos =fullNode.getRecordShortInfoList(patientIdentifier);

        if(recordShortInfos==null)
            throw new NotFound();

        ArrayList<RecordShortInfoPojo> recordShortInfoPojos = new ArrayList<>();

        for(RecordShortInfo recordShortInfo : recordShortInfos)
        {
            RecordShortInfoPojo recordShortInfoPojo = new RecordShortInfoPojo();
            LocationPojo locationPojo= new LocationPojo();
            locationPojo.setBlockHash(recordShortInfo.getLocation().getBlockHash());
            locationPojo.setTargetIdentifier(recordShortInfo.getLocation().getTargetIdentifier());
            recordShortInfoPojo.setLocationPojo(locationPojo);
            recordShortInfoPojo.setTimestamp(recordShortInfo.getTimestamp());
            recordShortInfoPojo.setMedicalOrgName(recordShortInfo.getMedicalOrgName());
            recordShortInfoPojos.add(recordShortInfoPojo);
        }

        return recordShortInfoPojos;
    }

    /*
     * return list of the requested records' contents
     */
    public ArrayList<RecordContentPojo> getRecordContentsList(ArrayList<LocationPojo> locationPojos) throws IOException, BlockChainObjectParsingException, NotFound, BadRequest {

        ArrayList<RecordContentPojo> recordContentPojos = new ArrayList<>();

        for(LocationPojo locationPojo : locationPojos) {

            if (locationPojo == null
                    || locationPojo.getBlockHash() == null
                    || locationPojo.getBlockHash().length !=Configuration.HASH_LENGTH
                    || locationPojo.getTargetIdentifier() == null
                    ||locationPojo.getTargetIdentifier().length!=Configuration.HASH_LENGTH) {
                throw new BadRequest();
            }

            Transaction transaction = TransactionManager.load(new Location(locationPojo.getBlockHash(), locationPojo.getTargetIdentifier()));

            if (transaction == null) {
                throw new NotFound(); // patient doesn't exist
            }


            RecordContentPojo recordContentPojo = new RecordContentPojo();
            recordContentPojo.setEncryptedRecord(transaction.getEncryptedRecord());
            recordContentPojos.add(recordContentPojo);
        }

        return recordContentPojos;
    }

   /*
     * return list of short information of the patient
     */
    public ArrayList<PatientShortInfoPojo> getPatientShortInfoList(byte[] patientIdentifier) throws IOException, BlockChainObjectParsingException, NotFound, BadRequest {

        if (patientIdentifier == null
                || patientIdentifier.length != Configuration.IDENTIFIER_LENGTH)
            throw new BadRequest();

        ArrayList<PatientShortInfo> patientShortInfos = fullNode.getPatientShortInfoList(patientIdentifier);

        if (patientShortInfos == null)
            throw new NotFound();

        ArrayList<PatientShortInfoPojo> patientShortInfoPojos = new ArrayList<>();

        for (PatientShortInfo patientShortInfo : patientShortInfos) {
            PatientShortInfoPojo patientShortInfoPojo = new PatientShortInfoPojo();
            LocationPojo locationPojo = new LocationPojo();
            locationPojo.setBlockHash(patientShortInfo.getLocation().getBlockHash());
            locationPojo.setTargetIdentifier(patientShortInfo.getLocation().getTargetIdentifier());
            patientShortInfoPojo.setLocation(locationPojo);
            patientShortInfoPojo.setTimestamp(patientShortInfo.getTimestamp());
            patientShortInfoPojos.add(patientShortInfoPojo);
        }

        return patientShortInfoPojos;
    }

    /*
     * return list of the requested patient information contents
     */
    public ArrayList<PatientInfoContentPojo> getPatientInfoContentsList(ArrayList<LocationPojo> locationPojos) throws IOException, BlockChainObjectParsingException, BadRequest, NotFound {

        ArrayList<PatientInfoContentPojo> patientInfoContentPojos = new ArrayList<>();

        for (LocationPojo locationPojo : locationPojos) {

            if (locationPojo == null
                    || locationPojo.getBlockHash() == null
                    || locationPojo.getBlockHash().length !=Configuration.HASH_LENGTH
                    || locationPojo.getTargetIdentifier() == null
                    ||locationPojo.getTargetIdentifier().length!=Configuration.HASH_LENGTH) {
                throw new BadRequest();
            }

            byte[] patientEncryptedInfo = fullNode.getPatientEncryptedInfo(new Location(locationPojo.getBlockHash(), locationPojo.getTargetIdentifier()));

            if (patientEncryptedInfo == null) {
                throw new NotFound();
            }

            PatientInfoContentPojo patientInfoContentPojo = new PatientInfoContentPojo();
            patientInfoContentPojo.setEncryptedInfo(patientEncryptedInfo);
            patientInfoContentPojos.add(patientInfoContentPojo);
        }

        return patientInfoContentPojos;
    }

    /*
     * return this medical organization's identifier
     */
    public byte[] getMedicalOrgIdentifier()
    {
        return FullNode.getRunningFullNode().getMyIdentifier();
    }


    public boolean isValidUserLevelToken(String token) throws IOException {
        return SessionManager.isValidLevelToken(token, Configuration.USER_LEVEL)
                || SessionManager.isValidLevelToken(token, Configuration.ROOT_USER_LEVEL);
    }

    public boolean isValidRootLevelToken(String token) throws IOException {
        return SessionManager.isValidLevelToken(token, Configuration.ROOT_USER_LEVEL);
    }
}
