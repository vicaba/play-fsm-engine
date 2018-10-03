package models.fsm_engine;

import models.fsm_entities.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.List;

public class FSMQueries {
	public static final String BASE = "file:///D:/projects/JenaTest/ontologies/telecontrol_FSM.owl.ttl#";
	public static final String FSM_PREFIX = "file:///D:/projects/JenaTest/ontologies/fsm.owl#";
	public static final String HTTP_PREFIX = "http://www.w3.org/2011/http#";

	public static State findStateByURI(List<State> states, String URI) {
		for (State state : states) {
			if (state.getURI().equals(URI)) {
				return state;
			}
		}

		return null;
	}

	public static FiniteStateMachine readFSM(Model model, String fsmIRI) {
		List<State> states = new ArrayList<>();
		State initialState = null;

		Resource fsmRes = getFSM(model, fsmIRI);

		if (fsmRes == null) {
			return null;
		}

		String fsmLocalName = fsmRes.getLocalName();
		System.out.println(fsmLocalName);

		List<Resource> statesRes = getAllStatesResource(model, fsmIRI);
		for (Resource stateRes : statesRes) {
			String stateURI = stateRes.getURI();
			String stateLocalName = stateRes.getLocalName();
			List<Action> entryActions = getActions(model, stateRes, "entry");
			List<Action> exitActions = getActions(model, stateRes, "exit");
			states.add(new State(stateURI, stateLocalName, entryActions, exitActions, new ArrayList<>()));
		}

		for (State sourceState : states) {
			List<QuerySolution> transitionsSol = getTransitionsRes(model, sourceState);
			for (QuerySolution sol : transitionsSol) {
				Resource transitionRes = sol.getResource("transition");
				String transitionURI = transitionRes.getURI();
				String transitionLocalName = transitionRes.getLocalName();

				Resource targetStateRes = sol.getResource("targetState");
				State targetState = findStateByURI(states, targetStateRes.getURI());

				List<Guard> guards = getTransitionGuards(model, transitionRes);

				sourceState.getTransitions().add(new Transition(transitionURI, transitionLocalName, sourceState, targetState, guards));
			}
		}

		Resource initialStateRes = getInitialState(model);
		if (initialStateRes != null) {
			initialState = findStateByURI(states, initialStateRes.getURI());
		}

		return new FiniteStateMachine(fsmIRI, fsmLocalName, states, initialState);
	}

	public static Resource getFSM(Model model, String fsmIRI) {
		Resource fsm = null;

		String queryString =
				"		 PREFIX fsm: <" + FSM_PREFIX + ">  \n" +
						"PREFIX : <" + BASE + "> \n" +
						"SELECT ?fsm \n" +
						"WHERE { \n" +
						"	?fsm a fsm:StateMachine . " +
						"	FILTER (STR(?fsm) = \"" + fsmIRI + "\") . " +
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

	public static List<Action> getActions(Model model, Resource res, String actionsType) {
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
				"		 PREFIX fsm: <" + FSM_PREFIX + ">  \n" +
						"PREFIX http: <" + HTTP_PREFIX + "> \n" +
						"PREFIX : <" + BASE + "> \n" +
						"SELECT ?action ?URI ?method \n" +
						"WHERE { \n" +
						resIRI + " " + property + " ?action . \n" +
						"	?action a fsm:FSMEntities.Action . \n" +
						"	?action a http:Request . \n" +
						"	?action http:absoluteURI ?URI . \n" +
						"	?action http:mthd ?method . \n" +
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

				//TODO: tratar body de la request
				String body = null;

				entryActions.add(new Action(actionURI, actionLocalName, targetURI, method, body));
			}
		}

		return entryActions;
	}

	public static List<Guard> getTransitionGuards(Model model, Resource transitionRes) {
		List<Guard> guards = new ArrayList<>();

		String transitionIRI = getFormattedIRI(transitionRes.getURI());

		String queryString =
				"		 PREFIX fsm: <" + FSM_PREFIX + "> " +
						"PREFIX http: <" + HTTP_PREFIX + "> " +
						"PREFIX : <" + BASE + "> " +
						"SELECT ?guard " +
						"WHERE { " +
						transitionIRI + " fsm:hasTransitionGuard ?guard . " +
						"	?guard a fsm:FSMEntities.Guard . " +
						"}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet resultSet = qe.execSelect();

			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();

				Resource guardRes = sol.getResource("guard");

				String URI = guardRes.getURI();
				String localName = guardRes.getLocalName();
				List<Condition> conditions = getGuardConditions(model, guardRes);
				List<Action> actions = getActions(model, guardRes, "guard");

				guards.add(new Guard(URI, localName, conditions, actions));
			}
		}

		return guards;
	}

	public static List<Condition> getGuardConditions(Model model, Resource guardRes) {
		List<Condition> conditions = new ArrayList<>();

		String guardIRI = getFormattedIRI(guardRes.getURI());

		String queryString =
				"		 PREFIX fsm: <" + FSM_PREFIX + "> " +
				"PREFIX http: <" + HTTP_PREFIX + "> " +
				"PREFIX : <" + BASE + "> " +
				"SELECT ?condition ?sparqlQuery " +
				"WHERE { " +
				guardIRI + " a fsm:FSMEntities.Guard . " +
				guardIRI + " fsm:hasGuardCondition ?condition . " +
				"	?condition fsm:hasBody ?sparqlQuery . " +
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

				conditions.add(new Condition(URI, localName, sparqlQueryRes));
			}
		}

		return conditions;
	}

	public static List<Resource> getRequiredProperties(Model model, Resource transition) {
		List<Resource> properties = new ArrayList<>();

		String transitionIRI = getFormattedIRI(transition.getURI());

		System.out.println(transitionIRI);

		String queryString =
				"		 PREFIX fsm: <" + FSM_PREFIX + "> " +
						"PREFIX : <" + BASE + "> " +
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
	}

	public static boolean evaluateCondition(Model model, Condition condition) {
		boolean result = false;

		if (!condition.hasQuery()) {
			return true;
	}

		Query query = QueryFactory.create(condition.getSparqlQuery());
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			result = qe.execAsk();
			System.out.println(result);
		}

		return result;
	}

	public static List<Resource> getAllStatesResource(Model model, String fsmURI) {
		List<Resource> statesRes = new ArrayList<>();

		String queryString =
				"PREFIX fsm: <" + FSM_PREFIX + "> " +
						"PREFIX : <" + BASE + "> " +
						"SELECT ?state " +
						"WHERE { " +
						getFormattedIRI(fsmURI) + " fsm:hasStateMachineElement ?state . " +
						"	?state a fsm:FSMEntities.State . " +
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

	public static List<QuerySolution> getTransitionsRes(Model model, State state) {
		List<QuerySolution> transitionsSol = new ArrayList<>();

		String stateIRI = getFormattedIRI(state.getURI());

		//TODO: mirar la importancia de indicar la clase en lugar de solo las propiedades
		String queryString =
				"PREFIX fsm: <" + FSM_PREFIX + "> " +
						"PREFIX : <" + BASE + "> " +
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

	public static Resource getInitialState(Model model) {
		Resource initialStateRes = null;

		String queryString =
				"		  PREFIX fsm: <" + FSM_PREFIX + "> " +
						" PREFIX : <" + BASE + "> " +
						" SELECT ?initialState " +
						" WHERE { " +
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

	public static boolean onlyNewDataAllowed(State state, Model model) {
		boolean only = false;

		String stateIRI = getFormattedIRI(state.getURI());

		String queryString =
				"		  PREFIX fsm: <" + FSM_PREFIX + "> " +
						" PREFIX : <" + BASE + "> " +
						" ASK { " +
						stateIRI + " fsm:onlyNewData	 ?only . " +
						" 	FILTER (?only = true) . " +
						" } ";
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			only = qe.execAsk();
		}

		return only;
	}

	private static String getFormattedIRI(String IRI) {
		return "<" + IRI + ">";
	}
	
}
