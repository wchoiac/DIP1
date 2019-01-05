package blockchain.internal;

import blockchain.interfaces.Raw;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;
import java.util.Arrays;

public class ChainInfo implements Raw {
    private byte[] prevBlockHash;
    private boolean isBestChain;
    private byte[] nextBlockHash;

    public ChainInfo(byte[] prevBlockHash,boolean isBestChain,byte[] nextBlockHash)
    {
        this.setPrevBlockHash(prevBlockHash);
        this.setBestChain(isBestChain);
        this.setNextBlockHash(nextBlockHash);
    }

    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    public void setPrevBlockHash(byte[] prevBlockHash) {
        this.prevBlockHash = prevBlockHash;
    }

    public boolean isBestChain() {
        return isBestChain;
    }

    public void setBestChain(boolean bestChain) {
        isBestChain = bestChain;
    }

    public byte[] getNextBlockHash() {
        return nextBlockHash;
    }

    public void setNextBlockHash(byte[] nextBlockHash) {
        this.nextBlockHash = nextBlockHash;
    }


    public byte[] getRaw()
    {
        return GeneralHelper.mergeByteArrays(prevBlockHash,new byte[]{(byte)(isBestChain?1:0)},nextBlockHash);

    }

    public static ChainInfo parse(byte[] raw) throws BlockChainObjectParsingException {
        if(raw.length!= Configuration.HASH_LENGTH+1&&raw.length!=2*Configuration.HASH_LENGTH+1)
            throw new BlockChainObjectParsingException();
        byte[] prevBlockHash = Arrays.copyOfRange(raw, 0, Configuration.HASH_LENGTH);
        boolean isBestChain = raw[Configuration.HASH_LENGTH] == 1;
        byte[] nextBlockHash = isBestChain ? (raw.length == Configuration.HASH_LENGTH + 1 ?
                null : Arrays.copyOfRange(raw, Configuration.HASH_LENGTH + 1, raw.length)) : null;
        return new ChainInfo(prevBlockHash, isBestChain, nextBlockHash);
    }
}
