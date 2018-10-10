package models.fsm_websocket;

import akka.actor.*;
import models.fsm_engine.EstablishConnectionMessage;
import models.fsm_engine.FSMEngine;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionStage;



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
				.match(String.class, message -> {
					UUID uuid = UUID.fromString(message);

					String actorName = "user/" + FSMEngine.generateActorName(uuid);
					System.out.println("actor name = " + actorName);

					ActorRef fsmActor = getContext().getSystem().actorSelection(actorName).resolveOneCS(Duration.ofDays(1)).toCompletableFuture().join();
					System.out.println("sistema en websocket ->" + getContext().getSystem().hashCode());

					fsmActor.tell(new EstablishConnectionMessage(), self());

					//ActorRef fsmActor1 = getContext().actorSelection("akka://application/user/fsm*").resolveOneCS(Duration.ofDays(1)).toCompletableFuture().join();
					//System.out.println("fsm things = " + fsmActor1);

					//ActorRef fsmActor = getContext().actorSelection(actorName).resolveOneCS(Duration.ofDays(1)).toCompletableFuture().join();

					//fsmActor.tell(EstablishConnectionMessage.class, self());
					System.out.println("despues del for");
					out.tell("I received your message: " + message, self());
				}).match(NotifyStatusChangedMessage.class, change -> {
					out.tell(change.getMessage(), self());
				}).match(ActorIdentity.class, m -> System.out.println("Actor ref ->" + m.getRef() + " y -> " + m.getActorRef().get()))
				.build();
	}
}
