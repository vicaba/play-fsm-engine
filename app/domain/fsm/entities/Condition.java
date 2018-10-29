package domain.fsm.entities;

public class Condition extends Individual {
	private String sparqlQuery;

	public Condition(String URI, String localName, String sparqlQuery) {
		super(URI, localName);
		this.sparqlQuery = sparqlQuery;
	}

	public String getSparqlQuery() {
		return sparqlQuery;
	}

	public boolean hasQuery() {
		return sparqlQuery != null;
	}
}
