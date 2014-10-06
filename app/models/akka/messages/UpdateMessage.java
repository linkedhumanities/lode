package models.akka.messages;

public class UpdateMessage
{
    private String  step;
    private Integer progress;
    private String  description;


    public UpdateMessage(String step, Integer progress, String description)
    {
        this.step = step;
        this.progress = progress;
        this.description = description;
    }


    public String getStep()
    {
        return step;
    }


    public Integer getProgress()
    {
        return progress;
    }


    public String getDescription()
    {
        return description;
    }


    @Override
    public String toString()
    {
        return step + "#!#" + progress + "#!#" + description;
    }
}