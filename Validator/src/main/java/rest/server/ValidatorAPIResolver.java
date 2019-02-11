package rest.server;

import blockchain.internal.Voting;
import blockchain.block.*;
import blockchain.manager.datastructure.Location;
import blockchain.manager.datastructure.MedicalOrgShortInfo;
import blockchain.manager.datastructure.PatientShortInfo;
import blockchain.manager.datastructure.RecordShortInfo;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import general.utility.GeneralHelper;
import node.validator.Validator;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.operator.OperatorCreationException;
import rest.pojo.*;
import rest.server.exception.BadRequest;
import rest.server.exception.InvalidUserInfo;
import rest.server.exception.NotFound;
import rest.server.exception.ServerError;
import rest.server.manager.CertificateManager;
import rest.server.manager.SessionManager;
import general.security.SecurityHelper;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;


public class ValidatorAPIResolver {

    private Validator validator;


    public ValidatorAPIResolver(Validator validator) {
        this.validator = validator;
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
     * return list of authority information
     */
    public ArrayList<AuthorityInfoPojo> getOverallList() throws IOException, BlockChainObjectParsingException {

        ArrayList<AuthorityInfo> authorityInfos = validator.getOverallAuthorityList();
        ArrayList<AuthorityInfoPojo> authorityInfoPojos = new ArrayList<>();

        for (AuthorityInfo authorityInfo : authorityInfos) {
            AuthorityInfoPojo tempAuthorityInfoPojo = new AuthorityInfoPojo(authorityInfo.getName()
                    ,authorityInfo.getPublicKey().getEncoded(),true);
            authorityInfoPojos.add(tempAuthorityInfoPojo);
        }

        return authorityInfoPojos;
    }

    /*
     * return list of votes that to be processed
     */
    public ArrayList<VotePojo> getMyVotes() {

        ArrayList<Vote> votes = validator.getMyVotes();
        ArrayList<VotePojo> votePojos = new ArrayList<>();

        for (Vote vote : votes) {
            AuthorityInfoPojo tempAuthorityInfoPojo = new AuthorityInfoPojo(vote.getBeneficiary().getName()
                    ,vote.getBeneficiary().getPublicKey().getEncoded(),true);

            VotePojo tempVotePojo = new VotePojo();
            tempVotePojo.setBeneficiary(tempAuthorityInfoPojo);
            tempVotePojo.setAdd(vote.isAdd());
            tempVotePojo.setAgree(vote.isAgree());
            votePojos.add(tempVotePojo);
        }


        return votePojos;
    }

    /*
     * return list of on-going votings
     */
    public ArrayList<VotingPojo> getCurrentVotingList() {

        ArrayList<Voting> votings = validator.getVotingList();
        ArrayList<VotingPojo> votingPojos = new ArrayList<>();

        for (Voting voting : votings) {
            AuthorityInfoPojo tempAuthorityInfoPojo = new AuthorityInfoPojo(voting.getBeneficiary().getName()
                    ,voting.getBeneficiary().getPublicKey().getEncoded(),true);

            VotingPojo tempVotingPojo = new VotingPojo();
            tempVotingPojo.setBeneficiary(tempAuthorityInfoPojo);
            tempVotingPojo.setAdd(voting.isAdd());
            tempVotingPojo.setAgree(voting.getNumAgree());
            tempVotingPojo.setDisagree(voting.getNumDisagree());
            tempVotingPojo.setVoted(voting.isExistingVoter(validator.getMyIdentifier()));
            votingPojos.add(tempVotingPojo);
        }


        return votingPojos;
    }


    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: cannot be processed due to duplicate vote, etc
     */
    public byte castVote(VotePojo votePojo) throws InvalidKeySpecException, IOException, BlockChainObjectParsingException, BadRequest {
        if (votePojo == null || votePojo.getBeneficiary() == null
                || votePojo.getBeneficiary().getName() == null
                || votePojo.getBeneficiary().getName().length() > Configuration.MAX_NAME_LENGTH
                || votePojo.getBeneficiary().getEcPublicKey() == null
                || (!votePojo.getBeneficiary().isKeyDEREncoded()
                && votePojo.getBeneficiary().getEcPublicKey().length != Configuration.RAW_PUBLICKEY_LENGTH))
            throw new BadRequest();


        if(votePojo.getBeneficiary().isKeyDEREncoded())
        {
            if(!SecurityHelper.checkCurve(votePojo.getBeneficiary().getEcPublicKey(),Configuration.ELIPTIC_CURVE_OID))
                throw new BadRequest("Wrong curve");
        }

        ECPublicKey ecPublicKey = votePojo.getBeneficiary().isKeyDEREncoded()
                ? SecurityHelper.getECPublicKeyFromEncoded(votePojo.getBeneficiary().getEcPublicKey()) //check curve
                : SecurityHelper.getECPublicKeyFromCompressedRaw(votePojo.getBeneficiary().getEcPublicKey(), Configuration.ELIPTIC_CURVE);

        AuthorityInfo tempAuthorityInfo = new AuthorityInfo(votePojo.getBeneficiary().getName(),ecPublicKey);


        Vote vote = new Vote(tempAuthorityInfo, votePojo.isAdd(), votePojo.isAgree());

        return validator.castVote(vote);

    }

    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: already exists
     */

    public byte register(PatientInfoPojo patientInfoPojo) throws InvalidKeySpecException, IOException, BlockChainObjectParsingException, ServerError, BadRequest {

        if (patientInfoPojo == null
                || patientInfoPojo.getEncryptedInfo() == null
                || patientInfoPojo.getEcPublicKey() == null
                || (!patientInfoPojo.isKeyDEREncoded()
                && patientInfoPojo.getEcPublicKey().length != Configuration.RAW_PUBLICKEY_LENGTH)
                || patientInfoPojo.getSignature() == null
                || (!patientInfoPojo.isSignatureDEREncoded()
                && patientInfoPojo.getSignature().length != Configuration.SIGNATURE_LENGTH)
        || patientInfoPojo.getTimestamp()>System.currentTimeMillis())
            throw new BadRequest("Bad data");

        if(patientInfoPojo.isKeyDEREncoded())
        {
            if(!SecurityHelper.checkCurve(patientInfoPojo.getEcPublicKey(),Configuration.ELIPTIC_CURVE_OID))
                throw new BadRequest("Wrong curve");
        }

        ECPublicKey ecPublicKey = patientInfoPojo.isKeyDEREncoded()
                ? SecurityHelper.getECPublicKeyFromEncoded(patientInfoPojo.getEcPublicKey()) //check curve
                : SecurityHelper.getECPublicKeyFromCompressedRaw(patientInfoPojo.getEcPublicKey(), Configuration.ELIPTIC_CURVE);

        byte[] rawSignature = patientInfoPojo.isSignatureDEREncoded()
                ? SecurityHelper.getRawFromDERECDSASignature(patientInfoPojo.getSignature(), Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH)
                : patientInfoPojo.getSignature();


        byte[] timestampBytes = GeneralHelper.longToBytes(patientInfoPojo.getTimestamp());
        byte[] signatureCoverage = GeneralHelper.mergeByteArrays(timestampBytes, patientInfoPojo.getEncryptedInfo());

        try {
            if (!SecurityHelper.verifyRawECDSASignatureWithContent(ecPublicKey,signatureCoverage, rawSignature
                    , Configuration.BLOCKCHAIN_HASH_ALGORITHM, Configuration.ELIPTIC_CURVE))
                throw new BadRequest("Bad signature");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ServerError(); // not expected
        }

        PatientInfo patientInfo = new PatientInfo(patientInfoPojo.getTimestamp()
                , ecPublicKey, patientInfoPojo.getEncryptedInfo(), rawSignature);

        return validator.addToPatientInfoListForRegistration(patientInfo);
    }

    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: patient doesn't exist
     * 3: already updated
     */
    public byte update(PatientInfoPojo patientInfoPojo) throws InvalidKeySpecException, IOException, BlockChainObjectParsingException, BadRequest, ServerError {

        if (patientInfoPojo == null
                || patientInfoPojo.getEncryptedInfo() == null
                || patientInfoPojo.getEcPublicKey() == null
                || (!patientInfoPojo.isKeyDEREncoded()
                && patientInfoPojo.getEcPublicKey().length != Configuration.RAW_PUBLICKEY_LENGTH)
                || patientInfoPojo.getSignature() == null
                || (!patientInfoPojo.isSignatureDEREncoded()
                && patientInfoPojo.getSignature().length != Configuration.SIGNATURE_LENGTH)
                || patientInfoPojo.getTimestamp()>System.currentTimeMillis())
            throw new BadRequest("Bad data");

        if(patientInfoPojo.isKeyDEREncoded())
        {
            if(!SecurityHelper.checkCurve(patientInfoPojo.getEcPublicKey(),Configuration.ELIPTIC_CURVE_OID))
                throw new BadRequest("Wrong curve");
        }

        ECPublicKey ecPublicKey = patientInfoPojo.isKeyDEREncoded()
                ? SecurityHelper.getECPublicKeyFromEncoded(patientInfoPojo.getEcPublicKey()) //check curve
                : SecurityHelper.getECPublicKeyFromCompressedRaw(patientInfoPojo.getEcPublicKey(), Configuration.ELIPTIC_CURVE);

        byte[] rawSignature = patientInfoPojo.isSignatureDEREncoded()
                ? SecurityHelper.getRawFromDERECDSASignature(patientInfoPojo.getSignature(), Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH)
                : patientInfoPojo.getSignature();

        byte[] timestampBytes = GeneralHelper.longToBytes(patientInfoPojo.getTimestamp());
        byte[] signatureCoverage = GeneralHelper.mergeByteArrays(timestampBytes, patientInfoPojo.getEncryptedInfo());

        try {
            if (!SecurityHelper.verifyRawECDSASignatureWithContent(ecPublicKey,signatureCoverage, rawSignature
                    , Configuration.BLOCKCHAIN_HASH_ALGORITHM, Configuration.ELIPTIC_CURVE))
                throw new BadRequest("Bad signature");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ServerError(); // not expected
        }

        PatientInfo patientInfo = new PatientInfo(patientInfoPojo.getTimestamp()
                , ecPublicKey, patientInfoPojo.getEncryptedInfo(), rawSignature);


        return validator.addToPatientInfoListForUpdate(patientInfo);
    }


    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: authorized by another authority
     */
    public byte revoke(byte[] revokedIdentifier) throws IOException, BlockChainObjectParsingException, BadRequest, FileCorruptionException, NotFound {
        if (revokedIdentifier == null || revokedIdentifier.length!=Configuration.IDENTIFIER_LENGTH)
            throw new BadRequest("Bad data");


        return validator.addToRevocationList(revokedIdentifier);
    }

    /*
     * return status code(1 byte) + encoded certificate:
     * status code:
     * 0: successful
     * 1: being processed
     * 2: already exists
     */
    public byte[] authorize(AuthorizationRequestPojo authorizationRequestPojo) throws OperatorCreationException, CertificateException, IOException, NoSuchAlgorithmException, BlockChainObjectParsingException, InvalidKeySpecException, BadRequest, FileCorruptionException {

        if (authorizationRequestPojo == null || authorizationRequestPojo.getMedicalOrgInfo() == null
                || authorizationRequestPojo.getMedicalOrgInfo().getName() == null
                || authorizationRequestPojo.getMedicalOrgInfo().getName().length() > Configuration.MAX_NAME_LENGTH
                || authorizationRequestPojo.getMedicalOrgInfo().getEcPublicKey() == null
                || (!authorizationRequestPojo.getMedicalOrgInfo().isKeyDEREncoded()
                && authorizationRequestPojo.getMedicalOrgInfo().getEcPublicKey().length != Configuration.RAW_PUBLICKEY_LENGTH)
                || authorizationRequestPojo.getNoAfter() == null
                || authorizationRequestPojo.getNoAfter().getTime() <= System.currentTimeMillis())
            throw new BadRequest("Bad data");


        if(authorizationRequestPojo.getMedicalOrgInfo().isKeyDEREncoded())
        {
            if(!SecurityHelper.checkCurve(authorizationRequestPojo.getMedicalOrgInfo().getEcPublicKey(),Configuration.ELIPTIC_CURVE_OID))
                throw new BadRequest("Wrong curve");
        }


        ECPublicKey ecPublicKey = authorizationRequestPojo.getMedicalOrgInfo().isKeyDEREncoded()
                ? SecurityHelper.getECPublicKeyFromEncoded(authorizationRequestPojo.getMedicalOrgInfo().getEcPublicKey())
                : SecurityHelper.getECPublicKeyFromCompressedRaw(authorizationRequestPojo.getMedicalOrgInfo().getEcPublicKey(), Configuration.ELIPTIC_CURVE);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        MedicalOrgInfo medicalOrgInfo = new MedicalOrgInfo(authorizationRequestPojo.getMedicalOrgInfo().getName()
                , ecPublicKey);


        byte authorizationResult = validator.addToAuthorizationList(medicalOrgInfo);
        result.write(authorizationResult);

        if (authorizationResult == 0) {
            X509Certificate x509Certificate = validator.issueCertificate(medicalOrgInfo, authorizationRequestPojo.getNoAfter());

            CertificateManager.store(x509Certificate);
            result.write(x509Certificate.getEncoded());

        }

        return result.toByteArray();
    }


    /*
     * return status code(1 byte) + encoded certificate:
     * status code:
     * 0: successful
     * 1: not authorized
     * 2: not authorized by me
     */
    public byte[] renewCertificate(CertificateRenewRequestPojo certificateRenewRequestPojo) throws OperatorCreationException, CertificateException, IOException, NoSuchAlgorithmException, BlockChainObjectParsingException, BadRequest, FileCorruptionException {

        if (certificateRenewRequestPojo==null
        ||certificateRenewRequestPojo.getIdentifier() == null || certificateRenewRequestPojo.getIdentifier().length!=Configuration.IDENTIFIER_LENGTH
                ||certificateRenewRequestPojo.getNoAfter()==null
        || certificateRenewRequestPojo.getNoAfter().getTime() <= System.currentTimeMillis())
            throw new BadRequest("Bad data");


        ByteArrayOutputStream result = new ByteArrayOutputStream();

        byte isAuthorizedByMe = validator.isMedicalOrgAuthorizedByMe(certificateRenewRequestPojo.getIdentifier());
        result.write(isAuthorizedByMe);

        if (isAuthorizedByMe == 0) {
            MedicalOrgInfo medicalOrgInfo = validator.getMedicalOrgInfo(certificateRenewRequestPojo.getIdentifier());
            X509Certificate x509Certificate = validator.issueCertificate(medicalOrgInfo, certificateRenewRequestPojo.getNoAfter());
            CertificateManager.store(x509Certificate);
            result.write(x509Certificate.getEncoded());
        }

        return result.toByteArray();
    }


    /*
     * return list of information of medical organizations authorized by this authority
     */
    public ArrayList<MedicalOrgShortInfoPojo> loadAllMedicalOrgShortInfoAuthorizedByThisAuthority() throws BlockChainObjectParsingException, IOException {
        ArrayList<MedicalOrgShortInfo> simplifiedMedicalOrgInfos = validator.getAllMedicalOrgShortInfoAuthorizedBy(validator.getMyIdentifier());
        ArrayList<MedicalOrgShortInfoPojo> medicalOrgShortInfoPojos = new ArrayList<>();

        for (MedicalOrgShortInfo medicalOrgShortInfo : simplifiedMedicalOrgInfos) {
            MedicalOrgShortInfoPojo medicalOrgShortInfoPojo = new MedicalOrgShortInfoPojo();
            medicalOrgShortInfoPojo.setName(medicalOrgShortInfo.getName());
            medicalOrgShortInfoPojo.setIdentifier(medicalOrgShortInfo.getIdentifier());

            medicalOrgShortInfoPojos.add(medicalOrgShortInfoPojo);
        }

        return medicalOrgShortInfoPojos;
    }

    /*
     * return the corresponding encoded X509 certificate
     */
    public byte[] getCertificate(byte[] identifier) throws CertificateEncodingException, BadRequest, NotFound {
        if (identifier == null || identifier.length != Configuration.IDENTIFIER_LENGTH)
            throw new BadRequest("Bad data");

        if (!CertificateManager.exist(identifier))
            throw new NotFound("The requested cerificate couldn't be found");

        return CertificateManager.get(identifier).getEncoded();
    }


    /*
     * return list of short information of the patient
     */
    public ArrayList<PatientShortInfoPojo> getPatientShortInfoList(byte[] patientIdentifier) throws IOException, BlockChainObjectParsingException, NotFound, BadRequest {

        if (patientIdentifier == null
                || patientIdentifier.length != Configuration.IDENTIFIER_LENGTH)
            throw new BadRequest();

        ArrayList<PatientShortInfo> patientShortInfos = validator.getPatientShortInfoList(patientIdentifier);

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
                    || locationPojo.getBlockHash().length != Configuration.HASH_LENGTH
                    || locationPojo.getTargetIdentifier() == null
                    || locationPojo.getTargetIdentifier().length != Configuration.HASH_LENGTH) {
                throw new BadRequest();
            }

            byte[] encryptedInfo = Validator.getRunningValidator().getPatientEncryptedInfo(new Location(locationPojo.getBlockHash(), locationPojo.getTargetIdentifier()));

            if (encryptedInfo == null) {
                throw new NotFound("One or more of the requested information couldn't be found");
            }

            patientInfoContentPojos.add(new PatientInfoContentPojo(encryptedInfo));
        }

        return patientInfoContentPojos;
    }

    /*
     * return list of record's short information of the patient
     */
    public ArrayList<RecordShortInfoPojo> getRecordShortInfoList(byte[] patientIdentifier) throws BlockChainObjectParsingException, InvalidKeySpecException, IOException, BadRequest, NotFound {

        if(patientIdentifier==null||patientIdentifier.length!= Configuration.IDENTIFIER_LENGTH)
            throw new BadRequest();

        ArrayList<RecordShortInfo> recordShortInfos =validator.getRecordShortInfoList(patientIdentifier);

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


    public boolean isValidUserLevelToken(String token) throws IOException {
        return SessionManager.isValidLevelToken(token, Configuration.USER_LEVEL)
                || SessionManager.isValidLevelToken(token, Configuration.ROOT_USER_LEVEL);
    }

    public boolean isValidRootLevelToken(String token) throws IOException {
        return SessionManager.isValidLevelToken(token, Configuration.ROOT_USER_LEVEL);
    }
}
