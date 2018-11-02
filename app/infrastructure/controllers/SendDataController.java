package infrastructure.controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import application.fsm.FSMActor;
import application.fsm.SendDataMessage;
import infrastructure.BodyParser;
import play.mvc.*;

import javax.inject.Inject;

public class SendDataController extends Controller {
	private final ActorSystem actorSystem;

	@Inject
	public SendDataController(ActorSystem actorSystem) {
		this.actorSystem = actorSystem;
	}

	public Result index(String actorId) {
		try {
			ActorRef fsmActor = FSMActor.findActorById(actorSystem, actorId);

			String body = BodyParser.parseRawBuffer(request().body().asRaw());

			fsmActor.tell(new SendDataMessage(body), ActorRef.noSender());

			return ok("Content saved?!?!");
		} catch (Exception e) {
			return badRequest("FSM not found");
		}
	}
}
