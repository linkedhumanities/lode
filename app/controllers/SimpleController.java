package controllers;

import models.states.Endpoint;
import play.mvc.Controller;
import play.mvc.Result;
import sparql.Sparql;
import utils.StringUtils;
import views.html.displayError;

import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class SimpleController extends Controller
{

    public static Result index()
    {
        try {
            return ok(Sparql.select("SELECT Distinct ?x WHERE {?y rdf:type ?x}", Endpoint.LOCAL).toString());
        } catch(QueryExceptionHTTP e) {
            return ok(displayError.render("Oh snap! An error occured!", 4041, StringUtils.getStackTrace(e)));
        }
    }
}