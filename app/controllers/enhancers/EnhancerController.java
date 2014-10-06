package controllers.enhancers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeSet;

import models.entity.EntityContainer;
import models.entity.PropertyContainer;
import models.entity.Value;
import models.states.LinkingState;

import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;

import play.mvc.Controller;
import play.mvc.Result;
import settings.Settings;
import sparql.query.Content;
import sparql.update.Enhancer;
import utils.MapUtils;
import utils.ResUtils;
import utils.StringUtils;
import utils.ViewUtils;
import views.html.displayError;
import views.html.enhancers.enhance_entity;


public class EnhancerController extends Controller
{
    public static Result enhance(String name, String target) throws QueryExceptionHTTP
    {
        try {
            if(ResUtils.toLongURI(name).contains("dbpedia")) {
                List<String> sameAs = new ArrayList<>(Content.getSameAsID(name));
                if(sameAs.size() > 1) {
                    sameAs.remove(name);
                    name = sameAs.get(0);
                }
            }

            EntityContainer desc = Content.show(name);
            if(desc == null) { return ok(displayError.render("Oh snap! An error occured!", 4044, "The entity container is null (" + name + ").")); }

            EntityContainer ec = Enhancer.enhance(name, target);
            if(ec == null) { return ok(displayError.render("Oh snap! An error occured!", 4044, "The entity container is null (" + name + " / " + target + ").")); }

            TreeSet<PropertyContainer> newData = new TreeSet<>(MapUtils.deepCopy(ec.getProperties()));
            NavigableMap<String, PropertyContainer> topProperties = ResUtils.getTopProperties(newData, Settings.E_PROPERTIES, Settings.E_VALUES);
            EntityContainer newEc = new EntityContainer(ec.getURI(), topProperties, LinkingState.UNKNOWN, ec.getDescription());

            return ok(enhance_entity.render(target, desc, newEc));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        } catch(NullPointerException e) {
            return ok(displayError.render("Oh snap! An error occured!", 4042, StringUtils.getStackTrace(e)));
        }
    }


    public static Result reloadContent(String name, String category, String position, Boolean normal)
    {
        EntityContainer entity = null;
        if(position.equals("remote") && Enhancer.getCache().containsKey(name)) {
            entity = Enhancer.getCache().get(name);
            if(normal) {
                NavigableSet<PropertyContainer> newData = new TreeSet<>(MapUtils.deepCopy(entity.getProperties()));
                NavigableMap<String, PropertyContainer> topProperties = ResUtils.getTopProperties(newData, Settings.E_PROPERTIES, Settings.E_VALUES);
                entity = new EntityContainer(entity.getURI(), topProperties, LinkingState.UNKNOWN, entity.getDescription());
            }
        } else if(position.equals("local") && Content.getEntityContainerbyID(name) != null) {
            entity = Content.getEntityContainerbyID(name);
        } else {
            return ok();
        }

        String content = ViewUtils.buildEnhancerBox(category, position, entity);

        if(content.contains("remote-loadmore") && !normal) {
            String partA = content.substring(0, content.indexOf("remote-loadmore"));
            partA = partA.substring(0, partA.lastIndexOf("<"));

            String partB = content.substring(content.indexOf("remote-loadmore"));
            partB = partB.substring(partB.indexOf("</dt>") + 5);

            content = partA + partB;
        }

        return ok(content.toString());
    }


    public static Result insert() throws QueryExceptionHTTP
    {
        Map<String, String[]> req = request().queryString();
        Value instance = new Value(req.get("local")[0].trim());
        Value property = new Value(req.get("remote")[0].trim());
        String data = req.get("value")[0].trim();

        String[] values = { data };
        if(data.contains("$##$!")) {
            values = data.split("[$]##[$][!]");
        }

        for(String value : values) {
            Enhancer.insertData(instance, property, new Value(value));
        }

        Content.show(ResUtils.createShortURI(instance.getValue()));

        return ok();
    }


    public static Result getValues(String name, String property)
    {
        EntityContainer ec = Enhancer.getCache().get(name);
        PropertyContainer values = ec.getProperty(ResUtils.toLongURI(property));

        StringBuilder sb = new StringBuilder();
        for(String lang : values.getLanguages()) {
            for(Value value : values.getValuesByLanguage(lang)) {
                sb.append("<span data-lang='" + value.getLanguage() + "' data-format='" + value.getDataType() + "'>" + value.getValue() + "</span>");
            }
        }

        return ok(sb.toString());
    }
}