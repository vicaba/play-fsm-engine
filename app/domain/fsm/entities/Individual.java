package domain.fsm.entities;

public class Individual {
	private String URI;
	private String localName;

	public Individual(String URI, String localName) {
		this.URI = URI;
		this.localName = localName;
	}

	public String getURI() {
		return URI;
	}

	public String getLocalName() {
		return localName;
	}
}
