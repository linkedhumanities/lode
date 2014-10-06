package controllers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import models.entity.Value;
import models.states.Endpoint;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.Result;
import sparql.Sparql;
import utils.ResUtils;
import views.html.eval;
import views.html.index;
import views.html.jqueryui;
import views.html.jstree;
import views.html.misc.index_about;
import views.html.misc.index_dev;
import views.html.misc.index_help;

import com.hp.hpl.jena.query.QuerySolution;


public class Main extends Controller
{

    public static Result index()
    {
        return ok(index.render("linked open data enhancer"));
    }


    public static Result about()
    {
        return ok(index_about.render("About"));
    }


    public static Result help()
    {
        return ok(index_help.render("Help"));
    }


    public static Result dev()
    {
        return ok(index_dev.render("Dev"));
    }


    public static Result jqueryui()
    {
        return ok(jqueryui.render());
    }


    public static Result jstree()
    {
        return ok(jstree.render("jstree"));
    }


    public static Result dbTest()
    {
        // Web Console server running at http://192.168.0.10:8082 (only local connections)
        // TCP server running at tcp://192.168.0.10:9092 (only local connections)
        // PG server running at pg://192.168.0.10:5435 (only local connections)
        // jdbc:h2:tcp://localhost/file:lode
        try {
            // DataSource ds = DB.getDataSource();
            Connection connection = DB.getConnection();

            String createString;
            // createString = "create table LODESettings (LS_ID VARCHAR(20), LS_VALUE VARCHAR(20))";
            createString = "insert into LODESettings values('test', '5')";

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(createString);

            stmt.close();
            connection.close();

        } catch(SQLException ex) {
            System.err.println("SQLException: " + ex.getMessage());
        }

        return ok();
    }


    public static Result eval() throws Exception
    {
        TreeSet<String> links = new TreeSet<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("links2.txt"), "UTF-8"));
        String line = null;

        while((line = br.readLine()) != null) {
            links.add(line);
        }

        List<Map<String, String>> map = new ArrayList<>();
        for(String s : links.descendingSet()) {
            Value v = new Value(s);
            Map<String, String> tmp = new HashMap<>();
            tmp.put("uri", s);
            tmp.put("suri", ResUtils.createShortURI(v.getValue()));

            String query = "SELECT DISTINCT ?dbp {" + v.toSparql() + " owl:sameAs ?dbp. FILTER ( regex(str(?dbp),\"http://dbpedia.org/resource/\",\"i\") ). }";

            List<QuerySolution> res = Sparql.select(query, Endpoint.LOCAL);

            if(res.size() == 0) {
                tmp.put("state", "FALSE");
            } else {
                for(QuerySolution qs : res) {
                    Value value = new Value(qs.get("?dbp").toString());
                    tmp.put("state", ResUtils.createShortURI(value.getValue()));
                    break;
                }
            }

            map.add(tmp);

        }
        br.close();
        return ok(eval.render(map));
    }
}