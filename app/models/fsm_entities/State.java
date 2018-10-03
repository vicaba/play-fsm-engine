package models.fsm_entities;

import java.util.List;

public class State extends Individual {
	private List<Action> entryActions;
	private List<Action> exitActions;
	private List<Transition> transitions;

	public State(String URI, String localName, List<Action> entryActions, List<Action> exitActions, List<Transition> transitions) {
		super(URI, localName);
		this.entryActions = entryActions;
		this.exitActions = exitActions;
		this.transitions = transitions;
	}

	public List<Action> getEntryActions() {
		return entryActions;
	}

	public List<Action> getExitActions() {
		return exitActions;
	}

	public List<Transition> getTransitions() {
		return transitions;
	}
}
