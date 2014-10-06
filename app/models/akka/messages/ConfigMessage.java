package models.akka.messages;

public class ConfigMessage
{
    private String object;
    private String destination;
    private int    method;


    public ConfigMessage(String object, String destination, int method)
    {
        this.object = object;
        this.destination = destination;
        this.method = method;
    }


    public String getObject()
    {
        return object;
    }


    public String getDestination()
    {
        return destination;
    }


    public int getMethod()
    {
        return method;
    }
}