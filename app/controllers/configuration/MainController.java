package controllers.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiKeyMap;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import settings.Settings;
import sparql.query.Concept;
import sparql.query.Property;
import utils.StringUtils;
import views.html.displayError;
import views.html.configuration.configuration_concepts;
import views.html.configuration.configuration_concepts_ajax;
import views.html.configuration.configuration_properties;
import views.html.configuration.configuration_properties_details;
import views.html.configuration.configuration_properties_examples;
import views.html.configuration.configuration_relation_store;
import views.html.configuration.configuration_start;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class MainController extends Controller
{
    // basic settings

    public static Result start()
    {
        Map<String, Object> settings = Settings.getSettings();
        return ok(configuration_start.render(settings));
    }


    public static Result saveBasicSettings()
    {
        try {
            Map<String, String[]> req = request().queryString();
            String data = req.get("data")[0] != null ? req.get("data")[0] : "";

            Map<String, String> tmpMap = new HashMap<>();
            JsonNode jsn = Json.parse(data);
            Iterator<String> it = jsn.fieldNames();

            System.out.println("Save: ");
            while(it.hasNext()) {
                String key = it.next();
                String value = jsn.get(key).asText().trim();
                tmpMap.put(key, value);
                System.out.println(String.format("%-15s", key) + "\t" + value);
            }
            Settings.restoreSettings(tmpMap, true);
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
        return ok();
    }


    // concept settings

    public static Result concepts()
    {
        return ok(configuration_concepts.render());
    }


    public static Result getConcept(String concept)
    {
        try {
            List<String> conceptProperties = Concept.getProperties(concept);
            return ok(configuration_concepts_ajax.render(concept, conceptProperties));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
    }


    // property settings

    public static Result properties()
    {
        try {
            Map<String, Map<String, Map<String, String>>> allPropertiesMap = Property.allProperties();
            Map<String, List<String>> sortedURI = new HashMap<>();

            for(String type : allPropertiesMap.keySet()) {
                List<String> uri = new ArrayList<>();
                uri.addAll(allPropertiesMap.get(type).keySet());
                Collections.sort(uri);
                sortedURI.put(type, uri);
            }
            return ok(configuration_properties.render(allPropertiesMap, sortedURI));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
    }


    public static Result showProperty()
    {
        Map<String, String[]> req = request().queryString();
        String propertyString = req.get("property")[0];
        String lastLoad = req.get("lastLoad")[0];

        try {
            Map<String, String> basicInformation = Property.getBasicInformation(propertyString);

            Map<String, String> response = new HashMap<>();
            response.put("main", configuration_properties_details.render(propertyString, basicInformation).toString());
            response.put("lastLoad", lastLoad);

            return ok(Json.toJson(response));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
    }


    public static Result showPropertyExample()
    {
        try {
            Map<String, String[]> req = request().queryString();
            String propertyString = req.get("property")[0];
            String lastLoad = req.get("lastLoad")[0];

            List<String[]> examples = Property.getTriples(propertyString);

            Map<String, String> response = new HashMap<>();
            response.put("main", configuration_properties_examples.render(examples).toString());
            response.put("lastLoad", lastLoad);

            return ok(Json.toJson(response));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
    }


    // relation store
    public static Result relationStore()
    {
        // MultiKeyMap map = ResRelationStore.getStore();
        // Set<MultiKey> keys = map.keySet();
        // for(MultiKey key : keys) {
        // System.out.println(key.getKey(0));
        // }
        return ok(configuration_relation_store.render(new MultiKeyMap()));
    }
}