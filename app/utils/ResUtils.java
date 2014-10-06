package utils;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import models.entity.EntityContainer;
import models.entity.PropertyContainer;
import models.entity.PropertyModel;
import models.entity.Value;
import models.states.Endpoint;
import models.states.LinkingState;
import settings.Settings;
import sparql.Namespace;
import sparql.Sparql;
import sparql.query.Content;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class ResUtils
{
    public static NavigableMap<String, PropertyContainer> storeProperties(Value instance, List<String> disLang, String match) throws QueryExceptionHTTP
    {
        NavigableMap<String, PropertyContainer> result = new TreeMap<>();

        // load properties of the instance
        String selectCommand = "SELECT ?property ?value { " + instance.toSparql() + " ?property ?value . MINUS{ " + instance.toSparql() + " owl:sameAs ?value } }";
        List<QuerySolution> dataSet = Sparql.select(selectCommand, Endpoint.DBPEDIA);

        EntityContainer local = null;
        if(match != null) {
            local = Content.show(ResUtils.createShortURI(match));
        }

        for(QuerySolution data : dataSet) {
            if(data == null || data.get("?value") == null) {
                continue;
            }

            Value property = new Value(data.getResource("?property").getURI());
            Value value = new Value(data.get("?value").toString());
            PropertyContainer pc = new PropertyContainer(new PropertyModel(property.getValue()));

            if(result.containsKey(pc.getPropertyURI())) {
                pc = result.get(pc.getPropertyURI());
            }

            if(disLang.isEmpty() || disLang.contains(value.getLanguage()) || value.getLanguage().length() == 0) {
                if(local != null && local.getProperty(property.getValue()) != null && local.getProperty(property.getValue()).getValuesByLanguage(value.getLanguage()) != null && local.getProperty(property.getValue()).getValuesByLanguage(value.getLanguage()).contains(value)) {
                    continue;
                }
                pc.addValue(value.getLanguage(), value);
            }

            if(pc.getValues().size() > 0) {
                result.put(pc.getPropertyURI(), pc);
            }
        }

        return result;
    }


    public static NavigableMap<String, PropertyContainer> getTopProperties(NavigableSet<PropertyContainer> properties, Integer nOp, Integer nOv)
    {
        NavigableMap<String, PropertyContainer> result = new TreeMap<>();

        // sort properties by frequency desc
        List<PropertyContainer> sortedProperties = new ArrayList<>(properties.descendingSet());

        // counter
        int numRes = nOp;
        int numLit = nOp;

        // store the most frequent properties
        for(int i = 0; (0 < numLit && i < sortedProperties.size()) || (0 < numRes && i < sortedProperties.size()); i++) {
            PropertyContainer pair = sortedProperties.get(i);
            Map<String, TreeSet<Value>> values = new LinkedHashMap<>();

            // counter for number of values
            Integer counter = new Integer(0);

            if(pair.getPropertyURI().toString().toLowerCase().endsWith("type")) {
                numRes++;
            }

            // build language list according to the priority
            Set<String> priority = new TreeSet<String>(pair.getLanguages());

            // select nOv values
            String type = new String();
            for(String lang : priority) {
                for(Value value : pair.getValuesByLanguage(lang)) {
                    if(counter == nOv) {
                        break;
                    }

                    if(!values.containsKey(lang)) {
                        values.put(lang, new TreeSet<Value>());
                    }

                    type = value.getPropertyTyp();
                    values.get(lang).add(value);
                    counter++;
                }
            }

            // store result
            pair.setValues(values);

            if(type.equals("dataproperty") && numLit > 0) {
                result.put(pair.getPropertyURI(), pair);
                numLit--;
            } else if(type.equals("objectproperty") && numRes > 0) {
                result.put(pair.getPropertyURI(), pair);
                numRes--;
            }
        }

        return result;
    }


    public static String getDescription(TreeSet<PropertyContainer> properties)
    {
        String result = new String();

        for(PropertyContainer pc : properties) {
            if(Settings.L_DESCRIPTION.contains(pc.getPropertyURI())) {
                Set<Value> values = pc.getValuesByLanguage(Settings.LANG.get(0));
                Set<String> data = new HashSet<>();
                for(Value v : values) {
                    data.add(v.getValue());
                }
                if(data.size() > 0) { return (String) data.toArray()[0]; }
            }
        }

        return result;
    }


    public static Set<String> getLocalNameSet(Set<Value> uris)
    {
        Set<String> result = new HashSet<>();

        for(Value uri : uris) {
            if(uri.isResource()) {
                result.add(getLocaleName(uri.getValue()));
            } else {
                result.add(uri.getValue());
            }
        }

        return result;
    }


    public static String getLocaleName(String uri)
    {
        if(uri.contains("/")) {
            uri = uri.substring(uri.lastIndexOf("/") + 1);
        }
        if(uri.contains("#")) {
            uri = uri.substring(uri.lastIndexOf("#") + 1);
        }

        uri = StringUtils.decode(uri);

        return uri;
    }


    public static Resource createResource(String data)
    {
        Model model = ModelFactory.createDefaultModel();
        return model.createResource(data);
    }


    public static LinkingState getState(RDFNode data)
    {
        if(data.toString().toLowerCase().contains("dbpedia")) {
            return LinkingState.DBPEDIA;
        } else {
            return LinkingState.UNKNOWN;
        }
    }


    public static List<String> createShortURI(List<String> uri)
    {
        List<String> uris = new ArrayList<>();
        for(String u : uri) {
            uris.add(createShortURI(u));
        }
        return uris;
    }


    public static String createShortURI(String res)
    {
        String localname = ResUtils.getLocaleName(res.toString());
        String prefix = new String("---");

        try {
            prefix = Namespace.getPrefix(StringUtils.decode(res.toString()).replace(localname, "")).toString();
        } catch(NullPointerException e) {
            // prefix not available
        }

        localname = StringUtils.decode(localname);
        localname = StringUtils.deleteSC(localname);

        return prefix + ":" + localname;
    }


    public static String toLongURI(String shortURI)
    {
        try {
            String prefix = shortURI.substring(0, shortURI.indexOf(":"));
            String data = shortURI.substring(shortURI.indexOf(":") + 1);

            String namespace = Namespace.getNamespace(prefix);

            return namespace + "" + data;
        } catch(Exception e) {
            return null;
        }
    }


    public static Value getRedirectURI(Value value)
    {
        String selectCommand = "SELECT ?value WHERE { " + value.toSparql() + " <http://dbpedia.org/ontology/wikiPageRedirects> ?value}";
        List<QuerySolution> data = Sparql.select(selectCommand, Endpoint.DBPEDIA);

        if(data.size() > 0 && data.get(0) != null && data.get(0).get("?value") != null) { return new Value(data.get(0).get("?value").toString()); }

        return value;
    }


    public static List<Value> getDisambiguates(Value instance)
    {
        List<Value> result = new LinkedList<>();

        String selectCommand = "SELECT ?value WHERE { " + instance.toSparql() + " <http://dbpedia.org/ontology/wikiPageDisambiguates> ?value}";
        List<QuerySolution> data = Sparql.select(selectCommand, Endpoint.DBPEDIA);

        for(QuerySolution qs : data) {
            if(qs == null || qs.get("?value") == null) {
                continue;
            }

            result.add(new Value(qs.get("?value").toString()));
        }

        return result;
    }


    public static boolean isURI(String value)
    {
        try {
            value = URLEncoder.encode(value, "UTF-8");
            URI.create(value);
        } catch(Exception e) {
            return false;
        }

        return value.startsWith("http");
    }


    public static Map<String, String> getResInfoMap(Resource res)
    {
        Map<String, String> resMap = new HashMap<>();

        String uri = res.getURI();
        String namespace = res.getNameSpace();
        String prefix = Namespace.getPrefix(res.getNameSpace());
        String localName = res.getLocalName();

        resMap.put("uri", uri);
        resMap.put("namespace", namespace);
        resMap.put("prefix", prefix);
        resMap.put("localName", localName);

        return resMap;
    }
}