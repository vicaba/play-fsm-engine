package models.fsm_entities;

import java.util.List;

public class FiniteStateMachine extends Individual {
	private List<State> states;
	private State initialState;

	public FiniteStateMachine(String URI, String localName, List<State> states, State initialState) {
		super(URI, localName);
		this.states = states;
		this.initialState = initialState;
	}

	public List<State> getStates() {
		return states;
	}

	public State getInitialState() {
		return initialState;
	}
}
