package blockchain.block;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.*;

import blockchain.block.transaction.Transaction;
import blockchain.interfaces.Raw;
import blockchain.utility.ByteArrayReader;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;

public class Block implements Raw {

	private BlockHeader header;
	private BlockContent content;
	
	public Block(ECPrivateKey validatorPrivate, Vote vote, AuthorityInfo[] validators, int blockNumber, byte score, byte[] prevHash, ECPublicKey validatorPublicKey, MedicalOrgInfo[] medicalOrgAuthorizationList, byte[][] medicalOrgRevocationList, PatientInfo[] patientRegistrationList, Transaction[] transactions ) {

		this.setContent(new BlockContent(validators,medicalOrgAuthorizationList,medicalOrgRevocationList,patientRegistrationList,transactions));
		this.setHeader(new BlockHeader(content.getContentStructureIndicator(),
				validatorPrivate,vote,blockNumber,score,prevHash,validatorPublicKey, getContent().calculateHash()));
	}



	public Block()
	{
		this.setHeader(null);
		this.setContent(null);
	}

	public byte[] calculateHash()
	{
		return getHeader().calculateHash();
	}


	public byte[] getRaw(){
		return GeneralHelper.mergeByteArrays(header.getRaw(),content.getRaw());
	}

	public static Block parse(ByteArrayReader byteArrayReader) throws BlockChainObjectParsingException {
		Block block = new Block();
		block.setHeader(BlockHeader.parse(byteArrayReader));
		block.setContent(BlockContent.parse(block.getHeader().getStructureIndicator(),byteArrayReader));
		return block;
	}

	public static Block parse(byte[] raw) throws BlockChainObjectParsingException {
		ByteArrayReader byteArrayReader = new ByteArrayReader();
		byteArrayReader.set(raw);
		Block block = parse(byteArrayReader);

		if(!byteArrayReader.isFinished())
			throw new BlockChainObjectParsingException();

		return block;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Block block = (Block) o;
		return Objects.equals(getHeader(), block.getHeader()) &&
				Objects.equals(getContent(), block.getContent());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getHeader(), getContent());
	}

	public BlockHeader getHeader() {
		return header;
	}

	public void setHeader(BlockHeader header) {
		this.header = header;
	}

	public BlockContent getContent() {
		return content;
	}

	public void setContent(BlockContent content) {
		this.content = content;
	}
}
