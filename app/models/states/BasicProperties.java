package models.states;

public enum BasicProperties {
    RANGE("rdfs:range"), DOMAIN("rdfs:domain"), TYPE("rdf:type"), SUBCLASS("rdfs:subClassOf"), SUBPROP("rdfs:subPropertyOf"), LABEL("rdfs:label"), COMMENT("rdfs:comment");

    String shortVersion;


    private BasicProperties(String shortVersion)
    {
        this.shortVersion = shortVersion;
    }


    public String getShortVersion()
    {
        return this.shortVersion;
    }
}