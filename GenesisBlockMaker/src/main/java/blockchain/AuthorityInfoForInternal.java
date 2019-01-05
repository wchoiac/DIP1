package blockchain;

import blockchain.block.AuthorityInfo;

import java.util.Objects;

//index of an authority is the total number of authorities, before it gets added in.
public class AuthorityInfoForInternal{
    private AuthorityInfo authorityInfo;
    private int lastSignedBlockNumber; // if -1, it is at most current number -((current Authority number)/2 +1) from now
    private byte[] revokedBlock;
    public AuthorityInfoForInternal(AuthorityInfo info, int lastSignedBlockHeight, byte[] revokedBlock)
    {
        this.setAuthorityInfo(info);
        this.setLastSignedBlockNumber(lastSignedBlockHeight);
        this.setRevokedBlock(revokedBlock);
    }
    public boolean canSign(int currentBlockNumber, int totalAuthorities)
    {
        int validationInterval=(totalAuthorities/2)+1;
        return getLastSignedBlockNumber() ==-1 || currentBlockNumber- getLastSignedBlockNumber() >=validationInterval;
    }

    public static boolean isInOrder(int currentBlockNumber, int totalAuthorities, int index)
    {
        return (currentBlockNumber%totalAuthorities)==index;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (!(o instanceof AuthorityInfoForInternal)) {
            return false;
        }

        AuthorityInfoForInternal authorityInfoForInternal = (AuthorityInfoForInternal) o;
        return authorityInfoForInternal.getAuthorityInfo().equals(getAuthorityInfo());

    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getAuthorityInfo());

    }

    public AuthorityInfo getAuthorityInfo() {
        return authorityInfo;
    }

    public void setAuthorityInfo(AuthorityInfo authorityInfo) {
        this.authorityInfo = authorityInfo;
    }

    public int getLastSignedBlockNumber() {
        return lastSignedBlockNumber;
    }

    public void setLastSignedBlockNumber(int lastSignedBlockNumber) {
        this.lastSignedBlockNumber = lastSignedBlockNumber;
    }

    public byte[] getRevokedBlock() {
        return revokedBlock;
    }

    public void setRevokedBlock(byte[] revokedBlock) {
        this.revokedBlock = revokedBlock;
    }
}
