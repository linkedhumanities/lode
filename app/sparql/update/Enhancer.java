package sparql.update;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeSet;

import models.entity.EntityContainer;
import models.entity.PropertyContainer;
import models.entity.Value;
import models.states.Endpoint;
import models.states.LinkingState;
import settings.Settings;
import sparql.Sparql;
import sparql.query.Content;
import utils.ResUtils;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class Enhancer
{
    private static Map<String, EntityContainer> cache = new HashMap<>();


    public static EntityContainer enhance(String name, String target) throws QueryExceptionHTTP
    {
        // prepare search term
        EntityContainer entry = Content.getEntityContainerbyID(name);
        if(entry == null) { return null; }
        String searchterm = entry.getURI().getValue();

        // cache
        if(cache.containsKey(ResUtils.createShortURI(searchterm))) { return cache.get(ResUtils.createShortURI(searchterm)); }

        // loading sameAs relation
        List<QuerySolution> relation = Sparql.select("SELECT ?c {" + entry.getURI().toSparql() + " owl:sameAs ?c}", Endpoint.LOCAL);

        List<QuerySolution> data = new LinkedList<>();
        for(QuerySolution qs : relation) {
            if(qs.get("?c").toString().toLowerCase().contains(target.toLowerCase())) {
                data.add(qs);
            }
        }

        if(data.size() != 1) { return new EntityContainer(ResUtils.createResource("Error! There is not exactly one sameAs relation to " + target + ".")); }
        Value instance = new Value(data.get(0).getResource("?c").toString());

        // grep each property including all values for the current instance
        NavigableMap<String, PropertyContainer> tmpProperties = ResUtils.storeProperties(instance, Settings.LANG, searchterm);

        // load description
        String description = ResUtils.getDescription(new TreeSet<>(tmpProperties.values()));

        // result container
        EntityContainer result = new EntityContainer(instance, tmpProperties, LinkingState.UNKNOWN, description);
        cache.put(ResUtils.createShortURI(searchterm), result);

        return result;
    }


    public static void insertData(Value instance, Value property, Value value) throws QueryExceptionHTTP
    {
        // update sesame store
        String command = "INSERT DATA { " + instance.toSparql() + " " + property.toSparql() + " " + value.toSparql() + " }";
        Sparql.update(command, Endpoint.LOCAL);

        // clean up cache
        Delete.refreshCache(instance.getValue(), property.getValue(), value);

        // update variable
        // Content.show(ResUtils.createShortURI(instance.getValue()));
    }


    public static Map<String, EntityContainer> getCache()
    {
        return cache;
    }
}