package exception;

public class Unsuccessful extends Exception {

    public Unsuccessful(byte status){
        super(""+status);

    }

}
