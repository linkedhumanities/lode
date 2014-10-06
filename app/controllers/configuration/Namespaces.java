package controllers.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sparql.Namespace;
import views.html.configuration.configuration_namespaces;
import views.html.configuration.configuration_namespaces_form;

import com.fasterxml.jackson.databind.JsonNode;


public class Namespaces extends Controller
{
    public static Result namespaces()
    {
        return ok(configuration_namespaces.render());
    }


    public static Result loadNamespaces()
    {
        Map<String, String> map = Namespace.getNamespacesAsMap();
        List<String> prefixes = new ArrayList<>();
        prefixes = new ArrayList<>(new TreeSet<String>(map.keySet()));

        return ok(configuration_namespaces_form.render(prefixes, map));
    }


    public static Result saveNamespaces()
    {
        Map<String, String[]> req = request().queryString();
        String data = req.get("data")[0] != null ? req.get("data")[0] : "";

        Map<String, String> tmpMap = new HashMap<>();
        JsonNode jsn = Json.parse(data);
        Iterator<String> it = jsn.fieldNames();

        while(it.hasNext()) {
            String prefix = it.next();
            String namespace = jsn.get(prefix).asText();
            tmpMap.put(prefix, namespace);
            System.out.println(String.format("%-15s", prefix) + "\t" + namespace);
        }
        Namespace.setNamespaces(tmpMap);

        return ok();
    }


    public static Result addNamespace()
    {
        Map<String, String[]> req = request().queryString();

        if(req.containsKey("prefix") && req.containsKey("ns")) {
            String prefix, ns;
            prefix = req.get("prefix")[0];
            ns = req.get("ns")[0];
            Namespace.addNamespace(prefix, ns);
        }

        return ok();
    }


    public static Result removeNamespace()
    {
        Map<String, String[]> req = request().queryString();

        if(req.containsKey("prefix")) {
            Namespace.removeNamespace(req.get("prefix")[0]);
        }
        return ok();
    }

}
