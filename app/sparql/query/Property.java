package sparql.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import models.states.BasicProperties;
import models.states.Endpoint;
import sparql.Sparql;
import utils.ResUtils;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class Property
{
    public static Map<String, Map<String, Map<String, String>>> allProperties() throws QueryExceptionHTTP
    {
        List<QuerySolution> annotation = Sparql.select("SELECT * WHERE { ?subject ?predicate owl:AnnotationProperty }", Endpoint.LOCAL);
        List<QuerySolution> data = Sparql.select("SELECT * WHERE { ?subject ?predicate owl:DataProperty }", Endpoint.LOCAL);
        List<QuerySolution> object = Sparql.select("SELECT * WHERE { ?subject ?predicate owl:ObjectProperty }", Endpoint.LOCAL);
        List<QuerySolution> unknown = Sparql.select(
                "SELECT DISTINCT ?predicate WHERE {?subject ?predicate ?object . MINUS { ?predicate ?stufe owl:AnnotationProperty .} . MINUS { ?predicate ?stufe owl:DataProperty .} . MINUS { ?predicate ?stufe owl:ObjectProperty .} . MINUS { ?tmp ?predicate owl:AnnotationProperty .} }", Endpoint.LOCAL);

        Map<Resource, HashSet<Resource>> aMap = Property.storeData(annotation);
        Map<Resource, HashSet<Resource>> dMap = Property.storeData(data);
        Map<Resource, HashSet<Resource>> oMap = Property.storeData(object);
        Map<Resource, HashSet<Resource>> uMap = Property.storeData(unknown);

        Map<String, Map<String, String>> aData = prepareData(aMap);
        Map<String, Map<String, String>> dData = prepareData(dMap);
        Map<String, Map<String, String>> oData = prepareData(oMap);
        Map<String, Map<String, String>> uData = prepareData(uMap);

        Map<String, Map<String, Map<String, String>>> result = new HashMap<>();
        result.put("Annotation Properties", aData);
        result.put("Data Properties", dData);
        result.put("Object Properties", oData);
        result.put("Unknown Properties", uData);

        return result;
    }


    private static Map<Resource, HashSet<Resource>> storeData(List<QuerySolution> data)
    {
        Map<Resource, HashSet<Resource>> map = new HashMap<Resource, HashSet<Resource>>();

        for(QuerySolution entry : data) {
            Resource sub = entry.getResource("?subject");
            Resource pre = entry.getResource("?predicate");
            if(map.containsKey(pre)) {
                map.get(pre).add(sub);
            } else {
                map.put(pre, new HashSet<Resource>());
                map.get(pre).add(sub);
            }
        }

        return map;
    }


    private static Map<String, Map<String, String>> prepareData(Map<Resource, HashSet<Resource>> map)
    {
        Map<String, Map<String, String>> result = new HashMap<>();

        if(map.isEmpty()) { return result; }

        for(Resource res : map.keySet()) {
            result.put(res.getURI(), ResUtils.getResInfoMap(res));
            for(Resource data : map.get(res)) {
                if(data != null)
                    result.put(data.getURI(), ResUtils.getResInfoMap(data));
            }
        }

        return result;
    }


    public static Map<String, String> getBasicInformation(String property) throws QueryExceptionHTTP
    {
        Map<String, String> info = new HashMap<>();
        String shortProp = ResUtils.createShortURI(property);
        info.put("uri", property);
        info.put("abbr", shortProp);
        info.put("domain", getInfo(BasicProperties.DOMAIN.getShortVersion(), shortProp));
        info.put("range", getInfo(BasicProperties.RANGE.getShortVersion(), shortProp));
        info.put("type", getInfo(BasicProperties.TYPE.getShortVersion(), shortProp));
        info.put("count", getCount(shortProp));
        return info;
    }


    public static String getInfo(String type, String shortProperty) throws QueryExceptionHTTP
    {
        String res = "-";

        // property = "db-ont:birthPlace";
        // System.out.println(System.lineSeparator() + type + " " + property);

        List<QuerySolution> qr = Sparql.select("SELECT DISTINCT ?x WHERE {" + shortProperty + " " + type + " " + "?x.} GROUP BY ?x", Endpoint.LOCAL);

        List<String> temp = new ArrayList<>();
        for(QuerySolution qs : qr) {
            if(qs.getResource("?x") != null) {
                String qsString = qs.getResource("?x").getURI().toString();
                System.out.println("\t" + qsString);
                temp.add(qsString);
            }
        }

        if(temp.size() > 0) {
            res = "";
            for(String s : temp) {
                res += ResUtils.createShortURI(s) + " ";
            }
        }

        return res;
    }


    public static List<String[]> getTriples(String property) throws QueryExceptionHTTP
    {
        String shortProperty = ResUtils.createShortURI(property);
        List<String[]> result = new ArrayList<>();
        List<QuerySolution> qr = Sparql.select("SELECT ?a ?c WHERE { ?a " + shortProperty + " ?c.} ORDER BY ?a LIMIT 30", Endpoint.LOCAL);
        for(QuerySolution qs : qr) {
            try {
                String a = qs.getResource("?a").toString();
                a = ResUtils.createShortURI(a);

                String c = "";
                if(qs.get("?c") instanceof Resource) {
                    c = qs.getResource("?c").toString();

                    c = ResUtils.createShortURI(c);

                } else if(qs.get("?c") instanceof Literal) {
                    c = qs.getLiteral("?c").toString();
                    if(c.contains("^^h")) {
                        c = c.substring(0, c.lastIndexOf("^^h"));
                    }
                }
                String[] res = new String[] { a, shortProperty, c };
                result.add(res);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    public static String getCount(String shortProperty) throws QueryExceptionHTTP
    {
        List<QuerySolution> count = Sparql.select("SELECT (COUNT(*) AS ?total) WHERE { ?a " + shortProperty + " ?c.}", Endpoint.LOCAL);
        int total = 0;
        for(QuerySolution qs : count) {
            String totalString = qs.get("?total").toString();
            total = Math.max(total, Integer.valueOf(totalString.substring(0, totalString.lastIndexOf("^") - 1)));
        }

        return String.valueOf(total);
    }
}