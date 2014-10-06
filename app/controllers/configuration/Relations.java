package controllers.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.entity.Value;
import play.mvc.Controller;
import play.mvc.Result;
import settings.Settings;
import views.html.configuration.configuration_relations;


public class Relations extends Controller
{
    public static Result show()
    {
        Map<String, List<Value>> relations = new HashMap<>();
        relations.put("1", Settings.REL_INSTANCE);
        relations.put("2", Settings.REL_CONCEPT);
        relations.put("3", Settings.REL_PROPERTY);
        return ok(configuration_relations.render(relations));
    }


    public static Result add()
    {
        Map<String, String[]> req = request().queryString();
        int type = req.get("type")[0] != null ? Integer.valueOf(req.get("type")[0]) : -1;
        String url = req.get("url")[0] != null ? req.get("url")[0] : "";
        String desc = req.get("desc")[0] != null ? req.get("desc")[0] : "";
        System.out.println("REL-ADD: " + type + " " + url + " - " + desc);

        switch(type) {
            case 1:
                add(Settings.REL_INSTANCE, url, desc);
            break;
            case 2:
                add(Settings.REL_CONCEPT, url, desc);
            break;
            case 3:
                add(Settings.REL_PROPERTY, url, desc);
            break;
            default:
            break;
        }
        Settings.saveSettings();
        return ok(type + " " + url + " - " + desc);
    }


    private static void add(List<Value> list, String url, String desc)
    {
        for(Value v : list) {
            if(v.getValue().equals(url)) {
                v.setDescription(desc);
                return;
            }
        }
        list.add(new Value(url, desc, 0.0f));
    }


    public static Result remove()
    {
        Map<String, String[]> req = request().queryString();
        int type = req.get("type")[0] != null ? Integer.valueOf(req.get("type")[0]) : -1;
        String url = req.get("url")[0] != null ? req.get("url")[0] : "";
        String desc = req.get("desc")[0] != null ? req.get("desc")[0] : "";
        System.out.println("REL-RM: " + type + " " + url + " - " + desc);

        switch(type) {
            case 1:
                remove(Settings.REL_INSTANCE, url);
            break;
            case 2:
                remove(Settings.REL_CONCEPT, url);
            break;
            case 3:
                remove(Settings.REL_PROPERTY, url);
            break;
            default:
            break;
        }
        Settings.saveSettings();
        return ok(type + " " + url + " - " + desc);
    }


    private static void remove(List<Value> list, String url)
    {
        Value tmp = null;
        for(Value v : list) {
            if(v.getValue().equals(url)) {
                tmp = v;
            }
        }
        if(tmp != null) {
            list.remove(tmp);
        }
    }
}
