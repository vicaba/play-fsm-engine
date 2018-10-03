package models.fsm_entities;

import java.util.List;

public class Guard extends Individual {
	private List<Condition> conditions;
	private List<Action> actions;

	public Guard(String URI, String localName, List<Condition> conditions, List<Action> actions) {
		super(URI, localName);
		this.conditions = conditions;
		this.actions = actions;
	}

	public List<Condition> getConditions() {
		return conditions;
	}

	public List<Action> getActions() {
		return actions;
	}
}
