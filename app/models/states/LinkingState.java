package models.states;

import java.util.HashMap;
import java.util.Map;


public enum LinkingState {
    UNKNOWN(""), NOT_LINKED(null), DBPEDIA("dbpedia.org"), YAGO(""), INDIANA("inpho.cogs.indiana.edu");

    private final String                           source;

    private static final Map<String, LinkingState> lookup = new HashMap<String, LinkingState>();
    static {
        for(LinkingState d : LinkingState.values())
            lookup.put(d.getSource(), d);
    }


    private LinkingState(String source)
    {
        this.source = source;
    }


    public String getSource()
    {
        return source;
    }


    public static LinkingState get(String source)
    {
        if(lookup.containsKey(source)) { return lookup.get(source); }

        return LinkingState.UNKNOWN;
    }
}