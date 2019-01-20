package blockchain.internal;

import blockchain.interfaces.Raw;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import general.utility.GeneralHelper;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class StateInfo implements Raw {
    private static final int EXPECTED_LENGTH = Configuration.HASH_LENGTH*2 + Integer.BYTES*2;
    private byte[] latestVotingListBlockHash;
    private byte[] latestAuthorityListBlockHash;
    private int totalScore;
    private int totalAuthorities;

    public StateInfo(byte[] latestVotingListBlockHash, byte[] latestAuthorityListBlockHash,int totalScore, int totalAuthorities)
    {
        this.setLatestVotingListBlockHash(latestVotingListBlockHash);
        this.setLatestAuthorityListBlockHash(latestAuthorityListBlockHash);
        this.setTotalScore(totalScore);
        this.setTotalAuthorities(totalAuthorities);
    }

    public StateInfo()
    {}

    public byte[] getLatestVotingListBlockHash() {
        return latestVotingListBlockHash;
    }

    public void setLatestVotingListBlockHash(byte[] latestVotingListBlockHash) {
        this.latestVotingListBlockHash = latestVotingListBlockHash;
    }

    public byte[] getLatestAuthorityListBlockHash() {
        return latestAuthorityListBlockHash;
    }

    public void setLatestAuthorityListBlockHash(byte[] latestAuthorityListBlockHash) {
        this.latestAuthorityListBlockHash = latestAuthorityListBlockHash;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getTotalAuthorities() {
        return totalAuthorities;
    }

    public void setTotalAuthorities(int totalAuthorities) {
        this.totalAuthorities = totalAuthorities;
    }

    public byte[] getRaw()
    {
        return GeneralHelper.mergeByteArrays(latestVotingListBlockHash,latestAuthorityListBlockHash,
                GeneralHelper.intToBytes(totalScore),GeneralHelper.intToBytes(totalAuthorities));

    }
    public static StateInfo parse(byte[] raw)
    {
        if(raw.length!=EXPECTED_LENGTH)
            return null;

        StateInfo stateInfo = new StateInfo();
        stateInfo.setLatestVotingListBlockHash(Arrays.copyOfRange(raw,0,Configuration.HASH_LENGTH));
        stateInfo.setLatestAuthorityListBlockHash(Arrays.copyOfRange(raw,Configuration.HASH_LENGTH,2*Configuration.HASH_LENGTH));
        stateInfo.setTotalScore(GeneralHelper.bytesToInt(Arrays.copyOfRange(raw,2*Configuration.HASH_LENGTH,2*Configuration.HASH_LENGTH+Integer.BYTES)));
        stateInfo.setTotalAuthorities(GeneralHelper.bytesToInt(Arrays.copyOfRange(raw,2*Configuration.HASH_LENGTH+Integer.BYTES,2*Configuration.HASH_LENGTH+2*Integer.BYTES)));

        return stateInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateInfo stateInfo = (StateInfo) o;
        return totalScore == stateInfo.totalScore &&
                totalAuthorities == stateInfo.totalAuthorities &&
                Arrays.equals(latestVotingListBlockHash, stateInfo.latestVotingListBlockHash) &&
                Arrays.equals(latestAuthorityListBlockHash, stateInfo.latestAuthorityListBlockHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(totalScore, totalAuthorities);
        result = 31 * result + Arrays.hashCode(latestVotingListBlockHash);
        result = 31 * result + Arrays.hashCode(latestAuthorityListBlockHash);
        return result;
    }
}
