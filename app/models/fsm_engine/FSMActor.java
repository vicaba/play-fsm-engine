package models.fsm_engine;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import models.fsm_entities.State;
import models.fsm_websocket.NotifyStatusChange;

public class FSMActor extends AbstractActor {
	private FSMEngine fsmEngine;
	private HTTPClient httpClient;
	private ActorRef notifierActor;

	public FSMActor(HTTPClient httpClient, FSMEngine fsmEngine) {
		this.httpClient = httpClient;
		this.fsmEngine = fsmEngine;
		this.notifierActor = null;
	}

	public void destroy() {
		httpClient.stop();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(ChangeStateMessage.class, changeStateMsg -> {
					//Case where we receive a Change FSMEntities.State Message
					fsmEngine.onStateChange();
					sendNotification("State changed -> " + fsmEngine.getActualState().getLocalName());
					this.self().tell(new CheckConditionsMessage(), this.self());
				})
				.match(CheckConditionsMessage.class, checkConditionMsg -> {
					//Case where we receive a Check FSMEntities.Condition Message
					State nextState = fsmEngine.checkTransitions();
					if (nextState == null) {
						System.out.println("All conditions false");
					} else {
						this.self().tell(new ChangeStateMessage(), this.self());
					}
				})
				.match(EstablishConnectionMessage.class, establishConnectionMsg -> {
					notifierActor = sender();
				})
				.matchAny(o -> System.out.println("received unknown message"))
				.build();
	}

	private void sendNotification(String message) {
		if (notifierActor != null) {
			notifierActor.tell(new NotifyStatusChange(message), self());
		}
	}
}
