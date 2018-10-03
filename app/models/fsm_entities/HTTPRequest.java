package models.fsm_entities;

public class HTTPRequest {
	private String targetURI;
	private String method;
	private String body;

	public HTTPRequest(String targetURI, String method, String body) {
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
