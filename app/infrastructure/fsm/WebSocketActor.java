package infrastructure.fsm;

import akka.actor.*;
import application.fsm.CloseActorMessage;
import application.fsm.FSMActor;
import application.fsm.NotifyStatusChangedMessage;
import com.fasterxml.jackson.databind.JsonNode;
import domain.fsm.engine.EstablishConnectionMessage;
import domain.fsm.engine.FSMEngine;

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
					if (!message.has("type")) {
						tellClient(new NotifyStatusChangedMessage("bad_message", "I've received a bad message"));
						return;
					}
					String type = message.findPath("type").textValue();


					switch (type) {
						case "connection_request":
							String actorId = message.findPath("wsId").textValue();

							ActorRef fsmActor = FSMActor.findActorById(getContext().getSystem(), actorId);

							fsmActor.tell(new EstablishConnectionMessage(), self());
							break;
						case "ping":
							tellClient(new NotifyStatusChangedMessage("pong", "Ping response"));
							break;
						default:
							tellClient(new NotifyStatusChangedMessage("bad_message", "I've received a bad message"));
							break;
					}
				})

				.match(NotifyStatusChangedMessage.class, this::tellClient)

				.match(CloseActorMessage.class, m -> {
					tellClient(new NotifyStatusChangedMessage("fsm_ended", "The fsm execution has finished"));
					self().tell(PoisonPill.getInstance(), self());
				})

				.matchAny(m -> {
					tellClient(new NotifyStatusChangedMessage("bad_message", "I've received a bad message" + m.toString()));
				})

				.build();
	}

	private void tellClient(NotifyStatusChangedMessage message) {
		out.tell(message.toJson(), self());
	}
}
