package controllers.enhancers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.entity.EntityContainer;
import models.entity.PropertyContainer;
import models.entity.Value;
import play.mvc.Controller;
import play.mvc.Result;
import settings.Settings;
import sparql.query.Content;
import sparql.update.Delete;
import utils.ResUtils;
import views.html.enhancers.rm_links;


public class MainController extends Controller
{

    public static Result rmLinks(String id)
    {
        EntityContainer e = Content.show(id);
        if(ResUtils.toLongURI(id).contains("dbpedia")) {
            List<String> sameAs = new ArrayList<>(Content.getSameAsID(id));
            if(sameAs.size() > 1) {
                sameAs.remove(id);
                id = sameAs.get(0);
            }
        }

        List<Map<String, String>> links = new ArrayList<>();
        for(Value property : Settings.REL_INSTANCE) {
            PropertyContainer relation = e.getProperty(property.getValue());
            if(relation != null) {
                for(Value value : relation.getValuesByLanguage("")) {
                    String id2 = ResUtils.createShortURI(value.getValue());
                    if(!id.equalsIgnoreCase(id2)) {
                        Map<String, String> info = new HashMap<>();
                        info.put("id", id);
                        info.put("id-value", e.getURI().getValue());
                        info.put("property", ResUtils.createShortURI(relation.getPropertyURI()));
                        info.put("property-value", relation.getPropertyURI());
                        info.put("value", id2);
                        info.put("value-value", value.getValue());
                        links.add(info);
                    }
                }
            }
        }

        String title;
        if(e.getReadableNames().size() == 0) {
            title = id;
        } else {
            title = e.getInstanceName();
        }

        return ok(rm_links.render(title + ": Remove Links", id, links));
    }


    public static Result rmLinksConfirm(String id)
    {
        Map<String, String[]> req = request().queryString();
        Map<String, Map<String, String>> query = new HashMap<>();

        Pattern p = Pattern.compile(".*\\[([0-9]*)\\]\\[(.*)\\].*");

        for(String s : req.keySet()) {
            Matcher m = p.matcher(s);
            if(m.find()) {
                String number = m.group(1);
                String feature = m.group(2);
                String value = req.get(s)[0];

                if(query.containsKey(number)) {
                    query.get(number).put(feature, value);
                } else {
                    Map<String, String> temp = new HashMap<>();
                    temp.put(feature, value);
                    query.put(number, temp);
                }
            }
        }

        for(String s : query.keySet()) {
            Map<String, String> link = query.get(s);
            Delete.deleteProperty(new Value(link.get("id")), new Value(link.get("property")), new Value(link.get("value")));
            Delete.deleteProperty(new Value(link.get("value")), new Value(link.get("property")), new Value(link.get("id"))); // Workaround
        }

        return controllers.content.MainController.show(id);
    }


    public static Result rmLink()
    {
        Map<String, String[]> req = request().queryString();
        Value instance = new Value(req.get("instance")[0].trim());
        Value property = new Value(req.get("property")[0].trim());
        Value value = new Value(req.get("value")[0].trim());

        Delete.deleteProperty(instance, property, value);
        Delete.deleteProperty(value, property, instance); // Workaround

        return ok();
    }
}