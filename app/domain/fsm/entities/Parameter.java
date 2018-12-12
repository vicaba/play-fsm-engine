package domain.fsm.entities;

public class Parameter {
	private String placeholder;
	private String query;

	public Parameter(String placeholder, String query) {
		this.placeholder = placeholder;
		this.query = query;
	}

	public String getPlaceholder() {
		return placeholder;
	}

	public String getQuery() {
		return query;
	}
}