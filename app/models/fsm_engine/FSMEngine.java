package models.fsm_engine;

import akka.util.Timeout;
import models.Tuple2;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

public class FSMEngine {
	private HTTPClient httpClient;

	private Model model, localModel;
	private boolean useLocalModel;
	private State actualState;

	public FSMEngine(HTTPClient httpClient, File file) throws OntologyNotFoundException, InitialStateNotFoundException, FileNotFoundException {
		this.httpClient = httpClient;

		State initialState;

		FiniteStateMachine fsm;

		Model userModel = ModelFactory.createDefaultModel().read(new FileInputStream(file), null, "TURTLE");

		String userBaseURI = userModel.getNsPrefixURI("");
		System.out.println(userModel.getNsPrefixMap().toString());

		System.out.println("USER PREFIX ENGINGE= " + userBaseURI);

		fsm = FSMQueries.readFSM(userModel, userBaseURI + "telecontrolFSM");
		if (fsm == null) {
			System.out.println("Can't find the Finite State Machine");
			throw new OntologyNotFoundException("The ontology " + userBaseURI + " was not found");
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

	public void setActualState(State actualState) {
		this.actualState = actualState;
	}

	public void prepareNewState() {
		System.out.println("State -> " + actualState.getLocalName());

		//Check if the conditions will be checked only against data that arrive now
		if (FSMQueries.onlyNewDataAllowed(actualState, model)) {
			localModel = ModelFactory.createDefaultModel();
			//localModel.read(TC_SYS_ONT);
			useLocalModel = true;
		} else {
			useLocalModel = false;
			localModel = null;
		}
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

	public Tuple2<State, List<Action>> tryTransitions() {
		List<Action> guardActions = new ArrayList<>();
		State nextState = null;

		for (Transition transition : actualState.getTransitions()) {
			boolean isAnyGuardTrue = false;

			List<Guard> transitionGuards = transition.getGuards();

			for (Guard guard : transitionGuards) {
				if (evaluateGuard(guard)) {
					isAnyGuardTrue = true;
					guardActions.addAll(guard.getActions());
				}
			}

			if (transitionGuards.isEmpty() || isAnyGuardTrue) {
				nextState = transition.getTargetState();
				break;
			}
		}

		return new Tuple2<>(nextState, guardActions);
	}

	public CompletableFuture<Void> executeActions(List<Action> actions) {
		List<CompletableFuture> futures = new ArrayList<>();

		actions.forEach(action -> {
			CompletionStage stage;
			System.out.println("Executing action " + action.getLocalName() + " at " + action.getTargetURI());

			switch (action.getMethod()) {
				case "GET":
					stage = httpClient.getRequest(action.getTargetURI(), this::onActionResponse, action.getTimeoutInMsec());
					break;
				case "POST":
					stage = httpClient.postRequest(action.getTargetURI(), action.getBody(), this::onActionResponse, action.getTimeoutInMsec());
					break;
				default:
					stage = CompletableFuture.completedStage(true);
					break;
			}

			futures.add(stage.toCompletableFuture());
		});

		CompletableFuture[] futuresArray = new CompletableFuture[futures.size()];
		futures.toArray(futuresArray);

		return CompletableFuture.allOf(futuresArray);
	}

	private void onActionResponse(int status, String body) {
		System.out.println("\t\tStatus: " + status);
		System.out.println("\t\tBody: " + body);

		insertData(body);
	}

	public void insertData(String data) {
		model.enterCriticalSection(Lock.WRITE);
		try {
			RDFDataMgr.read(model, new StringReader(data), FSMQueries.getOntologyBaseURI(model), Lang.TTL);
		} catch (RiotNotFoundException e){
			System.out.println("\t\t\tBody message bad RDF Turtle format");
		} finally {
			model.leaveCriticalSection();
		}

		if (localModel != null) {
			localModel.enterCriticalSection(Lock.WRITE);
			try {
				RDFDataMgr.read(localModel, new StringReader(data), FSMQueries.getOntologyBaseURI(model), Lang.TTL);
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
