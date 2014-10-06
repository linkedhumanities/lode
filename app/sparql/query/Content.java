package sparql.query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.entity.EntityContainer;
import models.entity.PropertyContainer;
import models.entity.PropertyModel;
import models.entity.Value;
import models.states.Endpoint;
import models.states.LinkingState;
import settings.Settings;
import sparql.Sparql;
import utils.ResUtils;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class Content
{
    private static Map<String, EntityContainer> uri;


    public static Set<EntityContainer> searchContent(String keyword, List<String> concept, int page, boolean nonlinked) throws QueryExceptionHTTP
    {
        List<QuerySolution> entities = new LinkedList<QuerySolution>();

        String keyword2 = keyword;

        if(uri == null) {
            uri = new HashMap<String, EntityContainer>();
        }

        String nonlinkedFilter1 = "", nonlinkedFilter2 = "";
        if(nonlinked) {
            nonlinkedFilter1 = "&& !regex(str(?s),\"http://dbpedia.org/resource\",\"i\")";
            nonlinkedFilter2 = "|| regex(str(?s2),\"http://dbpedia.org/resource\",\"i\")";
        }

        String keywordFilter = new String();
        if(keyword.length() >= 3) {
            String conceptFilter = "";
            Pattern pattern = Pattern.compile("concept: ?(\\w+) ?(.*)?");
            Matcher matcher = pattern.matcher(keyword);
            if(matcher.find()) {
                if(matcher.group(1).length() > 0)
                    conceptFilter = "?s rdf:type ?t . FILTER (regex(str(?t),\"" + matcher.group(1) + "\",\"i\")). ";
                keyword = matcher.group(2);
            }
            keyword = keyword.replace(":", " ");

            keywordFilter = " FILTER ((regex(?o, \"" + keyword + "\", \"i\") || regex(str(?s), \"" + keyword + "\", \"i\")) " + nonlinkedFilter1 + "). " + conceptFilter;
        } else if(nonlinked) {
            keywordFilter = " FILTER (" + nonlinkedFilter1.substring(3) + ").";
        }

        String clusteringFilter = " FILTER ( NOT EXISTS{?s owl:sameAs ?s2 FILTER (STR(?s2) < STR(?s) " + nonlinkedFilter2 + ")} )";

        String pagination = "";
        if(page > 0) {
            pagination = " ORDER BY ?s LIMIT " + Settings.I_PAGE_SIZE + " OFFSET " + Settings.I_PAGE_SIZE * (page - 1);
        }

        String spo = "?s ?prop ?o.";

        if(concept == null || concept.equals("") || (concept != null && concept.size() == 0)) {
            String query = "SELECT DISTINCT ?s WHERE {";
            query += "{" + spo + keywordFilter + "}";
            query += clusteringFilter + "}" + pagination;
            entities.addAll(Sparql.select(query, Endpoint.LOCAL));
        } else {
            String query = "";
            for(int i = 0; i < concept.size(); i++) {
                if(i > 0) {
                    query += " UNION ";
                }
                query += "{" + spo + "?s rdf:type <" + concept.get(i) + "> ." + keywordFilter + "}";
            }

            entities.addAll(Sparql.select("SELECT DISTINCT ?s WHERE {" + query + clusteringFilter + "}" + pagination, Endpoint.LOCAL));
        }

        Map<EntityContainer, Set<LinkingState>> result = new HashMap<>();
        for(QuerySolution content : entities) {
            EntityContainer ec = new EntityContainer(content.get("?s"));
            if(!result.containsKey(ec)) {
                uri.put(ec.getShortURI(), ec);
                result.put(ec, new HashSet<LinkingState>());
            }
        }

        if(keyword2.contains(":")) {
            EntityContainer ec = getEntityContainerbyID(keyword2);
            if(result.size() == 0 && ec != null) {
                result.put(ec, null);
            }
        }

        return result.keySet();
    }


    public static EntityContainer show(String person) throws QueryExceptionHTTP
    {
        if(uri == null || !uri.containsKey(person)) {
            getEntityContainerbyID(person);
            if(!uri.containsKey(person)) { return null; }
        }

        List<QuerySolution> result = Sparql.select("SELECT ?property ?value WHERE { " + uri.get(person).getURI().toSparql() + " ?property ?value . }", Endpoint.LOCAL);

        // properties and values
        NavigableMap<String, PropertyContainer> properties = new TreeMap<>();
        for(QuerySolution entry : result) {
            Resource res = entry.getResource("?property");

            if(!properties.containsKey(res.toString())) {
                PropertyContainer pair = new PropertyContainer(new PropertyModel(res.toString()));
                properties.put(res.toString(), pair);
            }

            Value value = new Value(entry.get("?value").toString());

            properties.get(res.toString()).addValue(value.getLanguage(), value);
        }

        // load description
        String description = ResUtils.getDescription(new TreeSet<>(properties.values()));

        EntityContainer ec = new EntityContainer(uri.get(person).getURI(), properties, LinkingState.NOT_LINKED, description);
        uri.put(person, ec);

        List<QuerySolution> sameas = Sparql.select("SELECT ?value WHERE { " + uri.get(person).getURI().toSparql() + " owl:sameAs ?value }", Endpoint.LOCAL);
        for(QuerySolution qs : sameas) {
            if(qs.getResource("?value").toString().toLowerCase().contains("dbpedia.org")) {
                ec.addState(LinkingState.DBPEDIA);
            } else if(qs.getResource("?value").toString().toLowerCase().contains("inpho.cogs.indiana.edu")) {
                ec.addState(LinkingState.INDIANA);
            } else {
                ec.addState(LinkingState.UNKNOWN);
            }
        }

        return ec;
    }


    public static EntityContainer getEntityContainerbyID(String id)
    {
        Value value = new Value(id);

        if(uri == null) {
            uri = new HashMap<>();
        }
        if(!uri.containsKey(id) || uri.get(id) == null) {
            try {
                // Search ID
                String query = "SELECT DISTINCT (" + value.toSparql() + " AS ?content) ?relation WHERE {" + value.toSparql() + " ?c ?d . OPTIONAL{" + value.toSparql() + " owl:sameAs ?relation}}";
                List<QuerySolution> result = Sparql.select(query, Endpoint.LOCAL);
                for(QuerySolution content : result) {
                    EntityContainer ec = new EntityContainer(content.get("?content"));
                    uri.put(ec.getShortURI(), ec);
                    if(content.get("?relation") != null) {
                        uri.get(ec.getShortURI()).addState(ResUtils.getState(content.get("?relation")));
                    }
                }
            } catch(Exception e) {
            }
        }
        return uri.get(id);
    }


    public static void loadProperties(EntityContainer ec, List<String> properties) throws QueryExceptionHTTP
    {
        StringBuilder query = new StringBuilder();
        List<String> varNames = new LinkedList<>();

        query.append("SELECT * WHERE { ");
        for(String property : properties) {
            query.append("OPTIONAL {" + ec.getURI().toSparql() + " <" + property + "> ?" + ResUtils.getLocaleName(property) + "} . "); // . FILTER
            // (lang(?" + ResUtil.getLocaleName(property) + ")='en')
            varNames.add(ResUtils.getLocaleName(property));
        }
        query.append("}");

        List<QuerySolution> results = Sparql.select(query.toString(), Endpoint.LOCAL);

        for(QuerySolution result : results) {
            for(int i = 0; i < varNames.size(); i++) {
                try {
                    Value value = new Value(result.get(varNames.get(i)).toString());
                    PropertyModel pm = new PropertyModel(properties.get(i));
                    PropertyContainer pc = new PropertyContainer(pm);

                    pc.addValue(value.getLanguage(), value);
                    ec.addProperty(pc);
                } catch(NullPointerException e) {
                    continue;
                }
            }
        }
    }


    public static Set<String> getSameAsID(String id)
    {
        Set<String> ids = new TreeSet<>();
        String query = "SELECT DISTINCT ?id {" + id + " owl:sameAs ?id}";

        for(QuerySolution result : Sparql.select(query, Endpoint.LOCAL)) {
            ids.add(ResUtils.createShortURI(result.get("?id").toString()));
        }

        return ids;
    }


    public static void removeFromCache(String id)
    {
        uri.remove(id);
    }


    public static void reset()
    {
        uri = null;
    }

}