package blockchain.block;

import blockchain.interfaces.Raw;
import blockchain.utility.BlockChainSecurityHelper;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class BlockHeader implements Raw {


	private byte structureIndicator;
	//For voting
	private Vote vote; //variable length - due to name
	private int blockNumber; //4 bytes
	private byte[] contentHash; //32 bytes
	private byte[] prevHash; //32 bytes
	private long timestamp; //8 bytes
	private byte score; //1 byte
	private byte[] validatorIdentifier; //20 bytes
	private byte[] validatorSignature; //64 bytes

	public BlockHeader(byte contentStructureIndicator, ECPrivateKey validatorPrivate, Vote vote, int blockNumber, byte score, byte[] prevHash, ECPublicKey validatorPublicKey, byte[] contentHash){

		this.structureIndicator = vote==null?contentStructureIndicator:(byte)(contentStructureIndicator|(1<<Configuration.VOTE_BIT_POSITION));
		this.vote=vote;
		this.setBlockNumber(blockNumber);

		this.setPrevHash(prevHash);
		this.setTimestamp(System.currentTimeMillis());
		this.setScore(score);

		this.setContentHash(contentHash);

		if(blockNumber!=0) {
			this.setValidatorIdentifier(BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(validatorPublicKey));
			try {
				this.setValidatorSignature(SecurityHelper.createRawECDSASignatureWithContent(validatorPrivate, getSignatureCoverage()
						, Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		else
		{
			this.setValidatorIdentifier(new byte[Configuration.IDENTIFIER_LENGTH]);
			this.setValidatorSignature(new byte[Configuration.SIGNATURE_LENGTH]);
		}
	}

	public BlockHeader()
	{

	}

	public void setVote(Vote vote) {
		this.vote = vote;
	}

	public byte[] calculateHash() {

		byte[] hash =null;
		try {
			hash= SecurityHelper.hash(this.getRaw(), Configuration.BLOCKCHAIN_HASH_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return hash;
	}

	public byte getStructureIndicator() {
		return structureIndicator;
	}

	public void setStructureIndicator(byte structureIndicator) {
		this.structureIndicator = structureIndicator;
	}

	public Vote getVote() {
		return vote;
	}

	public int getBlockNumber() {
		return blockNumber;
	}

	public void setBlockNumber(int blockNumber) {
		this.blockNumber = blockNumber;
	}

	public byte[] getContentHash() {
		return contentHash;
	}

	public void setContentHash(byte[] contentHash) {
		this.contentHash = contentHash;
	}

	public byte[] getPrevHash() {
		return prevHash;
	}

	public void setPrevHash(byte[] prevHash) {
		this.prevHash = prevHash;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void changeTimestamp(long timestamp, ECPrivateKey validatorPrivate)
	{
		this.setTimestamp(timestamp);

		if(blockNumber!=0) {
			try {
				this.setValidatorSignature(SecurityHelper.createRawECDSASignatureWithContent(validatorPrivate, getSignatureCoverage()
						, Configuration.BLOCKCHAIN_HASH_ALGORITHM,Configuration.ELIPTIC_CURVE,Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
	}

	public byte getScore() {
		return score;
	}

	public void setScore(byte score) {
		this.score = score;
	}

	public byte[] getValidatorSignature() {
		return validatorSignature;
	}

	public void setValidatorSignature(byte[] validatorSignature) {
		this.validatorSignature = validatorSignature;
	}

	public byte[] getValidatorIdentifier() {
		return validatorIdentifier;
	}

	public void setValidatorIdentifier(byte[] validatorIdentifier) {
		this.validatorIdentifier = validatorIdentifier;}

	public static BlockHeader parse(ByteArrayReader byteArrayReader) throws BlockChainObjectParsingException {
		BlockHeader header = new BlockHeader();

		byte structureIndicator=byteArrayReader.readByte();
		header.setStructureIndicator(structureIndicator);
		if(GeneralHelper.isBitSet(structureIndicator,Configuration.VOTE_BIT_POSITION))
			header.setVote(Vote.parse(byteArrayReader));
		else
			header.setVote(null);
		header.setBlockNumber(GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES)));
		header.setContentHash(byteArrayReader.readBytes(Configuration.HASH_LENGTH));
		header.setPrevHash(byteArrayReader.readBytes(Configuration.HASH_LENGTH));
		header.setTimestamp(GeneralHelper.bytesToLong(byteArrayReader.readBytes(Long.BYTES)));
		header.setScore(byteArrayReader.readByte());
		header.setValidatorIdentifier(byteArrayReader.readBytes(Configuration.IDENTIFIER_LENGTH));
		header.setValidatorSignature(byteArrayReader.readBytes(Configuration.SIGNATURE_LENGTH));

		return header;
	}

	public static BlockHeader parse(byte[] raw) throws BlockChainObjectParsingException {

		ByteArrayReader byteArrayReader = new ByteArrayReader();
		byteArrayReader.set(raw);
		BlockHeader blockHeader = parse(byteArrayReader);

		if(!byteArrayReader.isFinished())
			throw new BlockChainObjectParsingException();

		return blockHeader;
	}

	public static BlockHeader[] parseArray(ByteArrayReader byteArrayReader) throws BlockChainObjectParsingException {

		ArrayList<BlockHeader> blockHeaders = new ArrayList<>();

		while(!byteArrayReader.isFinished()){
			blockHeaders.add(BlockHeader.parse(byteArrayReader));
		}

		return blockHeaders.toArray(new BlockHeader[0]);
	}

	public static BlockHeader[] parseArray(byte[] raw) throws BlockChainObjectParsingException {

		ByteArrayReader byteArrayReader = new ByteArrayReader();
		byteArrayReader.set(raw);
		BlockHeader[] blockHeader = parseArray(byteArrayReader);

		if(!byteArrayReader.isFinished())
			throw new BlockChainObjectParsingException();

		return blockHeader;
	}

	public byte[] getRaw()
	{
		return GeneralHelper.mergeByteArrays(getSignatureCoverage(), getValidatorIdentifier(), getValidatorSignature());
	}

	public byte[] getSignatureCoverage()
	{

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		try {
			byteArrayOutputStream.write(structureIndicator);
			if (getVote() != null) {
				byteArrayOutputStream.write(getVote().getRaw());
			}
			byteArrayOutputStream.write(GeneralHelper.intToBytes(getBlockNumber()));
			byteArrayOutputStream.write(getContentHash());
			byteArrayOutputStream.write(getPrevHash());
			byteArrayOutputStream.write(GeneralHelper.longToBytes(getTimestamp()));
			byteArrayOutputStream.write(getScore());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return byteArrayOutputStream.toByteArray();
	}



	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BlockHeader that = (BlockHeader) o;
		return getBlockNumber() == that.getBlockNumber() &&
				getTimestamp() == that.getTimestamp() &&
				getScore() == that.getScore() &&
				Objects.equals(getVote(), that.getVote()) &&
				Arrays.equals(getContentHash(), that.getContentHash()) &&
				Arrays.equals(getPrevHash(), that.getPrevHash()) &&
				Arrays.equals(getValidatorSignature(), that.getValidatorSignature()) &&
				Arrays.equals(getValidatorIdentifier(), that.getValidatorIdentifier());
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(getVote(), getBlockNumber(), getTimestamp(), getScore());
		result = 31 * result + Arrays.hashCode(getContentHash());
		result = 31 * result + Arrays.hashCode(getPrevHash());
		result = 31 * result + Arrays.hashCode(getValidatorSignature());
		result = 31 * result + Arrays.hashCode(getValidatorIdentifier());
		return result;
	}

}
