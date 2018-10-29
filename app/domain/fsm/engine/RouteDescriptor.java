package domain.fsm.engine;

import java.util.function.Consumer;

public class RouteDescriptor {
	private String relativePath;
	private String method;
	private String body;
	private Consumer<String> consumer;

	public RouteDescriptor(String relativePath, String method, String body, Consumer<String> consumer) {
		this.relativePath = relativePath;
		this.method = method;
		this.body = body;
		this.consumer = consumer;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public String getMethod() {
		return method;
	}

	public String getBody() {
		return body;
	}

	public Consumer<String> getConsumer() {
		return consumer;
	}
}
