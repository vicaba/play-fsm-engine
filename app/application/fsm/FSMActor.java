package application.fsm;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import domain.fsm.engine.EstablishConnectionMessage;
import domain.fsm.engine.FSMEngine;
import domain.fsm.engine.OnStateChangeMessage;
import domain.fsm.engine.TryTransitionsMessage;
import domain.Tuple2;
import domain.fsm.entities.Action;
import domain.fsm.entities.State;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FSMActor extends AbstractActor {
	private FSMEngine fsmEngine;
	private ActorRef notifierActor;

	private List<NotifyStatusChangedMessage> statusChangeList;

	public FSMActor(FSMEngine fsmEngine) {
		this.fsmEngine = fsmEngine;
		this.notifierActor = null;
		statusChangeList = new ArrayList<>();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()


				.match(OnStateChangeMessage.class, onStateChangeMessage -> {
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

						if (fsmEngine.getActualState().isFinal()) {
							close();
						} else {
							self().tell(new TryTransitionsMessage(), self());
						}
					});
				})


				.match(TryTransitionsMessage.class, tryTransitionsMessage -> {
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


				.match(SendDataMessage.class, sendDataMessage -> {
					printMessage("Received some data " + sendDataMessage.getData());

					fsmEngine.insertData(sendDataMessage.getData());

					sendNotification("otherInfo", "I have received some data");

					self().tell(new TryTransitionsMessage(), self());
				})


				.match(GetDataMessage.class, getDataMessage -> {
					printMessage("Received a query " + getDataMessage.getQuery());

					String result = fsmEngine.getData(getDataMessage.getQuery());

					sender().tell(result, self());

					sendNotification("otherInfo", "I have received some data");
				})

				.match(ExecuteOperationMessage.class, executeOperationMessage -> {
					printMessage("Received a query " + executeOperationMessage.getOperation());

					fsmEngine.executeOperation(executeOperationMessage.getOperation());

					sendNotification("otherInfo", "I have received some data");

					self().tell(new TryTransitionsMessage(), self());
				})


				.match(EstablishConnectionMessage.class, establishConnectionMessage -> {
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

	private void close() {
		if (notifierActor != null) {
			notifierActor.tell(new CloseActorMessage(), self());
		}
		self().tell(PoisonPill.getInstance(), self());
	}

	private void printMessage(String message) {
		//System.out.println("[" + self().path().name() + "] " + message);
		System.out.println("Actor: " + message);
	}

	public static ActorRef findActorById(ActorSystem system, String actorId) {
		UUID uuid = UUID.fromString(actorId);

		String actorName = "user/" + FSMEngine.generateActorName(uuid);

		return system.actorSelection(actorName).resolveOneCS(Duration.ofDays(1)).toCompletableFuture().join();
	}
}
