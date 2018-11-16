package domain.fsm.entities;

public class Action extends Individual {
	private String targetURI;
	private String method;
	private String bodyType;
	private String body;
	private int timeoutInMs;

	public Action(String URI, String localName, String targetURI, String method, String bodyType, String body, int timeoutInMs) {
		super(URI, localName);
		this.targetURI = targetURI;
		this.method = method;
		this.bodyType = bodyType;
		this.body = body;
		this.timeoutInMs = timeoutInMs;
	}

	public String getTargetURI() {
		return targetURI;
	}

	public String getMethod() {
		return method;
	}

	public String getBodyType() {
		return bodyType;
	}

	public String getBody() {
		return body;
	}

	public int getTimeoutInMs() {
		return timeoutInMs;
	}
}
