package models.fsm_engine;

import models.fsm_engine.Exceptions.*;
import models.fsm_entities.*;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotNotFoundException;
import org.apache.jena.shared.Lock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FSMEngine {
	private HTTPClient httpClient;

	private Model model, localModel;
	private boolean useLocalModel;
	private State actualState;

	private static final String FSM_IRI = "file:///D:/projects/JenaTest/ontologies/telecontrol_FSM.owl.ttl#telecontrolFSM";
	private static final String TC_FSM_ONT = "ontologies/telecontrol_FSM.owl.ttl";
	private static final String TC_SYS_ONT = "ontologies/telecontrol_system.owl.ttl";

	public FSMEngine(HTTPClient httpClient, File file) throws OntologyNotFoundException, InitialStateNotFoundException, FileNotFoundException {
		this.httpClient = httpClient;

		State initialState;

		FiniteStateMachine fsm;

		System.out.println(file.getName());

		fsm = FSMQueries.readFSM(ModelFactory.createDefaultModel().read(new FileInputStream(file), null, "TURTLE"), FSM_IRI);
		if (fsm == null) {
			System.out.println("Can't find the Finite FSMEntities.State Machine");
			throw new OntologyNotFoundException("The ontology " + TC_FSM_ONT + " was not found");
		}

		initialState = fsm.getInitialState();
		if (initialState == null) {
			System.out.println("No initial state found");
			throw new InitialStateNotFoundException();
		}

		model = ModelFactory.createDefaultModel();
		//Aqui toca leer informacion sobre el sistema, como los sensores, etc
		//model.read(new FileInputStream(file), null);

		System.out.println("Init state -> " + initialState.getLocalName());

		localModel = null;
		useLocalModel = false;

		actualState = initialState;
	}

	public State getActualState() {
		return actualState;
	}

	public void onStateChange() {
		System.out.println("FSMEntities.State -> " + actualState.getLocalName());

		//Check if the conditions will be checked only against data that arrive now
		if (FSMQueries.onlyNewDataAllowed(actualState, model)) {
			localModel = ModelFactory.createDefaultModel();
			//localModel.read(TC_SYS_ONT);
			useLocalModel = true;
		} else {
			useLocalModel = false;
			localModel = null;
		}

		//Get the entry actions of the actual state and execute them
		executeActions(actualState.getEntryActions());

		//Send message to checkTransition
	}

	public State checkTransitions() {
		for (Transition transition : actualState.getTransitions()) {
			if (evaluateTransition(transition, actualState)) {
				//Change the next state and exit while loop
				actualState = transition.getTargetState();
				return transition.getTargetState();
			}
		}
		return null;
	}

	private boolean evaluateTransition(Transition transition, State actualState) {
		List<Guard> guards = transition.getGuards();

		if (guards.isEmpty()) {
			//Execute the state exit actions
			executeActions(actualState.getExitActions());
			return true;
		}

		for (Guard guard : guards) {
			//TODO: ejecutar todas las acciones de todos los guards que sean ciertos y no solo del primero que se encuentre
			if (evaluateGuard(guard)) {
				//Execute the state exit actions
				executeActions(actualState.getExitActions());

				//Execute the guards actions
				executeActions(guard.getActions());

				return true;
			}
		}

		return false;
	}

	private boolean evaluateGuard(Guard guard) {
		List<Condition> conditions = guard.getConditions();

		if (conditions.isEmpty()) {
			return true;
		}

		for (Condition condition : conditions) {
			Model modelToUse;
			if (useLocalModel) {
				modelToUse = localModel;
			} else {
				modelToUse = model;
			}

			model.enterCriticalSection(Lock.READ);
			boolean isConditionTrue = FSMQueries.evaluateCondition(modelToUse, condition);
			model.leaveCriticalSection();

			if (isConditionTrue) {
				return true;
			}
		}

		return false;
	}

	public void executeActions(List<Action> actions) {
		List<Future<Boolean>> futures = new ArrayList<>();

		for (Action action : actions) {
			System.out.println("Executing action " + action.getLocalName() + " at " + action.getTargetURI());

			switch (action.getMethod()) {
				case "POST":
					Future<Boolean> f = httpClient.postRequest(action.getTargetURI(), (status, body) -> {
						System.out.println("\t\tStatus: " + status);
						System.out.println("\t\tBody: " + body);
						try {
							insertData(body);
						} catch (Exception e) {
							System.out.println("Bad RDF read");
						}
					});
					futures.add(f);
					break;
			}
		}

		//TODO: descomentar esto
		for (Future<Boolean> f : futures) {
			try {
				f.get(10, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Continuing...");
	}

	public void insertData(String data) throws RiotNotFoundException {
		model.enterCriticalSection(Lock.WRITE);
		try {
			RDFDataMgr.read(model, new StringReader(data), FSMQueries.BASE, Lang.TTL);
		} catch (RiotNotFoundException e){
			System.out.println("\t\t\tBody message bad RDF Turtle format");
		} finally {
			model.leaveCriticalSection();
		}

		if (localModel != null) {
			localModel.enterCriticalSection(Lock.WRITE);
			try {
				RDFDataMgr.read(localModel, new StringReader(data), FSMQueries.BASE, Lang.TTL);
			} catch (RiotNotFoundException e) {
				System.out.println("Body message bad RDF Turtle format");
			} finally {
				localModel.leaveCriticalSection();
			}
		}
	}

	public static String generateActorName(UUID uuid) {
		return "fsm_actor_" + uuid.toString();
	}

}
