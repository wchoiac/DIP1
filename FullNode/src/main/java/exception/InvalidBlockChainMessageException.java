package exception;

public class InvalidBlockChainMessageException extends Exception {

    public InvalidBlockChainMessageException(String address, int messageNumber)
    {
        super(address+": Invalid message "+messageNumber+" received");
    }
}
