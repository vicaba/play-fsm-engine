package infrastructure.controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import application.fsm.FSMActor;
import application.fsm.GetDataMessage;
import application.fsm.SendDataMessage;
import infrastructure.BodyParser;
import play.mvc.*;
import scala.concurrent.Await;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class GetDataController extends Controller {
	private final ActorSystem actorSystem;

	@Inject
	public GetDataController(ActorSystem actorSystem) {
		this.actorSystem = actorSystem;
	}

	public Result index(String actorId) {
		try {
			ActorRef fsmActor = FSMActor.findActorById(actorSystem, actorId);

			String body = BodyParser.parseRawBuffer(request().body().asRaw());

			var timeout = new Timeout(5, TimeUnit.SECONDS);

			var future = Patterns.ask(fsmActor, new GetDataMessage(body), timeout);

			var result = "";
			try {
				result = (String) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
				result = "Bad SELECT query";
			}

			return ok(result).withHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		} catch (Exception e) {
			return badRequest("FSM not found");
		}
	}
}