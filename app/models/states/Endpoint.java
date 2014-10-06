package models.states;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;

import settings.Settings;
import utils.StringUtils;


public class Endpoint
{
    public static Endpoint LOCAL   = new Endpoint(Settings.SERVER + "/openrdf-sesame/repositories/" + Settings.REPO);
    public static Endpoint DBPEDIA = new Endpoint("http://dbpedia.org/sparql");

    private URL            endpoint;


    public Endpoint(String destination)
    {
        this.endpoint = StringUtils.toURL(destination);
    }


    public String getDestination()
    {
        return endpoint.toExternalForm();
    }


    public static Endpoint valueOf(String destination)
    {
        Field[] fields = Endpoint.class.getDeclaredFields();

        for(Field f : fields) {
            if(Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                try {
                    if(f.isAccessible() && f.getName().toUpperCase().equals(destination.toUpperCase()) && (Endpoint) f.get(null) instanceof Endpoint) { return (Endpoint) f.get(null); }
                } catch(IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }
}