package exception;

public class BlockChainObjectParsingException extends Exception {

    public BlockChainObjectParsingException()
    {
        super("The object couldn't be parsed.");
    }
}
