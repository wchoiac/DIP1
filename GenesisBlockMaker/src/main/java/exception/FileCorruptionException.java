package exception;

public class FileCorruptionException extends Exception{

    public FileCorruptionException()
    {
        super("Files have been corrupted, please remove all the blockchain data and resync the blockchain.");
    }
}
