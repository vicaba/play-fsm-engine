package controllers;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import models.fsm_websocket.WebSocketActor;
import play.libs.streams.ActorFlow;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

import javax.inject.Inject;
import java.io.File;

public class FsmClientController extends Controller {
	private final ActorSystem actorSystem;
	private final Materializer materializer;

	@Inject
	public FsmClientController(ActorSystem actorSystem, Materializer materializer) {
		this.actorSystem = actorSystem;
		this.materializer = materializer;
	}

	public Result startWebSocket(String actorId) {
		return ok("Para empezar el websocket");
	}

	public WebSocket createFsmClient() {


		return WebSocket.Text.accept(request ->
													  ActorFlow.actorRef(WebSocketActor::props,
																				actorSystem, materializer
													  )
		);
	}
}
