package controllers.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import models.entity.EntityContainer;
import models.entity.PropertyContainer;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualTreeBidiMap;
import org.apache.jena.atlas.lib.Pair;
import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import settings.Settings;
import sparql.Namespace;
import sparql.query.Concept;
import sparql.query.Content;
import utils.MapUtils;
import utils.ResUtils;
import utils.StringUtils;
import views.html.displayError;
import views.html.content.content_entity;
import views.html.content.content_index_concept_filter;
import views.html.content.content_index_indiviudals;
import views.html.content.index_content;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class MainController extends Controller
{

    public static Result content()
    {
        return ok(index_content.render("Explore"));
    }


    /**
     * Generates concept filter box.
     * 
     * @return /content concept filter
     */
    public static Result getConcepts()
    {
        try {
            Map<String, String[]> req = request().queryString();
            Map<String, Object> response = new HashMap<>();

            // query
            String query = req.get("query")[0] != null ? req.get("query")[0] : "";
            response.put("query", query);

            // nonlinked
            boolean nonlinked = req.get("nonlinked")[0] != null ? Boolean.valueOf(req.get("nonlinked")[0]) : false;
            response.put("nonlinked", String.valueOf(nonlinked));

            List<String> filter = new ArrayList<>(getValues(req, "filter[]"));

            List<Pair<Resource, Integer>> concepts = MapUtils.toList(Concept.searchConcepts(query, filter, nonlinked));
            response.put("main", content_index_concept_filter.render(concepts).toString());

            return ok(Json.toJson(response));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
    }


    public static Result getTypeAhead()
    {
        List<Pair<Resource, Integer>> concepts = MapUtils.toList(Concept.searchConcepts(null, new ArrayList<String>(), false));
        List<String> autocomplete = new ArrayList<>();
        for(Pair<Resource, Integer> p : concepts) {
            autocomplete.add(("concept:" + ResUtils.createShortURI(p.getLeft().toString()).split(":")[1]));
        }
        return ok(Json.toJson(autocomplete));
    }


    private static Set<String> getValues(Map<String, String[]> req, String key)
    {
    	
        Set<String> values = new HashSet<>();

        if(req.containsKey(key)) {
            String[] elements = req.get(key);
            for(int i = 0; i < elements.length; i++) {
                if(elements[i].length() > 0) {
                    values.add(elements[i]);
                }
            }
        }
          return values;
    }


    public static Result getInstances()
    {
        try {
            Map<String, String[]> req = request().queryString();
            Map<String, Object> response = new HashMap<>();

            // query
            String query = req.get("query")[0] != null ? req.get("query")[0].trim() : "";
            response.put("query", query);

            // page
            int page = req.get("page")[0] != null ? Integer.valueOf(req.get("page")[0]) : 1;
            response.put("page", String.valueOf(page));

            // nonlinked
            boolean nonlinked = req.get("nonlinked")[0] != null ? Boolean.valueOf(req.get("nonlinked")[0]) : false;
            response.put("nonlinked", String.valueOf(nonlinked));

            // filter and instances
            Set<EntityContainer> result = new TreeSet<>();
            if(!req.containsKey("filter[]")) {
                result.addAll(Content.searchContent(query, null, page, nonlinked));
            } else {
                List<String> filter = new ArrayList<>(getValues(req, "filter[]"));
                response.put("filter", filter);
                result.addAll(Content.searchContent(query, filter, page, nonlinked));
            }

            if(!result.isEmpty()) {
                response.put("main", content_index_indiviudals.render(new ArrayList<>(result), page).toString());
                response.put("lastPage", String.valueOf(false || (result.size() < Settings.I_PAGE_SIZE)));
            } else {
                response.put("lastPage", String.valueOf(true));
                // last page has Settings.I_PAGE_SIZE) -> load page-1
                if(page > 1) {
                    page -= 1;
                    if(!req.containsKey("filter[]")) {
                        result.addAll(Content.searchContent(query, null, page, nonlinked));
                    } else {
                        result.addAll(Content.searchContent(query, new ArrayList<>(getValues(req, "filter[]")), page, nonlinked));
                    }
                    response.put("main", content_index_indiviudals.render(new ArrayList<>(result), page).toString());
                }
                if(result.isEmpty()) {
                    page = 0;
                }
                response.put("page", String.valueOf(page));
            }
            System.out.println("--------------------------------------------------------------------------------------");
            System.out.println("here: "+ Json.toJson(response));
            return ok(Json.toJson(response));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
    }


    @SuppressWarnings ("unchecked")
    public static Result getBasicProperties()
    {
        try {
            Map<String, String[]> req = request().queryString();
            if(!req.containsKey("id[]")) { return noContent(); }
            Set<String> ids = getValues(req, "id[]");

            Map<String, Map<String, Object>> instanceMap = new HashMap<>();
            for(String id : ids) {
                EntityContainer ec = new EntityContainer(ResUtils.createResource(id));
                ec.loadBasicProperties();
                Map<String, Object> props = new HashMap<>();

                // cluster names
                Map<String, Set<String>> clusturedNames = new HashMap<>(), names = ec.getReadableNamesMap();
                BidiMap map = new DualTreeBidiMap();
                for(String prop : names.keySet()) {
                    for(String n : names.get(prop)) {
                        if(!map.containsValue(n)) {
                            map.put(prop, n);
                        } else {
                            String pTemp = (String) map.getKey(n);
                            map.remove(pTemp);
                            pTemp += ", " + prop;
                            map.put(pTemp, n);
                        }
                    }
                }

                clusturedNames.putAll(map);

                props.put("name", map);
                props.put("concepts", ec.getBasicConceptMap());
                List<String> sameAsCluster = new ArrayList<>(new TreeSet<>(ec.getSameAsRelation()));
                props.put("same_as", sameAsCluster);
                props.put("same_as_short", ResUtils.createShortURI(sameAsCluster));
                instanceMap.put(id, props);
            }
            Map<String, JsonNode> response = new HashMap<>();
            response.put("data", Json.toJson(instanceMap));
            response.put("query", Json.toJson(req.get("query")[0]));

            response.put("filter", Json.toJson(new ArrayList<>(getValues(req, "filter[]"))));
            response.put("nonlinked", Json.toJson(Boolean.valueOf(req.get("nonlinked")[0])));

            response.put("namespace", Json.toJson(Namespace.getNamespacesAsMap()));

            return ok(Json.toJson(response));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
    }


    public static Result show(String id)
    {
        try {
            if(ResUtils.toLongURI(id).contains("dbpedia")) {
                List<String> sameAs = new ArrayList<>(Content.getSameAsID(id));
                if(sameAs.size() > 1) {
                    sameAs.remove(id);
                    id = sameAs.get(0);
                }
            }

            EntityContainer desc = Content.show(id);

            if(desc == null) { return ok(displayError.render("Oh snap! An error occured!", 4044, "The entity container is null (" + id + ").")); }

            List<List<PropertyContainer>> list = new LinkedList<List<PropertyContainer>>();
            List<PropertyContainer> leftCol = new LinkedList<PropertyContainer>();
            List<PropertyContainer> rightCol = new LinkedList<PropertyContainer>();

            int i = 0;
            for(PropertyContainer pair : desc.getProperties()) {
                if(i % 2 == 0) {
                    leftCol.add(pair);
                } else {
                    rightCol.add(pair);
                }
                i++;
            }

            list.add(leftCol);
            list.add(rightCol);

            String name;
            if(desc.getReadableNames().size() == 0) {
                name = id;
            } else {
                name = desc.getInstanceName();
            }

            return ok(content_entity.render(id, name, list, desc.getStates()));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
    }

}