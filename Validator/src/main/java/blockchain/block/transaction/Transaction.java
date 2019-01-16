package blockchain.block.transaction;

import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;
import general.security.SecurityHelper;

import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Objects;

public class Transaction{

	private long timestamp;
	//<= record size (4 byte) for raw
	private byte[] encryptedRecord;
	private byte[] medicalOrgIdentifier;
	private byte[] patientIdentifier;
	private byte[] medicalOrgSignature;
	private byte[] patientSignature;

//simply check the whole transaction private long nonce;

//content to be specified in detail

	public Transaction(byte[] medicalOrgIdentifier, long timestamp, byte[] encryptedRecord, byte[] patientSignature, byte[] patientIdentifier, ECPrivateKey medicalOrgPrivateKey)
	{
		this.timestamp = timestamp;
		this.encryptedRecord = encryptedRecord;
		this.medicalOrgIdentifier=medicalOrgIdentifier;
		this.patientIdentifier=patientIdentifier;
		this.medicalOrgSignature=sign(medicalOrgPrivateKey);
		this.patientSignature=patientSignature;
	}

	public Transaction(byte[] raw)
	{

	}

	public Transaction(){

	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setEncryptedRecord(byte[] encryptedRecord) {
		this.encryptedRecord = encryptedRecord;
	}

	public void setMedicalOrgIdentifier(byte[] medicalOrgIdentifier) {
		this.medicalOrgIdentifier = medicalOrgIdentifier;
	}

	public void setPatientIdentifier(byte[] patientIdentifier) {
		this.patientIdentifier = patientIdentifier;
	}

	public void setMedicalOrgSignature(byte[] medicalOrgSignature) {
		this.medicalOrgSignature = medicalOrgSignature;
	}

	public void setPatientSignature(byte[] patientSignature) {
		this.patientSignature = patientSignature;
	}


	private byte[] sign(ECPrivateKey medicalOrgPrivateKey)
	{
		try {
			return SecurityHelper.createRawECDSASignatureWithContent(medicalOrgPrivateKey,getMedicalOrgSignatureCoverage(),Configuration
					.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE, Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null; //not expected
	}


	// validity for hospital key has to be checked outside(in FullNode class) and uniqueness of record is check outside as well.
	public boolean verify(ECPublicKey patientPublicKey, ECPublicKey medicalOrgPublicKey)
	{

		try {
			if(!SecurityHelper.verifyRawECDSASignatureWithContent( patientPublicKey, getPatientSignatureCoverage(),getPatientSignature(),Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE))
				return false;

            return SecurityHelper.verifyRawECDSASignatureWithContent(medicalOrgPublicKey, getMedicalOrgSignatureCoverage(), getMedicalOrgSignature(), Configuration.BLOCKCHAIN_HASH_ALGORITHM, Configuration.ELIPTIC_CURVE);

        } catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return false;
	}

	private byte[] getMedicalOrgSignatureCoverage()
	{
		return GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(getTimestamp()), getEncryptedRecord(),getPatientIdentifier());
	}

	private byte[] getPatientSignatureCoverage()
	{
		return GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(getTimestamp()), getEncryptedRecord(),getMedicalOrgIdentifier());
	}

	public byte[] getRaw()
	{
		return GeneralHelper.mergeByteArrays(GeneralHelper.longToBytes(getTimestamp()), GeneralHelper.intToBytes(getEncryptedRecord().length)
				,getEncryptedRecord(), getMedicalOrgIdentifier(), getPatientIdentifier(), getMedicalOrgSignature()
				,getPatientSignature());
	}
	public static Transaction parse(ByteArrayReader byteArrayReader){
		Transaction transaction = new Transaction();

		transaction.setTimestamp(GeneralHelper.bytesToLong(byteArrayReader.readBytes(Long.BYTES)));

		int encryptedRecord = GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES));
		transaction.setEncryptedRecord(byteArrayReader.readBytes(encryptedRecord));
		transaction.setMedicalOrgIdentifier(byteArrayReader.readBytes(Configuration.IDENTIFIER_LENGTH));
		transaction.setPatientIdentifier(byteArrayReader.readBytes(Configuration.IDENTIFIER_LENGTH));
		transaction.setMedicalOrgSignature(byteArrayReader.readBytes(Configuration.SIGNATURE_LENGTH));
		transaction.setPatientSignature(byteArrayReader.readBytes(Configuration.SIGNATURE_LENGTH));

		return transaction;
	}

	public static Transaction parse(byte[] raw) throws BlockChainObjectParsingException {
		ByteArrayReader byteArrayReader = new ByteArrayReader();
		byteArrayReader.set(raw);
		Transaction transaction = parse(byteArrayReader);

		if(!byteArrayReader.isFinished())
			throw new BlockChainObjectParsingException();

		return transaction;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Transaction that = (Transaction) o;
		return getTimestamp() == that.getTimestamp() &&
				Arrays.equals(getMedicalOrgIdentifier(), that.getMedicalOrgIdentifier()) &&
				Arrays.equals(getEncryptedRecord(), that.getEncryptedRecord()) &&
				Arrays.equals(getPatientSignature(), that.getPatientSignature()) &&
				Arrays.equals(getPatientIdentifier(), that.getPatientIdentifier()) &&
				Arrays.equals(getMedicalOrgSignature(), that.getMedicalOrgSignature());
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(getTimestamp());
		result = 31 * result + Arrays.hashCode(getMedicalOrgIdentifier());
		result = 31 * result + Arrays.hashCode(getEncryptedRecord());
		result = 31 * result + Arrays.hashCode(getPatientSignature());
		result = 31 * result + Arrays.hashCode(getPatientIdentifier());
		result = 31 * result + Arrays.hashCode(getMedicalOrgSignature());
		return result;
	}

	public byte[] calculateHash()
	{
		try {
			return SecurityHelper.hash(getRaw(),Configuration.BLOCKCHAIN_HASH_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null; // not expected
	}

	public byte[] getMedicalOrgIdentifier() {
		return medicalOrgIdentifier;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public byte[] getEncryptedRecord() {
		return encryptedRecord;
	}

	public byte[] getPatientSignature() {
		return patientSignature;
	}

	public byte[] getPatientIdentifier() {
		return patientIdentifier;
	}

	public byte[] getMedicalOrgSignature() {
		return medicalOrgSignature;
	}
}
