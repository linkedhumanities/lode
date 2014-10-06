package models.akka.messages;

import java.util.List;

import models.entity.EntityContainer;


public class ResultMessage
{
    String                source;
    EntityContainer       desc;
    List<EntityContainer> suggestions;
    Integer               method;


    public ResultMessage(String source, EntityContainer desc, List<EntityContainer> suggestions, int method)
    {
        this.source = source;
        this.desc = desc;
        this.suggestions = suggestions;
        this.method = method;
    }


    public String getSource()
    {
        return source;
    }


    public EntityContainer getDesc()
    {
        return desc;
    }


    public List<EntityContainer> getSuggestions()
    {
        return suggestions;
    }


    public Integer getMethod()
    {
        return method;
    }

}