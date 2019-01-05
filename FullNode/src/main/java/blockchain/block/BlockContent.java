package blockchain.block;

import blockchain.block.transaction.Transaction;
import blockchain.interfaces.Raw;
import blockchain.utility.ByteArrayReader;
import blockchain.utility.RawTranslator;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class BlockContent implements Raw {
    private AuthorityInfo[] initialAuthorities; //for genesis block
    private MedicalOrgInfo[] medicalOrgAuthorizationList;
    private byte[][] medicalOrgRevocationList; // list of identifier
    private PatientInfo[] patientInfoList;
    private Transaction[] transactions;

    public BlockContent(AuthorityInfo[] validators, MedicalOrgInfo[] medicalOrgAuthorizationList, byte[][] medicalOrgRevocationList, PatientInfo[] patientInfoList, Transaction[] transactions) {

        this.setInitialAuthorities(validators);
        this.medicalOrgAuthorizationList=medicalOrgAuthorizationList;
        this.medicalOrgRevocationList=medicalOrgRevocationList;
        this.patientInfoList = patientInfoList;
        this.transactions = transactions;
    }

    public BlockContent()
    {


    }

    public byte[] getRaw()
    {

        byte[] zeroBytes = new byte[4];

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            if (getInitialAuthorities() != null) {
                byteArrayOutputStream.write(GeneralHelper.intToBytes(getInitialAuthorities().length));
                for (AuthorityInfo info : getInitialAuthorities())
                    byteArrayOutputStream.write(info.getRaw());
            }

            if (getMedicalOrgAuthorizationList() != null) {
                byteArrayOutputStream.write(GeneralHelper.intToBytes(getMedicalOrgAuthorizationList().length));
                for (MedicalOrgInfo info : getMedicalOrgAuthorizationList())
                    byteArrayOutputStream.write(info.getRaw());
            }

            if (getMedicalOrgRevocationList() != null) {
                byteArrayOutputStream.write(GeneralHelper.intToBytes(getMedicalOrgRevocationList().length));
                for (byte[] rovokedIdentifier : getMedicalOrgRevocationList())
                    byteArrayOutputStream.write(rovokedIdentifier);
            }

            if (getPatientInfoList() != null) {
                byteArrayOutputStream.write(GeneralHelper.intToBytes(getPatientInfoList().length));
                for (PatientInfo info : getPatientInfoList())
                    byteArrayOutputStream.write(info.getRaw());
            }

            if (getTransactions() != null) {
                byteArrayOutputStream.write(GeneralHelper.intToBytes(getTransactions().length));
                for (Transaction transaction : getTransactions())
                    byteArrayOutputStream.write(transaction.getRaw());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static BlockContent parse(byte structureIndicator, ByteArrayReader byteArrayReader) throws BlockChainObjectParsingException {

        BlockContent blockContent = new BlockContent();

        int length;
        ArrayList<AuthorityInfo> authorityInfos = new ArrayList<>();
        ArrayList<MedicalOrgInfo> medicalOrgInfosForAuthorization = new ArrayList<>();
        ArrayList<PatientInfo> patientInfos = new ArrayList<>();
        ArrayList<Transaction> transactions = new ArrayList<>();

        //initial authority list
        if(GeneralHelper.isBitSet(structureIndicator,Configuration.INITIAL_AUTHORITIES_BIT_POSITION)) {
            length = GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES));
            if (length < 0)
                throw new BlockChainObjectParsingException();
            for (int i = 0; i < length; ++i) {
                authorityInfos.add(AuthorityInfo.parse(byteArrayReader));
            }
            blockContent.setInitialAuthorities(authorityInfos.isEmpty() ? null : authorityInfos.toArray(new AuthorityInfo[0]));
            authorityInfos.clear();
        }

        //authorization list
        if(GeneralHelper.isBitSet(structureIndicator,Configuration.AUTHORIZATION_BIT_POSITION)) {
            length = GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES));
            if (length < 0)
                throw new BlockChainObjectParsingException();
            for (int i = 0; i < length; ++i) {
                medicalOrgInfosForAuthorization.add(MedicalOrgInfo.parse(byteArrayReader));
            }
            blockContent.setMedicalOrgAuthorizationList(medicalOrgInfosForAuthorization.isEmpty() ? null : medicalOrgInfosForAuthorization.toArray(new MedicalOrgInfo[0]));
            medicalOrgInfosForAuthorization.clear();
        }

        //revocation list
        if(GeneralHelper.isBitSet(structureIndicator,Configuration.REVOCATION_BIT_POSITION)) {
            length = GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES));
            if (length < 0)
                throw new BlockChainObjectParsingException();
            if (length > 0) {
                blockContent.setMedicalOrgRevocationList(RawTranslator.splitBytesToBytesArray(
                        byteArrayReader.readBytes(length * Configuration.IDENTIFIER_LENGTH), Configuration.IDENTIFIER_LENGTH));
            } else {
                blockContent.setMedicalOrgRevocationList(null);
            }
        }

        //patient registration list
        if(GeneralHelper.isBitSet(structureIndicator,Configuration.PATIENT_REGISTRATION_BIT_POSITION)) {
            length = GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES));
            if (length < 0)
                throw new BlockChainObjectParsingException();
            for (int i = 0; i < length; ++i) {
                patientInfos.add(PatientInfo.parse(byteArrayReader));
            }
            blockContent.setPatientInfoList(patientInfos.isEmpty() ? null : patientInfos.toArray(new PatientInfo[0]));
            patientInfos.clear();
        }

        //transactions
        if(GeneralHelper.isBitSet(structureIndicator,Configuration.TRANSACTION_BIT_POSITION)) {
            length = GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES));
            if (length < 0)
                throw new BlockChainObjectParsingException();
            for (int i = 0; i < length; ++i) {
                transactions.add(Transaction.parse(byteArrayReader));
            }
            blockContent.setTransactions(transactions.isEmpty() ? null : transactions.toArray(new Transaction[0]));
            transactions.clear();
        }

        return blockContent;
    }


    public byte[] calculateHash(){

        try {
            return SecurityHelper.hash(getRaw(), Configuration.BLOCKCHAIN_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;//not expected
    }
    public static BlockContent parse(byte structureIndicator, byte[] raw) throws BlockChainObjectParsingException {

        ByteArrayReader byteArrayReader = new ByteArrayReader();
        byteArrayReader.set(raw);
        BlockContent blockContent = parse(structureIndicator, byteArrayReader);

        if(!byteArrayReader.isFinished())
            throw new BlockChainObjectParsingException();

        return blockContent;
    }

    public byte getContentStructureIndicator()
    {
        byte contentStructureIndicator=0;

        if(initialAuthorities!=null)
            contentStructureIndicator=(byte)(contentStructureIndicator|(1<<Configuration.INITIAL_AUTHORITIES_BIT_POSITION));
        if(medicalOrgAuthorizationList!=null)
            contentStructureIndicator=(byte)(contentStructureIndicator|(1<<Configuration.AUTHORIZATION_BIT_POSITION));
        if(medicalOrgRevocationList!=null)
            contentStructureIndicator=(byte)(contentStructureIndicator|(1<<Configuration.REVOCATION_BIT_POSITION));
        if(patientInfoList !=null)
            contentStructureIndicator=(byte)(contentStructureIndicator|(1<<Configuration.PATIENT_REGISTRATION_BIT_POSITION));
        return contentStructureIndicator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockContent that = (BlockContent) o;
        return Arrays.equals(getInitialAuthorities(), that.getInitialAuthorities()) &&
                Arrays.equals(getMedicalOrgAuthorizationList(), that.getMedicalOrgAuthorizationList()) &&
                Arrays.equals(getMedicalOrgRevocationList(), that.getMedicalOrgRevocationList()) &&
                Arrays.equals(getPatientInfoList(), that.getPatientInfoList()) &&
                Arrays.equals(getTransactions(), that.getTransactions());
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(getInitialAuthorities());
        result = 31 * result + Arrays.hashCode(getMedicalOrgAuthorizationList());
        result = 31 * result + Arrays.hashCode(getMedicalOrgRevocationList());
        result = 31 * result + Arrays.hashCode(getPatientInfoList());
        result = 31 * result + Arrays.hashCode(getTransactions());
        return result;
    }

    public void setMedicalOrgAuthorizationList(MedicalOrgInfo[] medicalOrgAuthorizationList) {
        this.medicalOrgAuthorizationList = medicalOrgAuthorizationList;
    }


    public void setPatientInfoList(PatientInfo[] patientInfoList) {
        this.patientInfoList = patientInfoList;
    }

    public void setTransactions(Transaction[] transactions) {
        this.transactions = transactions;
    }

    public AuthorityInfo[] getInitialAuthorities() {
        return initialAuthorities;
    }

    public void setInitialAuthorities(AuthorityInfo[] initialAuthorities) {
        this.initialAuthorities = initialAuthorities;
    }

    public MedicalOrgInfo[] getMedicalOrgAuthorizationList() {
        return medicalOrgAuthorizationList;
    }


    public PatientInfo[] getPatientInfoList() {
        return patientInfoList;
    }

    public Transaction[] getTransactions() {
        return transactions;
    }

    public byte[][] getMedicalOrgRevocationList() {
        return medicalOrgRevocationList;
    }

    public void setMedicalOrgRevocationList(byte[][] medicalOrgRevocationList) {
        this.medicalOrgRevocationList = medicalOrgRevocationList;
    }

}
