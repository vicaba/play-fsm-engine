package domain.fsm.entities;

import java.util.List;

public class State extends Individual {
	private StateType type;
	private List<Action> entryActions;
	private List<Action> exitActions;
	private List<Transition> transitions;

	public State(String URI, String localName, StateType type, List<Action> entryActions, List<Action> exitActions, List<Transition> transitions) {
		super(URI, localName);
		this.type = type;
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

	public boolean isFinal() {
		return type == StateType.FINAL;
	}
}
