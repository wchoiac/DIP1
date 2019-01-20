package blockchain.block;

import general.utility.GeneralHelper;

import java.io.Serializable;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class Voting implements Serializable{
	
	private final AuthorityInfo beneficiary;
	private final boolean add; // true:add, false:remove
	private SortedSet<byte[]> agree= new TreeSet<>(new GeneralHelper.byteArrayComparator()); // maybe change to authority Info
	private SortedSet<byte[]> disagree= new TreeSet<>(new GeneralHelper.byteArrayComparator());

	public SortedSet<byte[]> getAgree() {
		return agree;
	}

	public SortedSet<byte[]> getDisagree() {
		return disagree;
	}

	public Voting(AuthorityInfo beneficiary, boolean add)
	{
		this.beneficiary=beneficiary;
		this.add = add;
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

	public int getNumAgree()
	{
		return agree.size();
	}
	
	public int getNumDisagree()
	{
		return disagree.size();
	}

}
