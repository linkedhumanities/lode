package controllers.enhancers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import models.akka.messages.ConfigMessage;
import models.akka.messages.ErrorMessage;
import models.akka.messages.ResultMessage;
import models.akka.messages.StatusMessage;
import models.entity.EntityContainer;
import models.entity.PropertyContainer;
import models.entity.Value;
import models.states.Endpoint;

import org.apache.commons.lang3.StringEscapeUtils;

import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.Await;
import scala.concurrent.Future;
import settings.Settings;
import sparql.query.Content;
import sparql.update.Linker;
import utils.ResUtils;
import utils.StringUtils;
import utils.ViewUtils;
import views.html.displayError;
import views.html.enhancers.link_entity;
import views.html.enhancers.wikiStat;
import views.html.helpers.displayProperty;
import views.html.helpers.linkerProgress;
import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Identify;
import akka.actor.Props;
import akka.pattern.AskableActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class LinkerController extends Controller
{
    public static Result link(String source, String id) throws QueryExceptionHTTP
    {
        if(ResUtils.toLongURI(id).contains("dbpedia")) {
            List<String> sameAs = new ArrayList<>(Content.getSameAsID(id));
            if(sameAs.size() > 1) {
                sameAs.remove(id);
                id = sameAs.get(0);
            }
        }
        try {
            Map<String, String[]> req = request().queryString();
            int method = !req.containsKey("method") ? 0 : Integer.valueOf(req.get("method")[0]);

            ActorRef myActor = Akka.system().actorOf(Props.create(models.akka.LinkerActor.class));
            myActor.tell(new ConfigMessage(id, source, method), null);

            return ok(linkerProgress.render(source, myActor.path().name()));
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        } catch(NullPointerException e) {
            return ok(displayError.render("Oh snap! An error occured!", 4042, StringUtils.getStackTrace(e)));
        }
    }


    public static Promise<Result> status(String uuid)
    {
        try {
            uuid = "akka://application/user/" + uuid;
            ActorSelection selection = Akka.system().actorSelection(uuid);
            AskableActorSelection asker = new AskableActorSelection(selection);
            Timeout t = new Timeout(5, TimeUnit.SECONDS);
            Future<Object> fut = asker.ask(new Identify(1), t);
            ActorIdentity ident = (ActorIdentity) Await.result(fut, t.duration());
            ActorRef myActor = ident.getRef();

            if(myActor == null) { throw new Exception("java.lang.IllegalArgumentException: Unsupported recipient ActorRef type, question not sent to [null]"); }

            Promise<Result> promise = Promise.wrap(Patterns.ask(myActor, new StatusMessage(), 3000)).map(new F.Function<Object, Result>() {
                public Result apply(Object response)
                {
                    if(response instanceof ResultMessage) {
                        ResultMessage rm = (ResultMessage) response;
                        return ok(link_entity.render(rm.getSource(), rm.getDesc(), rm.getSuggestions(), ((ResultMessage) response).getMethod()));
                    }

                    if(response instanceof ErrorMessage) {
                        ErrorMessage em = (ErrorMessage) response;
                        return (Result) ok(displayError.render("Oh snap! An error occured!", em.getErrorCode(), StringUtils.getStackTrace(em.getError())));
                    }

                    return ok(response.toString());
                }
            });

            return promise;
        } catch(Exception e) {
            return Promise.pure((Result) ok(displayError.render("Oh snap! An error occured!", 4043, StringUtils.getStackTrace(e))));
        }
    }


    public static Result setLink()
    {
        // get data
        Map<String, String[]> req = request().queryString();
        Value local = new Value(req.get("local")[0]);
        Value remote = new Value(req.get("remote")[0]);
        Value relation = new Value(req.get("relation")[0]);

        // store relation in database
        Linker.setLink(local, remote, relation);

        // reload properties of local instance
        StringBuilder sb = new StringBuilder();
        EntityContainer ec = Content.getEntityContainerbyID(ResUtils.createShortURI(local.getValue()));
        for(PropertyContainer pc : ec.getProperties()) {
            sb.append(displayProperty.render(pc.getPropertyURI(), pc).toString());
        }

        // result
        return ok(sb.toString());
    }


    public static Result show()
    {
        // get data
        Map<String, String[]> req = request().queryString();
        String id = req.get("id")[0].trim();
        String method = req.get("method")[0].trim();
        Endpoint source = Endpoint.valueOf(req.get("source")[0].trim().toUpperCase());
        int pagenumber = Integer.valueOf(req.get("pagenumber")[0].trim());

        // build key
        String key = id + method;

        // load proposals
        List<EntityContainer> proposals = Linker.show(key, source, null, pagenumber);

        // build HTML code
        StringBuilder html = new StringBuilder();
        html.append("<div id=\"proposals-entries\" style=\"display: none;\">");
        for(EntityContainer proposal : proposals) {
            html.append("<div id=\"" + proposal.getShortURI() + "\" data-uri-short=\"" + proposal.getShortURI() + "\" data-uri-long=\"" + proposal.getURI().getValue() + "\" class=\"alert alert-success drag\" style=\"cursor:pointer;\">");
            html.append("<span style=\"font-weight:bold\">" + proposal.getShortURI() + "</span>");
            html.append("<span style=\"float: right;\"><button id=\"button-" + proposal.getShortURI() + "\" type=\"button\" class=\"btn btn-mini btn-info\" onclick=\"showMore('" + proposal.getShortURI() + "', this)\" data-toggle=\"button\" rel=\"popover\" data-html=\"true\" data-content=\""
                    + StringEscapeUtils.escapeHtml4(ViewUtils.compareDescriptionDialog(Content.getEntityContainerbyID(id), proposal)) + "\" data-original-title=\"Description\" data-placement=\"bottom\">Show More</button></span>");
            html.append("</div>");

            html.append("<div id=\"content-" + proposal.getShortURI() + "\" style=\"padding: 0 1.5em; display:none;\">");
            for(PropertyContainer pc : proposal.getProperties()) {
                html.append(displayProperty.render(pc.getPropertyURI(), pc).toString());
            }
            html.append("</div>");
        }

        if(proposals.size() % 5 != 0 || (Linker.show(key, source, null, pagenumber + 1)).size() == 0) {
            html.append("<span data-info=\"complete\"></span>");
        }

        return ok(html.toString());
    }


    public static Result wikiStat() throws Exception
    {
        String user = Settings.MySQL_USER;
        String password = Settings.MySQL_PASSWORD;

        String database = "wikiStat";
        String table = "wikiPrep";
        // columns: uri, sf count

        Class.forName("com.mysql.jdbc.Driver");
        // Setup the connection with the DB
        Connection connect = DriverManager.getConnection("jdbc:mysql://" + Settings.MySQL_SERVER + "/" + database + "?user=" + user + "&password=" + password);

        Statement stmt = connect.createStatement();
        // query
        Map<String, String[]> req = request().queryString();
        String query = !req.containsKey("query") ? null : req.get("query")[0];

        if(query == null) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " ORDER BY count desc LIMIT 10");
            List<String[]> result = new ArrayList<>();
            while(rs.next()) {
                result.add(new String[] { rs.getString("sf"), rs.getString("uri"), rs.getString("count") });
            }
            connect.close();
            return ok(wikiStat.render("wikiStat", result));
        } else {
            StringBuilder sb = new StringBuilder();
            ResultSet rs = null;
            if(query.length() == 0) {
                rs = stmt.executeQuery("SELECT * FROM " + table + " ORDER BY count desc LIMIT 10");
            } else {
                // PreparedStatement stmt2 = connect.prepareStatement("SELECT * FROM " + table + " WHERE sf like ? OR uri like ? ORDER BY count desc");
                // stmt2.setString(1, query + "%"); // "%" query + "%"
                // stmt2.setString(2, query + "%");
                PreparedStatement stmt2 = connect.prepareStatement("SELECT * FROM " + table + " WHERE sf = ? OR uri = ? ORDER BY count desc");
                stmt2.setString(1, query);
                stmt2.setString(2, query);
                rs = stmt2.executeQuery();
            }
            while(rs.next()) {
                sb.append("<tr><td>" + rs.getString("sf") + "</td><td>" + rs.getString("uri") + "</td><td>" + rs.getString("count") + "</td></tr>");
            }
            connect.close();
            return ok(sb.toString());
        }

    }
}