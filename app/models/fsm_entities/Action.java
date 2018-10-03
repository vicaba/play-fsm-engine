package models.fsm_entities;

public class Action extends Individual {
	private String targetURI;
	private String method;
	private String body;

	public Action(String URI, String localName, String targetURI, String method, String body) {
		super(URI, localName);
		this.targetURI = targetURI;
		this.method = method;
		this.body = body;
	}

	public String getTargetURI() {
		return targetURI;
	}

	public String getMethod() {
		return method;
	}

	public String getBody() {
		return body;
	}
}
