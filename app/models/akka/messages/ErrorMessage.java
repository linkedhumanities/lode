package models.akka.messages;

public class ErrorMessage
{
    private Integer   errorCode;
    private Exception error;


    public ErrorMessage(Integer errorCode, Exception error)
    {
        this.errorCode = errorCode;
        this.error = error;
    }


    public Integer getErrorCode()
    {
        return errorCode;
    }


    public Exception getError()
    {
        return error;
    }
}