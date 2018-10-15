package models.fsm_engine;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import models.fsm_entities.State;
import models.fsm_websocket.NotifyStatusChangedMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FSMActor extends AbstractActor {
	private FSMEngine fsmEngine;
	private HTTPClient httpClient;
	private ActorRef notifierActor;

	private List<NotifyStatusChangedMessage> statusChangeList;

	public FSMActor(HTTPClient httpClient, FSMEngine fsmEngine) {
		this.httpClient = httpClient;
		this.fsmEngine = fsmEngine;
		this.notifierActor = null;
		statusChangeList = new ArrayList<>();
	}

	@Override
	public void preStart() throws Exception {
		super.preStart();
		//self().tell("Hola", self());
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
					sendNotification("stateChanged", "State changed -> " + fsmEngine.getActualState().getLocalName());
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
					System.out.println("Mensaje recibido en fsm");
					notifierActor = sender();
					sendNotification("stateChanged", "Actual state: " + fsmEngine.getActualState().getLocalName());
				})
				.matchAny(o -> {
					System.out.println("he recibido algo " + o.toString());
				} )
				.build();
	}

	private void sendNotification(String type, String message) {
		if (notifierActor != null) {
			if (statusChangeList.size() > 0) {
				statusChangeList.forEach(m -> notifierActor.tell(m, self()));
				statusChangeList.clear();
			}

			notifierActor.tell(new NotifyStatusChangedMessage(type, message), self());
		} else {
			statusChangeList.add(new NotifyStatusChangedMessage(type, message));
		}
	}
}
