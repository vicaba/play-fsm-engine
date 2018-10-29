package infrastructure.controllers;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import infrastructure.fsm.WebSocketActor;
import play.libs.streams.ActorFlow;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import infrastructure.views.html.*;

import javax.inject.Inject;

public class FsmClientController extends Controller {
	private final ActorSystem actorSystem;
	private final Materializer materializer;

	@Inject
	public FsmClientController(ActorSystem actorSystem, Materializer materializer) {
		this.actorSystem = actorSystem;
		this.materializer = materializer;
	}

	public Result createFsmClient(String actorId) {
		return ok(fsm_client_view.render(actorId));
	}

	public WebSocket startWebSocket() {
		return WebSocket.Json.accept(request ->
													  ActorFlow.actorRef(WebSocketActor::props,
																				actorSystem, materializer
													  )
		);
	}
}
