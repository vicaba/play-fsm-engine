package models.fsm_websocket;

import akka.actor.*;
import models.fsm_engine.EstablishConnectionMessage;
import models.fsm_engine.FSMEngine;

import javax.inject.Inject;
import java.util.UUID;

public class WebSocketActor extends AbstractActor {
	public static Props props(ActorRef out) {
		return Props.create(WebSocketActor.class, out);
	}

	private final ActorRef out;
	private @Inject ActorSystem actorSystem;

	public WebSocketActor(ActorRef out) {
		this.out = out;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(String.class, message -> {
					UUID uuid = UUID.fromString(message);
					
					String actorName = FSMEngine.generateActorName(uuid);

					ActorRef fsmActor = context().system().actorFor(actorName);

					fsmActor.tell(new EstablishConnectionMessage(), self());

					out.tell("I received your message: " + message, self());
				}).match(NotifyStatusChange.class, change -> {
					out.tell(change.getMessage(), self());
				}).build();
	}
}
