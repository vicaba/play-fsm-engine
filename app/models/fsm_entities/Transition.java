package models.fsm_entities;

import java.util.List;

public class Transition extends Individual {
	private State sourceState;
	private State targetState;
	private List<Guard> guards;

	public Transition(String URI, String localName, State sourceState, State targetState, List<Guard> guards) {
		super(URI, localName);
		this.sourceState = sourceState;
		this.targetState = targetState;
		this.guards = guards;
	}

	public State getSourceState() {
		return sourceState;
	}

	public State getTargetState() {
		return targetState;
	}

	public List<Guard> getGuards() {
		return guards;
	}
}
