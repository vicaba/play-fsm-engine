package models.fsm_engine;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import models.Tuple2;
import models.fsm_entities.Action;
import models.fsm_entities.State;
import models.fsm_websocket.NotifyStatusChangedMessage;

import java.util.ArrayList;
import java.util.List;

public class FSMActor extends AbstractActor {
	private FSMEngine fsmEngine;
	private ActorRef notifierActor;

	private List<NotifyStatusChangedMessage> statusChangeList;

	public FSMActor(FSMEngine fsmEngine) {
		this.fsmEngine = fsmEngine;
		this.notifierActor = null;
		statusChangeList = new ArrayList<>();
	}

	/*
	//TODO: implement destroy to stop an FSM_Actor
	public void destroy() {
		httpClient.stop();
	}
	*/

	@Override
	public Receive createReceive() {
		return receiveBuilder()


				.match(OnStateChangeMessage.class, changeStateMsg -> {
					//Case where we receive a Change FSMEntities.State Message

					sendNotification("stateChanged", "New state: " + fsmEngine.getActualState().getLocalName());

					fsmEngine.prepareNewState();

					List<Action> entryActions = fsmEngine.getActualState().getEntryActions();
					if (entryActions.isEmpty()) {
						sendNotification("otherInfo", "There aren't any entry actions");
					} else {
						sendNotification("otherInfo", "Executing state's exit actions");
					}

					fsmEngine.executeActions(entryActions).thenAccept(f -> {
						printMessage("Entry actions finished");
						self().tell(new TryTransitionsMessage(), self());
					});
				})


				.match(TryTransitionsMessage.class, checkConditionMsg -> {
					//Case where we receive a Check FSMEntities.Condition Message

					sendNotification("otherInfo", "Trying transitions...");
					Tuple2<State, List<Action>> tuple = fsmEngine.tryTransitions();

					State nextState = tuple._1;
					List<Action> transitionActions = tuple._2;

					if (nextState == null) {
						sendNotification("otherInfo", "All conditions are false");
						return;
					}

					sendNotification("otherInfo", "A transition is feasible");

					List<Action> exitActions = fsmEngine.getActualState().getExitActions();
					if (exitActions.isEmpty()) {
						sendNotification("otherInfo", "There aren't any exit actions");
					} else {
						sendNotification("otherInfo", "Executing state's exit actions");
					}

					fsmEngine.executeActions(exitActions)
								.thenCompose(f -> {

									if (transitionActions.isEmpty()) {
										sendNotification("otherInfo", "There aren't any transition actions");
									} else {
										sendNotification("otherInfo", "Executing feasible guard's actions of the transition");
									}

									return fsmEngine.executeActions(transitionActions);

								})
								.thenAccept(f -> {
									sendNotification("otherInfo", "Changing state...");
									fsmEngine.setActualState(nextState);
									this.self().tell(new OnStateChangeMessage(), this.self());
								});
				})


				.match(EstablishConnectionMessage.class, establishConnectionMsg -> {
					printMessage("Connection made with a WebSocketActor");

					notifierActor = sender();

					statusChangeList.add(0, new NotifyStatusChangedMessage("connected", "Connection established!"));
					sendOldNotifications();
				})


				.matchAny(o -> printMessage("He recibido algo raro: " + o.toString()))


				.build();
	}

	private void sendOldNotifications() {
		if (statusChangeList.size() > 0) {
			statusChangeList.forEach(m -> notifierActor.tell(m, self()));
			statusChangeList.clear();
		}
	}

	private void sendNotification(String type, String message) {
		if (notifierActor != null) {
			sendOldNotifications();

			notifierActor.tell(new NotifyStatusChangedMessage(type, message), self());
		} else {
			statusChangeList.add(new NotifyStatusChangedMessage(type, message));
		}

		printMessage(message);
	}

	private void printMessage(String message) {
		//System.out.println("[" + self().path().name() + "] " + message);
		System.out.println("Actor: " + message);
	}
}
