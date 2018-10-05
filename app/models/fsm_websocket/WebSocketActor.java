package models.fsm_websocket;

import akka.actor.*;

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
				.match(String.class, message ->
						out.tell("I received your message: " + message, self())
				)
				.build();
	}
}
