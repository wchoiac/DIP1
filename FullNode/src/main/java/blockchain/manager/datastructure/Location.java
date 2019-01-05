package blockchain.manager.datastructure;

public class Location {

    private byte[] targetIdentifier;
    private byte[] blockHash;

    public Location(byte[] blockHash, byte[] transactionHash)
    {
        this.setBlockHash(blockHash);
        this.setTargetIdentifier(transactionHash);
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(byte[] blockHash) {
        this.blockHash = blockHash;
    }

    public byte[] getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(byte[] targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }
}
