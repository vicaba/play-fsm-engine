package domain.fsm.engine;

import domain.fsm.entities.Action;
import domain.fsm.entities.Condition;
import domain.fsm.entities.FiniteStateMachine;
import domain.fsm.entities.Guard;
import domain.fsm.entities.State;
import domain.fsm.entities.StateType;
import domain.fsm.entities.Transition;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateAction;

import java.util.ArrayList;
import java.util.List;

class FSMQueries {
	private static final String FSM_PREFIX = "file:///D:/projects/ontologies/fsm/fsm.owl#";
	private static final String HTTP_PREFIX = "http://www.w3.org/2011/http#";

	static FiniteStateMachine readFSM(Model model, String serverBaseUri, String userFsmBaseUri, String userFsmUri) {
		List<State> states = new ArrayList<>();
		State initialState = null;

		Resource fsmRes = getFSM(model, userFsmBaseUri, userFsmUri);

		if (fsmRes == null) {
			return null;
		}

		String fsmLocalName = fsmRes.getLocalName();
		System.out.println(fsmLocalName);

		List<Resource> statesResource = getAllStatesResource(model, userFsmBaseUri, userFsmUri);
		for (Resource stateRes : statesResource) {
			String stateUri = stateRes.getURI();
			String stateLocalName = stateRes.getLocalName();
			StateType stateType = getStateType(model, stateUri);
			List<Action> entryActions = getActions(model, stateRes, "entry", serverBaseUri, userFsmBaseUri);
			List<Action> exitActions = getActions(model, stateRes, "exit", serverBaseUri, userFsmBaseUri);
			states.add(new State(stateUri, stateLocalName, stateType, entryActions, exitActions, new ArrayList<>()));
			System.out.println("State " + stateLocalName + " is a " + stateType.toString());
		}

		for (State sourceState : states) {
			List<QuerySolution> transitionsSol = getTransitionsRes(model, sourceState, userFsmBaseUri);
			for (QuerySolution sol : transitionsSol) {
				Resource transitionRes = sol.getResource("transition");
				String transitionURI = transitionRes.getURI();
				String transitionLocalName = transitionRes.getLocalName();

				Resource targetStateRes = sol.getResource("targetState");
				State targetState = findStateByURI(states, targetStateRes.getURI());

				List<Guard> guards = getTransitionGuards(model, transitionRes, serverBaseUri, userFsmBaseUri);

				sourceState.getTransitions().add(new Transition(transitionURI, transitionLocalName, sourceState, targetState, guards));
			}
		}

		Resource initialStateRes = getInitialState(model, userFsmUri);
		if (initialStateRes != null) {
			initialState = findStateByURI(states, initialStateRes.getURI());
		}

		return new FiniteStateMachine(userFsmUri, fsmLocalName, states, initialState);
	}

	static boolean evaluateCondition(Model model, Condition condition) {
		boolean result = false;

		if (!condition.hasQuery()) {
			return true;
		}

		Query query = QueryFactory.create(condition.getSparqlQuery());
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			result = qe.execAsk();
			System.out.println(result);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	static boolean onlyNewDataAllowed(State state, Model model, String userFsmBaseUri) {
		boolean only = false;

		String stateIRI = getFormattedIRI(state.getURI());

		String queryString =
				"		  prefix fsm: <" + FSM_PREFIX + "> " +
						" prefix : <" + userFsmBaseUri + "> " +
						" ASK { " +
						stateIRI + " fsm:onlyNewData	 ?only . " +
						" 	FILTER (?only = true) . " +
						" } ";
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			only = qe.execAsk();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return only;
	}

	static void executeOperation(Model model, String operation) {
		UpdateAction.parseExecute(operation, model);
	}

	static String getData(Model model, String queryString) {
		Query query = QueryFactory.create(queryString);
		StringBuilder sb = new StringBuilder();

		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet resultSet = qe.execSelect();

			while (resultSet.hasNext()) {
				var qs = resultSet.next();

				qs.varNames().forEachRemaining(key -> sb.append(qs.get(key).toString()).append(" "));

				sb.append(System.lineSeparator());
			}
		}

		return sb.toString();
	}

	static String addBasePrefixesToRdf(String rdf, String uri) {
		String basePrefix = "@prefix : <" + uri + "#> . ";
		String selfPrefix = "@prefix self: <" + uri + "> . ";

		return basePrefix + selfPrefix + rdf;
	}

	static String addBasePrefixesToQuery(String query, String uri) {
		String basePrefix = "PREFIX : <" + uri + "#> ";
		String selfPrefix = "PREFIX self: <" + uri + "> ";

		return basePrefix + selfPrefix + query;
	}

	private static State findStateByURI(List<State> states, String URI) {
		for (State state : states) {
			if (state.getURI().equals(URI)) {
				return state;
			}
		}

		return null;
	}

	private static Resource getFSM(Model model, String userFsmBaseUri, String userFsmUri) {
		Resource fsm = null;

		String queryString =
				"		 prefix fsm: <" + FSM_PREFIX + ">  \n" +
						"prefix : <" + userFsmBaseUri + "> \n" +
						"SELECT ?fsm \n" +
						"WHERE { \n" +
						"	?fsm a fsm:StateMachine . " +
						"	FILTER (STR(?fsm) = \"" + userFsmUri + "\") . " +
						"} LIMIT 1";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet resultSet = qe.execSelect();

			if (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();

				fsm = sol.getResource("fsm");
			}
		}

		return fsm;
	}

	private static List<Action> getActions(Model model, Resource res, String actionsType, String serverBaseUri, String userFsmBaseUri) {
		List<Action> entryActions = new ArrayList<>();

		String property;

		switch (actionsType) {
			case "entry":
				property = " fsm:hasEntryAction ";
				break;
			case "exit":
				property = " fsm:hasExitAction ";
				break;
			case "guard":
				property = " fsm:hasGuardAction ";
				break;
			default:
				return entryActions;
		}

		String resIRI = getFormattedIRI(res.getURI());

		String queryString =
				"		 prefix fsm: <" + FSM_PREFIX + ">  \n" +
						"prefix http: <" + HTTP_PREFIX + "> \n" +
						"prefix : <" + userFsmBaseUri + "> \n" +
						"SELECT ?action ?URI ?method ?bodyContent ?bodyType ?timeoutInMs \n" +
						"WHERE { \n" +
						resIRI + " " + property + " ?action . \n" +
						"	?action a fsm:Action . \n" +
						"	?action a http:Request . \n" +
						"	?action http:absoluteURI ?URI . \n" +
						"	?action http:mthd ?method . \n" +
						"  OPTIONAL { ?action fsm:hasBody ?body . ?body fsm:hasContent ?bodyContent } . \n" +
						"  OPTIONAL { ?body fsm:hasBodyType ?bodyType } . \n" +
						"  OPTIONAL { ?action fsm:hasTimeoutInMs ?timeoutInMs } . \n" +
						"}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet resultSet = qe.execSelect();

			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();

				Resource actionRes = sol.getResource("action");
				String actionURI = actionRes.getURI();
				String actionLocalName = actionRes.getLocalName();

				String targetURI = sol.getResource("URI").getURI();

				String method = sol.getResource("method").getLocalName();

				String body = "";
				String bodyType = "";
				if (sol.contains("bodyContent")) {
					body = sol.getLiteral("bodyContent").getString();

					if (sol.contains("bodyType")) {
						bodyType = sol.getResource("bodyType").getLocalName();

						switch (bodyType) {
							case "rdf":
								body = addBasePrefixesToRdf(body, serverBaseUri);
								break;
							case "sparql":
								body = addBasePrefixesToQuery(body, serverBaseUri);
								break;
							case "other":
							default:
								//Don't do anything to the data
								break;
						}
					}
				}

				int timeoutInMs = 1000;
				if (sol.contains("timeoutInMs")) {
					timeoutInMs = sol.getLiteral("timeoutInMs").getInt();
				}

				entryActions.add(new Action(actionURI, actionLocalName, targetURI, method, bodyType, body, timeoutInMs));
			}
		}

		return entryActions;
	}

	private static List<Guard> getTransitionGuards(Model model, Resource transitionRes, String serverBaseUri, String userFsmBaseUri) {
		List<Guard> guards = new ArrayList<>();

		String transitionIRI = getFormattedIRI(transitionRes.getURI());

		String queryString =
				"		 prefix fsm: <" + FSM_PREFIX + "> " +
						"prefix http: <" + HTTP_PREFIX + "> " +
						"prefix : <" + userFsmBaseUri + "> " +
						"SELECT ?guard " +
						"WHERE { " +
						transitionIRI + " fsm:hasTransitionGuard ?guard . " +
						"	?guard a fsm:Guard . " +
						"}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet resultSet = qe.execSelect();

			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();

				Resource guardRes = sol.getResource("guard");

				String URI = guardRes.getURI();
				String localName = guardRes.getLocalName();
				List<Condition> conditions = getGuardConditions(model, guardRes, serverBaseUri, userFsmBaseUri);
				List<Action> actions = getActions(model, guardRes, "guard", serverBaseUri, userFsmBaseUri);

				guards.add(new Guard(URI, localName, conditions, actions));
			}
		}

		return guards;
	}

	private static List<Condition> getGuardConditions(Model model, Resource guardRes, String serverBaseUri, String userFsmBaseUri) {
		List<Condition> conditions = new ArrayList<>();

		String guardIRI = getFormattedIRI(guardRes.getURI());

		String queryString =
				"		 prefix fsm: <" + FSM_PREFIX + "> " +
						"prefix http: <" + HTTP_PREFIX + "> " +
						"prefix : <" + userFsmBaseUri + "> " +
						"SELECT ?condition ?sparqlQuery " +
						"WHERE { " +
						guardIRI + " a fsm:Guard . " +
						guardIRI + " fsm:hasGuardCondition ?condition . " +
						"	?condition fsm:hasContent ?sparqlQuery . " +
						"}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet resultSet = qe.execSelect();

			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();

				Resource conditionRes = sol.getResource("condition");

				String URI = conditionRes.getURI();
				String localName = conditionRes.getLocalName();
				String sparqlQueryRes = sol.getLiteral("sparqlQuery").getString().replace("\\\"", "\"");

				sparqlQueryRes = addBasePrefixesToQuery(sparqlQueryRes, serverBaseUri);

				conditions.add(new Condition(URI, localName, sparqlQueryRes));
			}
		}

		return conditions;
	}

	/*public static List<Resource> getRequiredProperties(Model model, Resource transition) {
		List<Resource> properties = new ArrayList<>();

		String prefix = getOntologyBaseURI(model);

		String transitionIRI = getFormattedIRI(transition.getURI());

		System.out.println(transitionIRI);

		String queryString =
				"		 prefix fsm: <" + FSM_PREFIX + "> " +
						"prefix : <" + prefix + "> " +
						"SELECT ?property " +
						"WHERE { " +
						transitionIRI + " fsm:hasTransitionGuard ?guard . " +
						"?guard fsm:hasGuardCondition ?condition . " +
						"?condition :requiredProperty ?property . " +
						"}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qe.execSelect();
			if (!results.hasNext()) {
				System.out.println("no result");
			}
			while (results.hasNext()) {
				QuerySolution sol = results.next();

				Resource state = sol.getResource("property");
				properties.add(state);
			}

			return properties;
		}
	}*/

	private static List<Resource> getAllStatesResource(Model model, String userFsmBaseUri, String userFsmUri) {
		List<Resource> statesRes = new ArrayList<>();

		String queryString =
				"prefix fsm: <" + FSM_PREFIX + "> " +
						"prefix : <" + userFsmBaseUri + "> " +
						"SELECT ?state " +
						"WHERE { " +
						getFormattedIRI(userFsmUri) + " fsm:hasStateMachineElement ?state . " +
						"	?state a fsm:State . " +
						"}";
		Query query = QueryFactory.create(queryString);

		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution sol = results.next();

				statesRes.add(sol.getResource("state"));
			}
		}

		return statesRes;
	}

	private static StateType getStateType(Model model, String stateUri) {
		String queryString =
				"prefix fsm: <" + FSM_PREFIX + "> " +
						"SELECT ?stateType " +
						"WHERE { " +
						"	<" + stateUri + "> a ?stateType . " +
						"}";


		try (QueryExecution qe = QueryExecutionFactory.create(queryString, model)) {
			ResultSet results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution sol = results.next();

				Resource stateType = sol.getResource("stateType");
				String stateClass = stateType.getLocalName();

				if (!stateClass.equals("State")) {
					switch (stateClass) {
						case "InitialState":
							return StateType.INITIAL;
						case "FinalState":
							return StateType.FINAL;
						case "SimpleState":
							return StateType.SIMPLE;
					}
				}
			}
		}

		return StateType.DEFAULT;
	}


	private static List<QuerySolution> getTransitionsRes(Model model, State state, String userFsmBaseUri) {
		List<QuerySolution> transitionsSol = new ArrayList<>();

		String stateIRI = getFormattedIRI(state.getURI());

		//TODO: check the importance of stating the properties' classes instead of only the properties
		String queryString =
				"prefix fsm: <" + FSM_PREFIX + "> " +
						"prefix : <" + userFsmBaseUri + "> " +
						"SELECT ?transition ?targetState " +
						"WHERE { " +
						"	?transition fsm:hasSourceState " + stateIRI + " . " +
						"	?transition fsm:hasTargetState ?targetState . " +
						"}";
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet transitionsRS = qe.execSelect();

			while (transitionsRS.hasNext()) {
				transitionsSol.add(transitionsRS.next());
			}
		}

		return transitionsSol;
	}

	private static Resource getInitialState(Model model, String userFsmUri) {
		Resource initialStateRes = null;

		String queryString =
				"		  prefix fsm: <" + FSM_PREFIX + "> " +
						" SELECT ?initialState " +
						" WHERE { " +
						"<" + userFsmUri + "> fsm:hasStateMachineElement ?initialState . " +
						"   ?initialState a fsm:InitialState . " +
						" } " +
						" LIMIT 1";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qe.execSelect();
			if (results.hasNext()) {
				QuerySolution sol = results.next();

				initialStateRes = sol.getResource("initialState");
			}
		}

		return initialStateRes;
	}

	private static String getFormattedIRI(String IRI) {
		return "<" + IRI + ">";
	}

}
