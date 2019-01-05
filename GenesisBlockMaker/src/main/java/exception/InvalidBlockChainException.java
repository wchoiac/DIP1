package exception;

import blockchain.block.Block;
import general.utility.GeneralHelper;

public class InvalidBlockChainException extends Exception {
    public InvalidBlockChainException(String peerAddress, Block block)
    {
        super(peerAddress+": Bad Blockchain received - block("+ GeneralHelper.bytesToStringHex(block.calculateHash())+")");
    }

    public InvalidBlockChainException(String peerAddress)
    {
        super(peerAddress+": Bad Blockchain received");
    }
}
