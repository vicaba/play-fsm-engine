package infrastructure.controllers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import play.mvc.*;
import infrastructure.BodyParser;

import java.io.StringReader;

public class CheckCompatibilityController extends Controller {

	public CheckCompatibilityController() {
		
	}

	public Result index() {
		try {
			String body = BodyParser.parseRawBuffer(request().body().asRaw());

			Model model = ModelFactory.createDefaultModel();
			RDFDataMgr.read(model, new StringReader(body), null, Lang.TTL);

			String basePrefix = "@prefix : <" + model.getNsPrefixURI("") + "> . " ;
			String selfPrefix = "@prefix self: <" + model.getNsPrefixURI("self") + "> . " ;
			String fsmPrefix = "@prefix fsm: <file:///D:/projects/ontologies/fsm/fsm#> . ";
			String siotPrefix = "@prefix siot: <file:///D:/projects/ontologies/siot/siot#> . ";

			String rdfResponse = basePrefix + selfPrefix + fsmPrefix + siotPrefix +
					"self: siot:hasPeer :peer20 . " +
					"self: siot:hasCompatibility :compatibility1 . " +
					":compatibility1 siot:withPeer :peer20 . " +
					":compatibility1 fsm:hasContent \"true\" ";
			
			return ok(rdfResponse);
		} catch (Exception e) {
			return badRequest("FSM not found");
		}
	}
}
