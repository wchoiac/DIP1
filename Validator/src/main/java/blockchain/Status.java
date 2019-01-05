package blockchain;

import blockchain.interfaces.Raw;
import exception.BlockChainObjectParsingException;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import general.utility.GeneralHelper;

import java.io.Serializable;

public class Status implements Raw{

    private int totalScore;
    private byte[] latestBlockHash;

    public Status(int totalScore, byte[] latestBlockHash)
    {
        this.setLatestBlockHash(latestBlockHash);
        this.setTotalScore(totalScore);
    }

    public Status()
    {
    }

    public byte[] getLatestBlockHash() {
        return latestBlockHash;
    }

    public void setLatestBlockHash(byte[] latestBlockHash) {
        this.latestBlockHash = latestBlockHash;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public byte[] getRaw()
    {
        return GeneralHelper.mergeByteArrays(GeneralHelper.intToBytes(totalScore),latestBlockHash);
    }

    public static Status parse(ByteArrayReader byteArrayReader){

        Status status = new Status();

        int totalScore = GeneralHelper.bytesToInt(byteArrayReader.readBytes(Integer.BYTES));

        status.setTotalScore(totalScore);
        status.setLatestBlockHash(byteArrayReader.readBytes( Configuration.HASH_LENGTH));

        return status;
    }

    public static Status parse(byte[] raw) throws BlockChainObjectParsingException {

        ByteArrayReader byteArrayReader = new ByteArrayReader();
        byteArrayReader.set(raw);
        Status status = parse(byteArrayReader);

        if(!byteArrayReader.isFinished())
            throw new BlockChainObjectParsingException();

        return status;
    }
}
