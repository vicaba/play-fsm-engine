package domain.fsm.entities;

import java.util.List;

public class Action extends Individual {
	private String absoluteUri;
	private String uriStructure;
	private List<Parameter> parameters;
	private String method;
	private String body;
	private String bodyType;
	private int timeoutInMs;

	public Action(String URI, String localName, String absoluteURI, String method, String bodyType, String body, int timeoutInMs) {
		super(URI, localName);

		this.absoluteUri = absoluteURI;
		this.method = method;
		this.body = body;
		this.parameters = null;
		this.uriStructure = null;

		this.bodyType = bodyType;
		this.timeoutInMs = timeoutInMs;
	}

	public Action(String URI, String localName, String uriStructure, List<Parameter> parameters, String method, String bodyType, String body, int timeoutInMs) {
		this(URI, localName, null, method, bodyType, body, timeoutInMs);

		this.parameters = parameters;
		this.uriStructure = uriStructure;
	}

	public String getAbsoluteUri() {
		return absoluteUri;
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

	public boolean hasAbsoluteURI() {
		return absoluteUri != null;
	}

	public String getUriStructure() {
		return uriStructure;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}
}
