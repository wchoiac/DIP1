package blockchain.internal;

import blockchain.block.AuthorityInfo;
import blockchain.interfaces.Raw;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class Voting implements Raw{
	
	private AuthorityInfo beneficiary;
	private boolean add; // true:add, false:remove
	private SortedSet<byte[]> agree= new TreeSet<>(new GeneralHelper.byteArrayComparator());
	private SortedSet<byte[]> disagree= new TreeSet<>(new GeneralHelper.byteArrayComparator());


	public Voting(AuthorityInfo beneficiary, boolean add)
	{
		this.beneficiary=beneficiary;
		this.add = add;
	}

	public Voting()
	{

	}

	public void setBeneficiary(AuthorityInfo beneficiary) {
		this.beneficiary = beneficiary;
	}

	public void setAdd(boolean add) {
		this.add = add;
	}

	public void setAgree(SortedSet<byte[]> agree) {
		this.agree = agree;
	}

	public void setDisagree(SortedSet<byte[]> disagree) {
		this.disagree = disagree;
	}

	public SortedSet<byte[]> getAgree() {
		return agree;
	}

	public SortedSet<byte[]> getDisagree() {
		return disagree;
	}

	public boolean isExistingVoter(byte[] voter)
	{
		return agree.contains(voter)||disagree.contains(voter);
	}
	
	public void addAgree(byte[] voter)
	{
		agree.add(voter);
	}
	
	public void addDisagree(byte[] voter)
	{
		disagree.add(voter);
	}
	
	public AuthorityInfo getBeneficiary()
	{
		return beneficiary;
	}
	
	public boolean isAdd()
	{
		return add;
	}


	public int getNumAgree()
	{
		return agree.size();
	}

	public int getNumDisagree()
	{
		return disagree.size();
	}


	public byte[] getRaw()
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		try {
			byteArrayOutputStream.write(beneficiary.getRaw());
			byteArrayOutputStream.write(add?1:0);

			byteArrayOutputStream.write(agree.size());
			Iterator<byte[]> iterator = agree.iterator();
			while(iterator.hasNext()) {
				byte[] element = iterator.next();
				byteArrayOutputStream.write(element);
			}

			byteArrayOutputStream.write(disagree.size());
			iterator = disagree.iterator();
			while(iterator.hasNext()) {
				byte[] element = iterator.next();
				byteArrayOutputStream.write(element);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return byteArrayOutputStream.toByteArray();
	}


	public static Voting parse(ByteArrayReader byteArrayReader) throws BlockChainObjectParsingException {
		Voting voting = new Voting();

		voting.setBeneficiary(AuthorityInfo.parse(byteArrayReader));
		voting.setAdd(byteArrayReader.readByte()==1);

		int length = byteArrayReader.readInt();
		SortedSet<byte[]> agreeSet= new TreeSet<>(new GeneralHelper.byteArrayComparator());
		for(int i=0;i<length;++i)
		{
			agreeSet.add(byteArrayReader.readBytes(Configuration.IDENTIFIER_LENGTH));
		}
		voting.setAgree(agreeSet);

		length = byteArrayReader.readInt();
		SortedSet<byte[]> disagreeSet= new TreeSet<>(new GeneralHelper.byteArrayComparator());
		for(int i=0;i<length;++i)
		{
			disagreeSet.add(byteArrayReader.readBytes(Configuration.IDENTIFIER_LENGTH));
		}
		voting.setDisagree(disagreeSet);

		return voting;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Voting voting = (Voting) o;
		return add == voting.add &&
				Objects.equals(beneficiary, voting.beneficiary) &&
				Objects.equals(agree, voting.agree) &&
				Objects.equals(disagree, voting.disagree);
	}

	@Override
	public int hashCode() {
		return Objects.hash(beneficiary, add, agree, disagree);
	}
}
