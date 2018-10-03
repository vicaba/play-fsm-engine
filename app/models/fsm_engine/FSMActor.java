package models.fsm_engine;

import akka.actor.AbstractActor;
import models.fsm_entities.State;

public class FSMActor extends AbstractActor {
	private FSMEngine fsmEngine;

	private HTTPClient httpClient;

	public FSMActor(HTTPClient httpClient, FSMEngine fsmEngine) throws Exception {
		this.httpClient = httpClient;
		this.fsmEngine = fsmEngine;
	}

	public void destroy() throws Exception {
		httpClient.stop();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(ChangeStateMessage.class, changeStateMsg -> {
					//Case where we receive a Change FSMEntities.State Message
					fsmEngine.onStateChange();
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
				.matchAny(o -> System.out.println("received unknown message"))
				.build();
	}
}
