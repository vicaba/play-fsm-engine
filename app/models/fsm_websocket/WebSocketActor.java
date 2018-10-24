package models.fsm_websocket;

import akka.actor.*;
import com.fasterxml.jackson.databind.JsonNode;
import models.fsm_engine.EstablishConnectionMessage;
import models.fsm_engine.FSMEngine;

import java.time.Duration;
import java.util.UUID;


public class WebSocketActor extends AbstractActor {
	public static Props props(ActorRef out) {
		return Props.create(WebSocketActor.class, out);
	}

	private final ActorRef out;

	public WebSocketActor(ActorRef out) {
		this.out = out;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()


				.match(JsonNode.class, message -> {
					if (!message.has("type") || !message.findPath("type").textValue().equals("connection_request")) {
						tellClient(new NotifyStatusChangedMessage("bad_message", "I've received a bad message"));
						return;
					}

					UUID uuid = UUID.fromString(message.findPath("wsId").textValue());

					String actorName = "user/" + FSMEngine.generateActorName(uuid);

					ActorRef fsmActor = getContext().getSystem().actorSelection(actorName).resolveOneCS(Duration.ofDays(1)).toCompletableFuture().join();

					fsmActor.tell(new EstablishConnectionMessage(), self());
				})


				.match(NotifyStatusChangedMessage.class, this::tellClient)


				.matchAny(m -> {
					System.out.println("he recibido " + m.getClass().toString());
					tellClient(new NotifyStatusChangedMessage("bad_message", "I've recieved a bad message" + m.toString()));
				})


				.build();
	}

	private void tellClient(NotifyStatusChangedMessage message) {
		out.tell(message.toJson(), self());
	}
}
