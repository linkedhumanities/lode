package sparql;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualTreeBidiMap;

import utils.SesameConnect;


public class Namespace
{
    private static BidiMap namespace = new DualTreeBidiMap();


    private static BidiMap loadNamespaces()
    {
        if(namespace.isEmpty()) {
            namespace = SesameConnect.getNamespaces();
        }
        return namespace;
    }


    public static String getNamespace(String prefix)
    {
        loadNamespaces();
        return namespace.get(prefix).toString();
    }


    public static String getPrefix(String ns)
    {
        String prefix = "";
        loadNamespaces();
        prefix = (String) namespace.getKey(ns);
        int n = 0;
        if(prefix == null && ns != null && ns.length() > 0) {
            do {
                // double random = Math.random();
                // int n = (int) Math.round(random * 1000) + 1;
                n++;
                prefix = "uri" + n;
            } while(namespace.containsKey(prefix));
            addNamespace(prefix, ns);
        }

        return prefix;
    }


    public static String toSparqlFormat()
    {
        StringBuilder sb = new StringBuilder();
        for(Object s : loadNamespaces().keySet()) {
            sb.append("PREFIX " + s + ":<" + namespace.get(s) + ">");
            sb.append(" ");
        }
        return sb.toString();
    }


    public static Map<String, String> getNamespacesAsMap()
    {
        Map<String, String> map = new HashMap<>();
        for(Object key : loadNamespaces().keySet()) {
            String value = (String) loadNamespaces().get(key);
            map.put((String) key, value);
        }
        return map;
    }


    public static void setNamespaces(Map<String, String> map)
    {
        for(String prefix : map.keySet()) {
            String ns = map.get(prefix);

            if(namespace.containsValue(ns)) {
                if(!((String) namespace.getKey((Object) ns)).equals(prefix)) {
                    System.out.println("delete: " + namespace.getKey(ns) + " => create " + prefix);
                    SesameConnect.deleteNameSpace((String) namespace.getKey(ns));
                    namespace.removeValue(ns);
                    SesameConnect.saveNameSpace(prefix, ns);
                    namespace.put(prefix, ns);
                } else {
                    System.out.println("unchanged: " + namespace.getKey(ns));
                }
            } else if(!namespace.containsKey((Object) prefix)) {
                System.out.println("create: " + namespace.getKey(ns));
                SesameConnect.saveNameSpace(prefix, ns);
                namespace.put(prefix, ns);
            } else {
                System.out.println("??? " + prefix + "-" + ns);
            }

        }
    }


    public static void addNamespace(String prefix, String ns)
    {
        if(!namespace.containsValue(ns) && !namespace.containsKey(prefix)) {
            namespace.put(prefix, ns);
            SesameConnect.saveNameSpace(prefix, ns);
        }
    }


    public static void removeNamespace(String prefix)
    {
        if(namespace.containsKey(prefix)) {
            System.out.println("remove " + prefix);
            namespace.remove(prefix);
            SesameConnect.deleteNameSpace(prefix);
        }
    }


    public static void main(String[] args)
    {
        setCorrectNamespaces();
        // http://www.openrdf.org/doc/sesame2/system/ch08.html
    }


    private static void setCorrectNamespaces()
    {
        for(String prefix : getNamespacesAsMap().keySet()) {
            SesameConnect.deleteNameSpace(prefix);
        }

        SesameConnect.saveNameSpace("db", "http://dbpedia.org/resource/");
        SesameConnect.saveNameSpace("dc", "http://purl.org/dc/elements/1.1/");
        SesameConnect.saveNameSpace("entity", "http://inpho.cogs.indiana.edu/entity/");
        SesameConnect.saveNameSpace("foaf", "http://xmlns.com/foaf/0.1/");
        SesameConnect.saveNameSpace("idea", "http://inpho.cogs.indiana.edu/idea/");
        SesameConnect.saveNameSpace("inpho", "http://inpho.cogs.indiana.edu/");
        SesameConnect.saveNameSpace("owl", "http://www.w3.org/2002/07/owl#");
        SesameConnect.saveNameSpace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        SesameConnect.saveNameSpace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        SesameConnect.saveNameSpace("skos", "http://www.w3.org/2004/02/skos/core#");
        SesameConnect.saveNameSpace("thinker", "http://inpho.cogs.indiana.edu/thinker/");
        SesameConnect.saveNameSpace("user", "http://inpho.cogs.indiana.edu/user/");
        SesameConnect.saveNameSpace("yago", "http://dbpedia.org/class/yago/");
    }


    public static void reset()
    {
        namespace = new DualTreeBidiMap();
    }

}