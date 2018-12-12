package domain.fsm.engine;

import domain.fsm.entities.Action;
import domain.fsm.entities.Condition;
import domain.fsm.entities.FiniteStateMachine;
import domain.fsm.entities.Guard;
import domain.fsm.entities.Parameter;
import domain.fsm.entities.State;
import domain.fsm.entities.Transition;
import domain.http.HTTPClient;
import domain.Tuple2;
import domain.fsm.engine.exceptions.*;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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

public class FSMEngine {
	private HTTPClient httpClient;

	private Model model, localModel;
	private boolean useLocalModel;
	private State actualState;

	private String serverBaseUri;
	private String userFsmBaseUri;

	private UUID myId;

	FSMEngine(File file, String userFsmUri, String serverBaseUri, HTTPClient httpClient, UUID myId) throws OntologyNotFoundException, InitialStateNotFoundException, FileNotFoundException {
		this.serverBaseUri = serverBaseUri;
		this.httpClient = httpClient;
		this.myId = myId;

		printMessage("Server base URI = " + serverBaseUri);
		printMessage("User FSM URI = " + userFsmUri);

		State initialState;

		FiniteStateMachine fsm;

		Model userFsmModel = ModelFactory.createDefaultModel().read(new FileInputStream(file), null, "TURTLE");

		userFsmBaseUri = userFsmModel.getNsPrefixURI("");

		printMessage("User FSM base URI = " + userFsmBaseUri);

		fsm = FSMQueries.readFSM(userFsmModel, serverBaseUri, userFsmBaseUri, userFsmUri);
		if (fsm == null) {
			printMessage("Can't find the Finite State Machine");
			throw new OntologyNotFoundException("The ontology was not found");
		}

		initialState = fsm.getInitialState();
		if (initialState == null) {
			printMessage("No initial state found");
			throw new InitialStateNotFoundException();
		}

		model = ModelFactory.createDefaultModel();
		//Aqui toca leer informacion sobre el sistema, como los sensores, etc
		//model.read(new FileInputStream(file), null);

		printMessage("Init state -> " + initialState.getLocalName());

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
		printMessage("State -> " + actualState.getLocalName());

		//Check if the conditions will be checked only against data that arrive now
		if (FSMQueries.onlyNewDataAllowed(actualState, model, userFsmBaseUri)) {
			localModel = ModelFactory.createDefaultModel();
			//localModel.read(TC_SYS_ONT);
			useLocalModel = true;
		} else {
			useLocalModel = false;
			localModel = null;
		}
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

			String targetUri;
			if (action.hasAbsoluteURI()) {
				targetUri = action.getAbsoluteUri();
			} else {
				targetUri = action.getUriStructure();
				System.out.println("Structure -> " + action.getUriStructure());
				for (Parameter parameter : action.getParameters()) {
					String replacement = FSMQueries.getParameterReplacement(model, parameter);
					targetUri = targetUri.replace(parameter.getPlaceholder(), replacement);
				}
			}

			String body = action.getBody();
			String bodyType = action.getBodyType();

			if (bodyType.equals("executableSparql")) {
				bodyType = "other";
				body = "";
			}

			printMessage("Executing action " + action.getLocalName() + " at " + targetUri);

			switch (action.getMethod()) {
				case "GET":
					stage = httpClient.getRequest(targetUri, this::onActionResponse, action.getTimeoutInMs());
					break;
				case "POST":
					stage = httpClient.postRequest(targetUri, bodyType, body, this::onActionResponse, action.getTimeoutInMs());
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

	public void insertData(String rdfData) {
		rdfData = FSMQueries.addBasePrefixesToRdf(rdfData, serverBaseUri);

		model.enterCriticalSection(Lock.WRITE);
		try {
			RDFDataMgr.read(model, new StringReader(rdfData), userFsmBaseUri, Lang.TTL);
		} catch (Exception e) {
			printMessage("\tBody message bad RDF Turtle format");
		} finally {
			model.leaveCriticalSection();
		}

		if (localModel != null) {
			localModel.enterCriticalSection(Lock.WRITE);
			try {
				RDFDataMgr.read(localModel, new StringReader(rdfData), userFsmBaseUri, Lang.TTL);
			} catch (Exception e) {
				printMessage("\tBody message bad RDF Turtle format");
			} finally {
				localModel.leaveCriticalSection();
			}
		}
	}

	public String getData(String query) {
		var result = "";

		query = FSMQueries.addBasePrefixesToQuery(query, serverBaseUri);

		model.enterCriticalSection(Lock.READ);
		try {
			result = FSMQueries.getData(model, query);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			model.leaveCriticalSection();
		}

		return result;
	}

	public void executeOperation(String query) {
		query = FSMQueries.addBasePrefixesToQuery(query, serverBaseUri);

		model.enterCriticalSection(Lock.WRITE);
		try {
			FSMQueries.executeOperation(model, query);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			model.leaveCriticalSection();
		}

		if (localModel != null) {
			localModel.enterCriticalSection(Lock.READ);
			try {
				FSMQueries.executeOperation(localModel, query);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				localModel.leaveCriticalSection();
			}
		}
	}

	public static String generateActorName(UUID uuid) {
		return "fsm_actor_" + uuid.toString();
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

	private void onActionResponse(int status, String body) {
		printMessage("\t\tStatus: " + status);
		printMessage("\t\tBody: " + body);

		insertData(body);
	}

	private void printMessage(String message) {
		char id = myId.toString().charAt(0);
		System.out.println("FSM " + id + ": " + message);
	}
}
