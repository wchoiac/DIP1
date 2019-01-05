package rest.server.exception;

public class BadRequest extends Exception {


    public BadRequest()
    {

    }

    public BadRequest(String message)
    {
        super(message);
    }
}
