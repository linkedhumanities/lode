package sparql.update;

import java.util.List;
import java.util.Set;

import models.entity.EntityContainer;
import models.entity.PropertyContainer;
import models.entity.PropertyModel;
import models.entity.Value;
import models.states.Endpoint;
import sparql.Sparql;
import sparql.query.Content;
import utils.ResUtils;

import com.hp.hpl.jena.query.QuerySolution;


public class Delete
{
    public static void deleteSameAs(String id, String target)
    {
        Sparql.update("DELETE {?a ?b ?c} WHERE {" + id + " owl:sameAs ?c. ?a ?b ?c. filter (regex(str(?c), \"" + target + "\",\"i\"))}", Endpoint.LOCAL);
    }


    public static void deleteProperty(Value instance, Value property, Value value)
    {
        String command = "DELETE WHERE { " + instance.toSparql() + " " + property.toSparql() + " " + value.toSparql() + " }";
        Sparql.update(command.toString(), Endpoint.LOCAL);

        // update local container
        Content.show(ResUtils.createShortURI(instance.getValue()));

        // update external container
        if(Enhancer.getCache().containsKey(instance)) {
            EntityContainer entity = Enhancer.getCache().get(instance);
            PropertyContainer pc = new PropertyContainer(new PropertyModel(property.getValue()));

            String askCommand = command.replace("DELETE", "ASK").replace(instance.getValue(), entity.getURI().getValue());
            if(!Sparql.ask(askCommand, Endpoint.DBPEDIA)) { return; }

            if(value.getValue().length() > 0) {
                pc.addValue(value.getLanguage(), value);
            } else if(value.getValue().length() == 0) {
                String selectCommand = command.toString().replace("DELETE", "SELECT ?data ").replace(instance.getValue(), entity.getURI().getValue());
                List<QuerySolution> values = Sparql.select(selectCommand, Endpoint.DBPEDIA);
                for(QuerySolution data : values) {
                    Value propertyValue = new Value(data.get("?data").toString());
                    pc.addValue(propertyValue.getLanguage(), propertyValue);
                }
            }

            entity.addProperty(pc);
        }
    }


    public static void refreshCache(String localURI, String remoteURI, Value value)
    {
        Set<Value> rawData = null;
        String localShortURI = ResUtils.createShortURI(localURI);
        PropertyContainer cachedValues = Enhancer.getCache().get(localShortURI).getProperty(remoteURI);

        if(cachedValues == null || cachedValues.getLanguages().size() == 0) { return; }

        rawData = cachedValues.getValuesByLanguage(value.getLanguage());

        for(Value oValue : rawData) {
            if(oValue.equals(value)) {
                rawData.remove(oValue);
                break;
            }
        }

        if(rawData.size() == 0) {
            Enhancer.getCache().get(localShortURI).delProperty(cachedValues.getPropertyURI());
        }
    }
}