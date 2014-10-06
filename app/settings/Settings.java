package settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.entity.PropertyModel;
import models.entity.Value;
import models.states.Endpoint;
import play.db.DB;
import sparql.Namespace;
import sparql.query.Concept;
import sparql.query.Content;
import utils.SesameConnect;
import utils.StringUtils;

import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


@SuppressWarnings ("serial")
public class Settings
{
    // database
    public static final String              DB_SETTINGS_TABLE = "LODE_SETTINGS";

    // Repo
    public static String                    SERVER            = "";
    public static String                    REPO              = "";

    // Entity Contain
    public static List<String>              EC_READABLE_NAMES = new ArrayList<String>() {
                                                                  {
                                                                      add("http://dbpedia.org/property/name");
                                                                      add("http://www.w3.org/2000/01/rdf-schema#label");
                                                                      add("http://xmlns.com/foaf/0.1/name");
                                                                      add("http://dbpedia.org/property/alternativeNames");
                                                                      add("http://dbpedia.org/property/id");
                                                                      add("http://dbpedia.org/property/commonName");
                                                                      add("http://www.w3.org/2004/02/skos/core#prefLabel");
                                                                  }
                                                              };
    // Linker
    public static Integer                   L_INSTANCES       = 5;
    public static Integer                   L_PROPERTIES      = 3;
    public static Integer                   L_VALUES          = 3;

    public static List<String>              L_DESCRIPTION     = new ArrayList<String>() {               // Der erste Match wird als Beschreibung verwendet
                                                                  {
                                                                      add("http://dbpedia.org/ontology/abstract");
                                                                      add("http://www.w3.org/2000/01/rdf-schema#comment");
                                                                  }
                                                              };

    // Relation
    public static List<Value>               REL_INSTANCE      = new ArrayList<Value>() {
                                                                  {
                                                                      add(new Value("http://www.w3.org/2002/07/owl#sameAs", "is the same as", 2.0f));
                                                                      add(new Value("http://www.w3.org/2002/07/owl#differentFrom", "is different from", 0.0f));
                                                                      add(new Value("http://www.w3.org/2004/02/skos/core#related", "is related to", 1.0f));
                                                                      add(new Value("http://www.w3.org/2004/02/skos/core#broader", "is broader than", 0.2f));
                                                                      add(new Value("http://www.w3.org/2004/02/skos/core#narrower", "is narrower than", 0.5f));
                                                                  }
                                                              };

    public static List<Value>               REL_CONCEPT       = new ArrayList<Value>() {
                                                                  {
                                                                      add(new Value("http://www.w3.org/2002/07/owl#equivalentClass", "is equivalent to", 2.0f));
                                                                      add(new Value("http://www.w3.org/2002/07/owl#disjointWith", "is disjoint with", 0.0f));
                                                                      add(new Value("http://www.w3.org/2000/01/rdf-schema#subClassOf", "is a subset of", 0.2f));
                                                                      add(new Value("http://www.w3.org/2004/02/skos/core#related", "is related to", 1.0f));
                                                                      add(new Value("http://www.w3.org/2004/02/skos/core#broader", "is broader than", 0.2f));
                                                                      add(new Value("http://www.w3.org/2004/02/skos/core#narrower", "is narrower than", 0.5f));
                                                                  }
                                                              };

    public static List<Value>               REL_PROPERTY      = new ArrayList<Value>() {
                                                                  {
                                                                      add(new Value("http://www.w3.org/2002/07/owl#equivalentProperty", "is equivalent to", 2.0f));
                                                                      add(new Value("http://www.w3.org/2002/07/owl#disjointWith", "is disjoint with", 0.0f));
                                                                      add(new Value("http://www.w3.org/2002/07/owl#inverseOf", "is the inverse of", 0.0f));
                                                                      add(new Value("http://www.w3.org/2000/01/rdf-schema#subPropertyOf", "is a subset of", 0.2f));
                                                                  }
                                                              };

    // Enhancer
    public static Integer                   E_PROPERTIES      = 3;
    public static Integer                   E_VALUES          = 5;
    public static Integer                   E_PREVIEW         = 3;

    // Languages
    public static List<String>              LANG              = new ArrayList<String>() {
                                                                  {
                                                                      add("en");
                                                                  }
                                                              };

    public static final Map<String, String> DEFAULTGRAPH      = new HashMap<String, String>() {
                                                                  {
                                                                      put("dbpedia", "http://dbpedia.org");
                                                                  }
                                                              };

    // Interface
    public static Integer                   I_DESC_LENGTH     = 200;
    public static Integer                   I_VALUE_LENGTH    = 200;
    public static Integer                   I_PAGE_SIZE       = 15;

    // MySQL
    public static String                    MySQL_USER        = "";
    public static String                    MySQL_PASSWORD    = "";
    public static String                    MySQL_SERVER      = "localhost:3306";


    public static Map<String, Object> getSettings()
    {
        Map<String, Object> settings = new HashMap<>();

        // server
        settings.put(SN.SERVER.toString(), SERVER);
        settings.put(SN.REPO.toString(), REPO);

        // repositories
        settings.put("REPOS", SesameConnect.getRepositories());

        // entity container
        settings.put(SN.EC_READABLE_NAMES.toString(), EC_READABLE_NAMES);

        // linker
        settings.put(SN.L_INSTANCES.toString(), L_INSTANCES);
        settings.put(SN.L_PROPERTIES.toString(), L_PROPERTIES);
        settings.put(SN.L_VALUES.toString(), L_VALUES);
        settings.put(SN.L_DESCRIPTION.toString(), L_DESCRIPTION);

        // enhancer
        settings.put(SN.E_PROPERTIES.toString(), E_PROPERTIES);
        settings.put(SN.E_VALUES.toString(), E_VALUES);
        settings.put(SN.E_PREVIEW.toString(), E_PREVIEW);

        // interface
        settings.put(SN.I_DESC_LENGTH.toString(), I_DESC_LENGTH);
        settings.put(SN.I_VALUE_LENGTH.toString(), I_VALUE_LENGTH);
        settings.put(SN.I_PAGE_SIZE.toString(), I_PAGE_SIZE);

        // lang
        settings.put(SN.LANG.toString(), LANG);

        // relations
        settings.put(SN.REL_INSTANCE.toString(), StringUtils.valueListToString(REL_INSTANCE));
        settings.put(SN.REL_CONCEPT.toString(), StringUtils.valueListToString(REL_CONCEPT));
        settings.put(SN.REL_PROPERTY.toString(), StringUtils.valueListToString(REL_PROPERTY));

        // mysql
        settings.put(SN.MySQL_SERVER.toString(), MySQL_SERVER);
        settings.put(SN.MySQL_USER.toString(), MySQL_USER);
        settings.put(SN.MySQL_PASSWORD.toString(), MySQL_PASSWORD);

        return settings;
    }

    enum SN {
        SERVER, REPO, EC_READABLE_NAMES, L_INSTANCES, L_PROPERTIES, L_VALUES, L_DESCRIPTION, E_PROPERTIES, E_VALUES, E_PREVIEW, LANG, I_DESC_LENGTH, I_VALUE_LENGTH, I_PAGE_SIZE, REL_INSTANCE, REL_CONCEPT, REL_PROPERTY, MySQL_USER, MySQL_PASSWORD, MySQL_SERVER
    }


    public static void restoreSettings(Map<String, String> map, boolean save) throws QueryExceptionHTTP
    {
        // server
        boolean reset = false;
        if(map.containsKey(SN.SERVER.toString())) {
            SERVER = map.get(SN.SERVER.toString());
            Endpoint.LOCAL = new Endpoint(map.get(SN.SERVER.toString()) + "/openrdf-sesame/repositories/" + map.get(SN.REPO.toString()));
        }
        if(map.containsKey(SN.REPO.toString())) {
            if(!REPO.equals(map.get(SN.REPO.toString()))) {
                reset = true;
            }
            REPO = map.get(SN.REPO.toString());
        }

        // entity container
        if(map.containsKey(SN.EC_READABLE_NAMES.toString())) {
            EC_READABLE_NAMES = StringUtils.stringToList(map.get(SN.EC_READABLE_NAMES.toString()), new ArrayList<String>());
        }

        // linker
        if(map.containsKey(SN.L_INSTANCES.toString())) {
            L_INSTANCES = Integer.valueOf(map.get(SN.L_INSTANCES.toString()));
        }
        if(map.containsKey(SN.L_PROPERTIES.toString())) {
            L_PROPERTIES = Integer.valueOf(map.get(SN.L_PROPERTIES.toString()));
        }
        if(map.containsKey(SN.L_VALUES.toString())) {
            L_VALUES = Integer.valueOf(map.get(SN.L_VALUES.toString()));
        }

        if(map.containsKey(SN.L_DESCRIPTION.toString())) {
            L_DESCRIPTION = StringUtils.stringToList(map.get(SN.L_DESCRIPTION.toString()), new ArrayList<String>());
        }

        // enhancer
        if(map.containsKey(SN.E_PROPERTIES.toString())) {
            E_PROPERTIES = Integer.valueOf(map.get(SN.E_PROPERTIES.toString()));
        }
        if(map.containsKey(SN.E_VALUES.toString())) {
            E_VALUES = Integer.valueOf(map.get(SN.E_VALUES.toString()));
        }
        if(map.containsKey(SN.E_PREVIEW.toString())) {
            E_PREVIEW = Integer.valueOf(map.get(SN.E_PREVIEW.toString()));
        }

        // language
        if(map.containsKey(SN.LANG.toString())) {
            LANG = StringUtils.stringToList(map.get(SN.LANG.toString()), new ArrayList<String>());
        }

        // interface
        if(map.containsKey(SN.I_DESC_LENGTH.toString())) {
            I_DESC_LENGTH = Integer.valueOf(map.get(SN.I_DESC_LENGTH.toString()));
        }
        if(map.containsKey(SN.I_VALUE_LENGTH.toString())) {
            I_VALUE_LENGTH = Integer.valueOf(map.get(SN.I_VALUE_LENGTH.toString()));
        }
        if(map.containsKey(SN.I_PAGE_SIZE.toString())) {
            I_PAGE_SIZE = Integer.valueOf(map.get(SN.I_PAGE_SIZE.toString()));
        }

        // relations
        if(map.containsKey(SN.REL_INSTANCE.toString())) {
            REL_INSTANCE = StringUtils.stringToValueList(map.get(SN.REL_INSTANCE.toString()));
        }

        if(map.containsKey(SN.REL_CONCEPT.toString())) {
            REL_CONCEPT = StringUtils.stringToValueList(map.get(SN.REL_CONCEPT.toString()));
        }

        if(map.containsKey(SN.REL_PROPERTY.toString())) {
            REL_PROPERTY = StringUtils.stringToValueList(map.get(SN.REL_PROPERTY.toString()));
        }

        // mysql
        if(map.containsKey(SN.MySQL_SERVER.toString())) {
            MySQL_SERVER = String.valueOf(map.get(SN.MySQL_SERVER.toString()));
        }

        if(map.containsKey(SN.MySQL_USER.toString())) {
            MySQL_USER = String.valueOf(map.get(SN.MySQL_USER.toString()));
        }

        if(map.containsKey(SN.MySQL_PASSWORD.toString())) {
            MySQL_PASSWORD = String.valueOf(map.get(SN.MySQL_PASSWORD.toString()));
        }

        if(save) {
            saveSettings();
        }
        if(reset) {
            reset();
        }

    }


    public static void saveSettings()
    {
        Map<String, Object> settingsMap = getSettings();

        try {
            Connection connection = DB.getConnection();
            PreparedStatement stmt;
            for(String key : settingsMap.keySet()) {
                String deleteString = "DELETE FROM " + DB_SETTINGS_TABLE + " where S_ID = ?";
                stmt = connection.prepareStatement(deleteString);
                stmt.setString(1, key);
                stmt.executeUpdate();
                stmt.close();

                String createString = "insert into " + DB_SETTINGS_TABLE + " values(?,?)";
                stmt = connection.prepareStatement(createString);
                stmt.setString(1, key);
                stmt.setString(2, settingsMap.get(key).toString());
                stmt.executeUpdate();
                stmt.close();
            }
            connection.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }


    public static void loadSettings() throws QueryExceptionHTTP
    {
        Map<String, String> tempMap = new HashMap<>();
        try {
            Connection connection = DB.getConnection();
            String query = "SELECT * FROM " + DB_SETTINGS_TABLE;
            Statement stmt = connection.createStatement();
            ResultSet res = stmt.executeQuery(query);

            while(res.next()) {
                tempMap.put(res.getString("S_ID"), res.getString("S_VALUE"));
            }
            stmt.close();
            connection.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }

        restoreSettings(tempMap, false);
    }


    private static void reset() throws QueryExceptionHTTP
    {
        System.out.println("RESET STATIC INFORMATION");
        // DESTINATION.put("sesame", Settings.SERVER + "/openrdf-sesame/repositories/" + Settings.REPO);
        new PropertyModel().loadFromFile();
        Content.reset();
        Concept.reset();
        Namespace.reset();
    }

}