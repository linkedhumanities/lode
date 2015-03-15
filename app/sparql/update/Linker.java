package sparql.update;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import models.akka.messages.UpdateMessage;
import models.entity.EntityContainer;
import models.entity.PropertyContainer;
import models.entity.Value;
import models.states.Endpoint;
import settings.Settings;
import sparql.Sparql;
import sparql.query.Content;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import utils.MapUtils;
import utils.ResUtils;
import utils.StringUtils;
import akka.actor.ActorRef;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class Linker
{
    private static Map<String, String>                      instanceURIs = new HashMap<>();
    private static Map<String, Map<EntityContainer, Float>> cache        = new HashMap<>();
    private static Set<String>                              blacklist    = new HashSet<>();


    public static List<EntityContainer> getProposals(String id, Endpoint source, int method, ActorRef observer) throws QueryExceptionHTTP, SQLException, ClassNotFoundException
    {
        // key
        String key = id + "" + method;

        // cache
        if(!cache.containsKey(key)) {
            cache.put(key, new HashMap<EntityContainer, Float>());
        }
        Map<EntityContainer, Float> proposals = cache.get(key);

        // loads similar instances (dbpedia) and calculates the weight (levenshtein)
        EntityContainer localInstance = Content.getEntityContainerbyID(id);
        if(localInstance == null) { return null; }

        // readable names
        Set<String> names = localInstance.getReadableNames();
        names = StringUtils.generateNames(names);

        // progress bar
        observer.tell(new UpdateMessage("Step 1: ", 5, "Initialization"), null);

        // string similarity
        Levenshtein ls = new Levenshtein();

        // search proposals
        List<String> searchterms = new LinkedList<>(names);
        for(int i = 0; i < names.size(); i++) {
            String name = searchterms.get(i).trim();
            String searchterm = searchterms.get(i).trim().replace("'", "\u2019");

            observer.tell(new UpdateMessage("Step 2: ", (i * (50 / names.size())) + 6, "Searching Proposals (" + (i + 1) + "/" + (names.size()) + ")"), null);

            List<QuerySolution> candidates = null;
              
            switch(method) {
                
                case 0:
                    candidates = (Sparql.select("SELECT ?i ?d WHERE { ?i <http://dbpedia.org/ontology/abstract> ?d . ?d <bif:contains> \"'" + searchterm
                            + "'\" . FILTER (lang(?d)='en') . MINUS{?i rdf:type <http://www.w3.org/2004/02/skos/core#Concept>} . MINUS{?i rdf:type <http://www.w3.org/2002/07/owl#DatatypeProperty>} . }", Endpoint.DBPEDIA));
                    candidates.addAll(Sparql.select("SELECT ?i ?d WHERE { ?i <http://www.w3.org/2000/01/rdf-schema#label> ?d . ?d <bif:contains> \"'" + searchterm
                            + "'\" . FILTER (lang(?d)='en') . MINUS{?i rdf:type <http://www.w3.org/2004/02/skos/core#Concept>} . MINUS{?i rdf:type <http://www.w3.org/2002/07/owl#DatatypeProperty>} . }", Endpoint.DBPEDIA));
                    for(QuerySolution candidate : candidates) {
                    	
                    	try{
                        if(candidate.getResource("?i").toString().contains("http://dbpedia.org/resource")) {
                            EntityContainer proposal = new EntityContainer(candidate.getResource("?i"));

                            // weighting
                            Float weight = ls.getSimilarity(name.toLowerCase(), ResUtils.getLocaleName(proposal.getURI().getValue().replace("_", " ")).toLowerCase());
                            if(!localInstance.getReadableNames().contains(name)) {
                                weight = weight * (7.0f / 8.0f);
                            }

                            if(proposals.containsKey(proposal) && proposals.get(proposal) > weight) {
                                continue;
                            }
                            proposals.put(proposal, weight);
                        }
                    	}
                    	catch(Exception e)
                    	{
                    		System.out.println("Exception in Linker.java");
                    	}
                    }
                  break;
                case 1:
                    // use only subsets if the results is empty for a search term
                    if(!localInstance.getReadableNames().contains(name))
                        continue;

                    Class.forName("com.mysql.jdbc.Driver");

                    String arnab_database = "wikiStat";
                    String arnab_table = "wikiPrep";
                    Connection connect = DriverManager.getConnection("jdbc:mysql://" + Settings.MySQL_SERVER + "/" + arnab_database + "?user=" + Settings.MySQL_USER + "&password=" + Settings.MySQL_PASSWORD + "&useUnicode=true&characterEncoding=utf-8");

                    List<String> searchTerms = new ArrayList<>();
                    searchTerms.add(name);

                    boolean subString = false;

                    while(!searchTerms.isEmpty()) {
                        PreparedStatement stmt2 = connect.prepareStatement("SELECT * FROM " + arnab_table + " WHERE sf = ? OR uri = ? ORDER BY count desc LIMIT 20");
                        stmt2.setString(1, searchTerms.get(0));
                        stmt2.setString(2, searchTerms.get(0));

                        searchTerms.remove(0);

                        System.out.println("SQL-Query: " + name);

                        ResultSet rs = stmt2.executeQuery();
                        boolean empty = true;

                        while(rs.next()) {
                            empty = false;
                            EntityContainer proposal = new EntityContainer(new Value("http://dbpedia.org/resource/" + rs.getString("uri").replace(" ", "_")));
                            if(!proposals.containsKey(proposal)) {
                                proposals.put(proposal, (float) rs.getInt("count"));
                            } else {
                                proposals.put(proposal, proposals.get(proposal) + (float) rs.getInt("count"));
                            }

                            System.out.println("\t" + rs.getString("uri") + " " + rs.getInt("count") + " / " + proposals.get(proposal));
                        }

                        if(!subString && empty) {
                            subString = true;
                            searchTerms.addAll(0, StringUtils.generateNames(names));
                        }
                    }
                    connect.close();
                break;
                default:
                 	 System.out.println("in case default");
                    candidates = new LinkedList<>();
                break;
            }
        }

        // tell
        observer.tell(new UpdateMessage("Step 2: ", 55, "Searching Complete"), null);

        // load page one
        return show(key, source, observer, 1);
    }


    public static List<EntityContainer> show(String key, Endpoint source, ActorRef observer, int pagenumber)
    {
        // cache
        instanceURIs = new HashMap<String, String>();
        Map<EntityContainer, Float> proposals = cache.get(key);

        // stuff
        List<EntityContainer> result = new LinkedList<>();
        EntityContainer localInstance = Content.getEntityContainerbyID(key.substring(0, key.length() - 1));

        if(localInstance == null) { return null; }

        // determine number of entries for the first page
        int maxCv = Settings.L_INSTANCES;
        if(proposals.size() < Settings.L_INSTANCES) {
            maxCv = proposals.size();
        }

        // build proposal entity container
        List<EntityContainer> canditates = sortProposals(key);
        for(int i = (pagenumber - 1) * Settings.L_INSTANCES; i < canditates.size(); i++) {
            EntityContainer proposal = canditates.get(i);

            // handle redirect URLs
            EntityContainer tmp = new EntityContainer(proposal.getRedirectURI());
            if(!proposal.getURI().equals(tmp.getURI())) {
                if(proposals.containsKey(tmp) && proposals.get(proposal) <= proposals.get(tmp)) {
                    proposals.remove(proposal);
                    continue;
                } else if((proposals.containsKey(tmp) && proposals.get(proposal) > proposals.get(tmp) || !proposals.containsKey(tmp))) {
                    proposals.put(tmp, proposals.get(proposal));
                    proposals.remove(proposal);
                    proposal = tmp;
                }
            }

            // validate proposal
            int code = validate(key, localInstance, proposal, source);
            if(code < 1) {
                canditates = sortProposals(key);
                i += code;
                continue;
            }

            // progress bar
            if(observer != null) {
                observer.tell(new UpdateMessage("Step 3: ", (result.size() * (45 / maxCv)) + 56, "Loading Properties (" + (result.size() + 1) + "/" + (maxCv) + ")"), null);
            }

            // load proposal
            load(proposal);

            // skip if no proposals are available
            if(proposal.getProperties().size() == 0) {
                // continue;
            }

            // store result
            result.add(proposal);

            // break if we have x similar results
            if(result.size() == Settings.L_INSTANCES) {
                break;
            }
        }

        return result;
    }


    public static void setLink(Value local, Value remote, Value relation)
    {
        // update database
        String command = "INSERT DATA { " + local.toSparql() + " " + relation.toSparql() + " " + remote.toSparql() + " }";
        Sparql.update(command, Endpoint.LOCAL);

        // update local variable
        Content.show(ResUtils.createShortURI(local.getValue()));
    }


    public static boolean hasRelation(Value uri1, Value uri2) throws QueryExceptionHTTP
    {
        // search relation
        List<QuerySolution> result = Sparql.select("SELECT * { " + uri2.toSparql() + " <" + "owl:sameAs" + "> " + uri1.toSparql() + " }", Endpoint.LOCAL);
        if(result.size() > 0) { return true; }

        return false;
    }


    private static int validate(String key, EntityContainer localInstance, EntityContainer proposal, Endpoint source)
    {
        // cache
        Map<EntityContainer, Float> proposals = cache.get(key);

        // duplicates
        if(instanceURIs.containsKey(ResUtils.createShortURI(proposal.getURI().getValue())) || proposal.equals(localInstance) || hasRelation(proposal.getURI(), localInstance.getURI())) { return 0; }

        // disambiguates
        if(!blacklist.contains(proposal.getURI().getValue())) {
            List<Value> disambiguates = ResUtils.getDisambiguates(proposal.getURI());
            if(disambiguates.size() > 0) {
                for(Value disambiguate : disambiguates) {
                    proposals.put(new EntityContainer(disambiguate), proposals.get(proposal));
                }
                blacklist.add(proposal.getURI().getValue());
                proposals.remove(proposal);
                return -1;
            }
        }

        return 1;
    }


    private static void load(EntityContainer proposal)
    {
        // grep each property including all values for the current instance
        NavigableMap<String, PropertyContainer> tmpProperties = ResUtils.storeProperties(proposal.getURI(), Settings.LANG, null);

        // grep top properties
        NavigableMap<String, PropertyContainer> topProperties = ResUtils.getTopProperties(new TreeSet<>(tmpProperties.values()), Settings.L_PROPERTIES, Settings.L_VALUES);

        // only submap of top-properties
        if(topProperties.size() > Settings.L_PROPERTIES) {
            List<String> keys = (new ArrayList<String>(topProperties.keySet()));
            topProperties = topProperties.subMap(keys.get(0), true, keys.get(Settings.L_PROPERTIES), false);
        }

        // load description
        String description = ResUtils.getDescription(new TreeSet<>(tmpProperties.values()));

        // build container
        proposal.setDescription(description);
        proposal.setProperties(topProperties);

        // store container
        instanceURIs.put(proposal.getShortURI(), proposal.getURI().getValue());
    }


    private static List<EntityContainer> sortProposals(String key)
    {
        // cache
        Map<EntityContainer, Float> proposals = cache.get(key);

        List<EntityContainer> result = new LinkedList<>();

        // proposals sorted by weight descending
        proposals = MapUtils.sortByValue(proposals, true);

        // proposals sorted by lexical grouped by weights
        List<EntityContainer> canditates = new LinkedList<>(proposals.keySet());
        NavigableMap<String, EntityContainer> tmp = new TreeMap<>();
        for(int i = 0; i < canditates.size(); i++) {
            tmp.put(canditates.get(i).getURI().getValue().toLowerCase(), canditates.get(i));
            if((i == canditates.size() - 1) || !((proposals.get(canditates.get(i)) - (proposals.get(canditates.get(i + 1)))) < 5.96e-08)) {
                // proposals sorted by lexical descending
                result.addAll(tmp.values());
                tmp = new TreeMap<>();
            }
        }

        return result;
    }
}