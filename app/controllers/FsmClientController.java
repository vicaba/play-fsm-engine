package controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.Materializer;
import models.fsm_websocket.WebSocketActor;
import play.libs.streams.ActorFlow;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.*;
import javax.inject.Inject;
import java.util.function.Function;

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
		System.out.println("He recibido una peticion para socket");
		return WebSocket.Json.accept(request ->
													  ActorFlow.actorRef(WebSocketActor::props,
																				actorSystem, materializer
													  )
		);
	}
}
