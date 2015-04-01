package sparql;

import java.util.LinkedList;
import java.util.List;

import models.states.Endpoint;

import org.apache.commons.lang.StringEscapeUtils;

import settings.Settings;
import utils.ResUtils;
import utils.StringUtils;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;


public class Sparql
{
    public static List<QuerySolution> select(String request, Endpoint endpoint) throws QueryExceptionHTTP
    {
        QueryExecution qexec = Sparql.prepare(request, endpoint);
       
        List<QuerySolution> result = null;

        ResultSet results = null;
        try {
            results = qexec.execSelect();
            result = ResultSetFormatter.toList(results);
        } catch(Exception e) {
            String report = StringUtils.getStackTrace(e);
            if(report.toLowerCase().contains("java.net.sockettimeoutexception")) {  // note catch(SocketTimeOutExcpetion e) is not possible
                return Sparql.select(request, endpoint);
            } else if(report.toLowerCase().contains("xmlstreamexception")) {
                System.out.println("XML Parsing Error. Skip Result.");
                result = new LinkedList<>();
            } else {
                System.out.println("SPARQL crash! \n"+ report);
                System.out.println("endpoint :"+endpoint.getDestination());
                System.out.println("---------query :"+qexec.getQuery().serialize());
                throw e;
            }
        }
        qexec.close();

        return result;
    }


    public static Boolean ask(String request, Endpoint endpoint)
    {
        QueryExecution qexec = Sparql.prepare(request, endpoint);

        boolean result = qexec.execAsk();
        qexec.close();

        return result;
    }


    public static void update(String command, Endpoint endpoint)
    {
        String update = convertPrefixToNamespace(command);
        update = StringEscapeUtils.unescapeHtml(update);

        UpdateRequest sparqlupdate = UpdateFactory.create(update);
        UpdateProcessor uexec = UpdateExecutionFactory.createRemoteForm(sparqlupdate, Settings.SERVER + "/openrdf-sesame/repositories/" + Settings.REPO + "/statements");

        uexec.execute();
    }


    private static QueryExecution prepare(String request, Endpoint endpoint)
    {
        request = convertPrefixToNamespace(request);
        request = StringEscapeUtils.unescapeHtml(request);
        request = request.replace("\n", "\\n");
        
        Query sparqlQuery = QueryFactory.create(request);
        
        QueryEngineHTTP qexec = QueryExecutionFactory.createServiceRequest(endpoint.getDestination(), sparqlQuery);

        if(Settings.DEFAULTGRAPH.containsKey(endpoint)) {
            qexec.addDefaultGraph(Settings.DEFAULTGRAPH.get(endpoint));
        }

        return qexec;
    }


    private static String convertPrefixToNamespace(String request)
    {
        String[] divided = request.split(" ");
        for(String part : divided) {
            part = part.trim();
            if(!(!part.startsWith("<") && !part.startsWith("'") && !part.endsWith(">") && !part.contains(":/") && !part.startsWith("\"") && !part.endsWith("\"") && !part.endsWith(":") && part.contains(":"))) {
                continue;
            }

            String[] cs = { "{", "(" };
            for(String c : cs) {
                if(part.contains(c)) {
                    part = part.substring(part.indexOf(c) + 1);
                }
            }

            String[] cs2 = { "}", ")" };
            for(String c : cs2) {
                if(part.contains(c)) {
                    part = part.substring(0, part.indexOf(c));
                }
            }

            request = request.replace(part, "<" + ResUtils.toLongURI(part) + ">");
        }

        return request;
    }
}