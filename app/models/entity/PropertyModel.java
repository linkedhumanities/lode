package models.entity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import models.states.Endpoint;

import sparql.Sparql;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class PropertyModel implements Comparable<PropertyModel>, Serializable
{
    private String                      uri;
    private String                      namespace;
    private String                      localname;
    private int                         weight;

    private static Map<String, Integer> frequencyProperty;

    private static final long           serialVersionUID = 1L;


    public PropertyModel()
    {

    }


    public PropertyModel(String uri) throws QueryExceptionHTTP
    {
        if(frequencyProperty == null) {
            loadFromFile();
        }

        this.uri = uri;
        try {
            this.weight = frequencyProperty.get(uri);
        } catch(Exception e) {
            this.weight = 0;
        }

        if(uri.contains("#")) {
            this.localname = uri.substring(uri.lastIndexOf("#") + 1);
            this.namespace = uri.substring(0, uri.lastIndexOf("#"));
        } else {
            this.localname = uri.substring(uri.lastIndexOf("/") + 1);
            this.namespace = uri.substring(0, uri.lastIndexOf("/"));
        }
        for(int i = 0; i < localname.length(); i++) {
            if(localname.substring(i, i + 1).matches("[A-Z]") && i > 0 && !localname.substring(i - 1, i).matches("[A-Z]")) {
                localname = localname.substring(0, i) + " " + localname.substring(i);
                i++;
            }
        }
        localname = localname.toLowerCase();
    }


    public void countOnDBPedia() throws QueryExceptionHTTP
    {
        int offset = 0;
        List<QuerySolution> dbdata = new ArrayList<QuerySolution>();

        do {
            dbdata.addAll(Sparql.select("SELECT ?predicate (COUNT(?subject) AS ?sum) WHERE { ?subject ?predicate ?object } GROUP BY ?predicate OFFSET " + offset + " LIMIT 40000", Endpoint.DBPEDIA));
            offset += 40000;
        } while(dbdata.size() == offset);

        frequencyProperty = new HashMap<String, Integer>();
        for(QuerySolution qs : dbdata) {
            frequencyProperty.put(qs.get("?predicate").toString(), Integer.valueOf(qs.get("?sum").toString().substring(0, qs.get("?sum").toString().lastIndexOf("^") - 1)));
        }

        FileWriter outputStream = null;
        try {
            outputStream = new FileWriter("conf/properties.txt");
            for(String key : frequencyProperty.keySet()) {
                outputStream.write(key + ";" + frequencyProperty.get(key) + System.getProperty("line.separator"));
            }
            outputStream.flush();
            outputStream.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    public void loadFromFile() throws QueryExceptionHTTP
    {
        frequencyProperty = new HashMap<String, Integer>();
        try {
            BufferedReader inputStream = new BufferedReader(new FileReader("conf/properties.txt"));
            String line;
            while((line = inputStream.readLine()) != null) {
                String property = line.substring(0, line.lastIndexOf(";"));
                Integer count = Integer.valueOf(line.substring(line.lastIndexOf(";") + 1));
                frequencyProperty.put(property, count);
            }
            inputStream.close();
        } catch(IOException e) {
            System.out.println("Count DBPedia Properties... (this can take some time)");
            //e.printStackTrace();
            countOnDBPedia();
        }
    }


    public List<String> getOrderdPropertyNames()
    {
        ValueComparator bvc = new ValueComparator(frequencyProperty);
        Map<String, Integer> sorted_data = new TreeMap<String, Integer>(bvc);
        sorted_data.putAll(frequencyProperty);

        return new ArrayList<String>(sorted_data.keySet());
    }

    class ValueComparator implements Comparator<String>
    {
        Map<String, Integer> base;


        public ValueComparator(Map<String, Integer> base)
        {
            this.base = base;
        }


        public int compare(String a, String b)
        {
            if(base.get(a) >= base.get(b)) {
                return -1;
            } else {
                return 1;
            }
        }
    }


    public String getName()
    {
        return localname;
    }


    public String getURI()
    {
        return uri;
    }


    public String getNamespace()
    {
        return namespace;
    }


    public int getWeight()
    {
        return weight;
    }


    public void setName(String name)
    {
        this.localname = name;
    }


    @Override
    public int compareTo(PropertyModel p)
    {
        if(this.weight < p.weight) {
            return -1;
        } else {
            return 1;
        }
    }


    @Override
    public String toString()
    {
        return localname;
    }
}