package infrastructure.controllers;

import play.mvc.*;
import infrastructure.BodyParser;

public class CheckCompatibilityController extends Controller {

	public CheckCompatibilityController() {
	}

	public Result index() {
		try {
			String body = BodyParser.parseRawBuffer(request().body().asRaw());
			System.out.println("Body");

			return ok("self: fsm:hasCompatibility :compatibility1 . :compatibility1 siot:with Peer :peer20 . :compatibility1 fsm:hasContent \"true\"");
		} catch (Exception e) {
			return badRequest("FSM not found");
		}
	}
}
