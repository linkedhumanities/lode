package sparql.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.states.Endpoint;
import sparql.Namespace;
import sparql.Sparql;
import utils.MapUtils;
import utils.ResUtils;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class Concept
{
    private static Map<String, String> uri;


    public static List<String> getProperties(String concept) throws QueryExceptionHTTP
    {
        List<String> conceptProperties = new ArrayList<String>();
        concept = Concept.getURIbyID(concept);

        Set<Resource> cProp = new HashSet<Resource>();
        if(concept.equals("http://www.w3.org/2002/07/owl#Thing")) { return conceptProperties; }

        List<QuerySolution> instances = Sparql.select("SELECT DISTINCT ?subject WHERE { ?subject a <" + concept + "> . }", Endpoint.LOCAL);

        Set<String> noInterest = new HashSet<String>();
        List<QuerySolution> subClasses = Sparql.select("SELECT * { ?subject rdfs:subClassOf <" + concept + "> . }", Endpoint.LOCAL);
        searchInstanceInSubClasses(noInterest, subClasses);

        for(QuerySolution qs : instances) {
            if(noInterest.contains(qs.getResource("?subject").toString())) {
                continue;
            }

            List<QuerySolution> properties = Sparql.select("SELECT DISTINCT ?predicate WHERE { <" + qs.getResource("?subject").toString() + ">  ?predicate ?object . }", Endpoint.LOCAL);
            for(QuerySolution property : properties) {
                cProp.add(property.getResource("?predicate"));
            }
        }

        for(Resource res : cProp) {
            conceptProperties.add(Namespace.getPrefix(res.getNameSpace()) + ":" + res.getLocalName());
        }

        return conceptProperties;
    }


    /**
     * 
     * @param keyword
     *            Search term from explorer
     * @param nonlinked
     * @return Map with key resource and value number of entities with this concept.
     * @throws QueryExceptionHTTP
     */
    public static Map<Resource, Integer> searchConcepts(String keyword, List<String> activeFilter, boolean nonlinked) throws QueryExceptionHTTP
    {
        Map<Resource, Integer> result = new LinkedHashMap<>();
        List<QuerySolution> data = new LinkedList<QuerySolution>();

        String countOnePerClusterFilter = "FILTER (NOT EXISTS {?s owl:sameAs ?s2. FILTER (STR(?s2) < STR(?s))}). ";

        String nonlinkedFilter = "";
        if(nonlinked) {
            nonlinkedFilter = "FILTER (!regex(str(?s),\"http://dbpedia.org/resource\",\"i\") " + "&& NOT EXISTS {?s owl:sameAs ?s2. FILTER (STR(?s2) < STR(?s) " + "&& regex(str(?s2),\"http://dbpedia.org/resource\",\"i\"))}) ";
        }
        if(keyword == null || keyword.length() < 3) {
            data.addAll(Sparql.select("SELECT ?concept (Count(?concept) AS ?count) { SELECT DISTINCT ?concept ?s WHERE {?s rdf:type ?concept. " + countOnePerClusterFilter + nonlinkedFilter + "} } GROUP BY(?concept)", Endpoint.LOCAL));
        } else {
            String conceptFilter = "";
            Pattern pattern = Pattern.compile("concept: ?(\\w+) ?(.*)?");
            Matcher matcher = pattern.matcher(keyword);
            if(matcher.find()) {
                if(matcher.group(1).length() > 0)
                    conceptFilter = "FILTER (regex(str(?concept),\"" + matcher.group(1) + "\",\"i\")) ";
                keyword = matcher.group(2);
            }

            data.addAll(Sparql.select("SELECT ?concept (Count(?concept) AS ?count) {SELECT DISTINCT ?concept ?s WHERE {?s rdf:type ?concept . ?s ?p ?o . FILTER (regex(?o, \"" + keyword + "\", \"i\") || regex(str(?s), \"" + keyword + "\", \"i\")). " + countOnePerClusterFilter + nonlinkedFilter + conceptFilter
                    + "} } GROUP BY(?concept)", Endpoint.LOCAL));

            try {
                data.addAll(Sparql.select("SELECT ?concept (Count(?concept) AS ?count) { SELECT DISTINCT ?concept WHERE {" + keyword + " rdf:type ?concept}} GROUP BY(?concept)", Endpoint.LOCAL));
            } catch(Exception e) {

            }
        }

        for(QuerySolution entry : data) {
            try {
                Resource key = entry.getResource("?concept");
                // key = ResUtil.getLocaleName(key);

                activeFilter.remove(key.getURI());

                String tmp = entry.get("?count").toString();
                Integer count = Integer.valueOf(tmp.substring(0, tmp.lastIndexOf("^^")));

                if(key != null)
                    result.put(key, count);
            } catch(Exception e) {
                continue;
            }
        }

        for(String s : activeFilter) {
            result.put(ResUtils.createResource(s), 0);
        }

        return MapUtils.sortByValue(result, false);
    }


    public static String getURIbyID(String id)
    {
        return uri.get(id);
    }


    private static void searchInstanceInSubClasses(Set<String> noInterest, List<QuerySolution> classes) throws QueryExceptionHTTP
    {
        for(QuerySolution c : classes) {
            List<QuerySolution> tmp = Sparql.select("SELECT * { ?subject rdfs:subClassOf <" + c.getResource("?subject").toString() + "> . }", Endpoint.LOCAL);
            List<QuerySolution> instances = Sparql.select("SELECT DISTINCT ?subject WHERE { ?subject a <" + c.getResource("?subject").toString() + "> . }", Endpoint.LOCAL);
            for(QuerySolution instance : instances) {
                noInterest.add(instance.get("?subject").toString());
            }

            searchInstanceInSubClasses(noInterest, tmp);
        }
    }


    public static void reset()
    {
        uri = null;
    }

}