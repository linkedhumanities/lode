package utils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualTreeBidiMap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import settings.Settings;



public class SesameConnect
{

    public static void main(String[] args)
    {
        System.out.println(getRepositories().toString());
    }


    public static List<Map<String, String>> getXMLInformation(String uri)
    {
        StringBuffer xmlString = new StringBuffer();
        List<Map<String, String>> result = new ArrayList<>();
        try {
            // load xml file
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/sparql-results+xml");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while((line = rd.readLine()) != null) {
                xmlString.append(line);
            }

            // extract information
            String data = xmlString.toString();
            SAXBuilder builder = new SAXBuilder();
            StringReader in = new StringReader(data);
            Document doc = builder.build(in);
            Element rootNode = doc.getRootElement();
            for(Element x : rootNode.getChildren()) {
                if(x.getName() == "results") {
                    for(Element y : x.getChildren()) {
                        Map<String, String> temp = new HashMap<>();
                        for(Element z : y.getChildren()) {
                            temp.put(z.getAttributes().get(0).getValue(), z.getValue());
                        }
                        result.add(temp);
                    }
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    public static List<String> getRepositories()
    {
        List<String> result = new ArrayList<>();

        for(Map<String, String> map : getXMLInformation(Settings.SERVER + "/openrdf-workbench/repositories/")) {
            if(map.containsKey("id")) {
                result.add(map.get("id").trim());
            }
        }

        return result;
    }


    public static BidiMap getNamespaces()
    {
        BidiMap namespace = new DualTreeBidiMap();
        for(Map<String, String> map : getXMLInformation(Settings.SERVER + "/openrdf-workbench/repositories/" + Settings.REPO + "/namespaces")) {
            if(map.containsKey("prefix") && map.containsKey("namespace")) {
                String prefix = map.get("prefix").trim();
                String ns = map.get("namespace").trim();
                namespace.put(prefix, ns);
            }
        }
        return namespace;
    }


    // do not change: rdf, owl, foaf, rdf
    public static void saveNameSpace(String prefix, String namespace)
    {
        try {
            HttpURLConnection httpCon;
            URL url = new URL(Settings.SERVER + "/openrdf-sesame/repositories/" + Settings.REPO + "/namespaces/" + prefix);
            httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("PUT");
            httpCon.setRequestProperty("Content-Type", "text/plain");

            OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
            out.write(namespace);
            out.close();
            httpCon.getResponseCode();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    public static void deleteNameSpace(String prefix)
    {
        try {
            HttpURLConnection httpCon;
            URL url = new URL(Settings.SERVER + "/openrdf-sesame/repositories/" + Settings.REPO + "/namespaces/" + prefix);
            httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestMethod("DELETE");
            httpCon.getResponseCode();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    // checks if a prefix is linked to a namespace
    public static void connectToNS(String prefix)
    {
        try {
            HttpURLConnection httpCon;
            URL url = new URL(Settings.SERVER + "/openrdf-sesame/repositories/" + Settings.REPO + "/namespaces/" + prefix);
            httpCon = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
            String inputLine;
            while((inputLine = in.readLine()) != null)
                System.out.println(inputLine);
            in.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
