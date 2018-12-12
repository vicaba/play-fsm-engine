package domain.fsm.entities;

import java.util.List;

public class HTTPRequest {
	private String absoluteURI;
	private String URIstructure;
	private List<Parameter> parameters;
	private String method;
	private String body;

	public HTTPRequest(String absoluteURI, String method, String body) {
		this.absoluteURI = absoluteURI;
		this.method = method;
		this.body = body;
		this.parameters = null;
		this.URIstructure = null;
	}

	public HTTPRequest(String URIstructure, List<Parameter> parameters, String method, String body) {
		this(null, method, body);
		this.URIstructure = URIstructure;
		this.parameters = parameters;
	}

	public String getAbsoluteURI() {
		return absoluteURI;
	}

	public String getMethod() {
		return method;
	}

	public String getBody() {
		return body;
	}

	public String getURIstructure() {
		return URIstructure;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public boolean hasAbsoluteURI() {
		return absoluteURI != null;
	}
}
