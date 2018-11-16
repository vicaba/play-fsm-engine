package infrastructure.controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import application.fsm.ExecuteOperationMessage;
import application.fsm.FSMActor;
import infrastructure.BodyParser;
import play.mvc.*;

import javax.inject.Inject;

public class ExecuteOperationController extends Controller {
	private final ActorSystem actorSystem;

	@Inject
	public ExecuteOperationController(ActorSystem actorSystem) {
		this.actorSystem = actorSystem;
	}

	public Result index(String actorId) {
		try {
			ActorRef fsmActor = FSMActor.findActorById(actorSystem, actorId);

			String body = BodyParser.parseRawBuffer(request().body().asRaw());

			fsmActor.tell(new ExecuteOperationMessage(body), ActorRef.noSender());

			return ok();
		} catch (Exception e) {
			return badRequest("FSM not found");
		}
	}
}
