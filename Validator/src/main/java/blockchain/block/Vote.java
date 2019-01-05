package blockchain.block;

import blockchain.interfaces.Raw;
import exception.BlockChainObjectParsingException;
import blockchain.utility.ByteArrayReader;
import general.utility.GeneralHelper;

import java.io.Serializable;
import java.util.Objects;

public class Vote implements Raw {

	private AuthorityInfo beneficiary;
	private boolean add;// true: add, false:remove
	private boolean agree;// true: agree, false:disagree

	public Vote(AuthorityInfo beneficiary, boolean add, boolean agree)
	{
		this.beneficiary=beneficiary;
		this.add = add;
		this.agree=agree;
	}

	public Vote()
	{
	}


	public AuthorityInfo getBeneficiary() {
		return beneficiary;
	}

	public boolean isAdd() {
		return add;
	}

	public boolean isAgree() {
		return agree;
	}

	public void setBeneficiary(AuthorityInfo beneficiary) {
		this.beneficiary = beneficiary;
	}

	public void setAdd(boolean add) {
		this.add = add;
	}

	public void setAgree(boolean agree) {
		this.agree = agree;
	}

	public byte[] getRaw()
	{
		byte[] voting= {(byte)(this.isAdd() ?1:0), (byte)(this.isAgree() ?1:0)};
		return GeneralHelper.mergeByteArrays(getBeneficiary().getRaw(), voting);
	}

	public static Vote parse(ByteArrayReader byteArrayReader) throws BlockChainObjectParsingException {
		Vote result = new Vote();
		result.setBeneficiary(AuthorityInfo.parse(byteArrayReader));
		result.setAdd(byteArrayReader.readByte()==1);
		result.setAgree(byteArrayReader.readByte()==1);

		return result;
	}

	public static Vote parse(byte[] raw) throws BlockChainObjectParsingException {

		ByteArrayReader byteArrayReader = new ByteArrayReader();
		byteArrayReader.set(raw);
		Vote vote =parse(byteArrayReader);

		if(!byteArrayReader.isFinished())
			throw new BlockChainObjectParsingException();
		return vote;
	}






	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Vote vote = (Vote) o;
		return isAdd() == vote.isAdd() &&
				isAgree() == vote.isAgree() &&
				Objects.equals(getBeneficiary(), vote.getBeneficiary());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getBeneficiary(), isAdd(), isAgree());
	}

}
