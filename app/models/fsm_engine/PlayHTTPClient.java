package models.fsm_engine;

import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSBodyWritables;
import play.libs.ws.WSClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

@Singleton
public class PlayHTTPClient implements HTTPClient, WSBodyReadables, WSBodyWritables {
	private final WSClient wsClient;

	@Inject
	public PlayHTTPClient(WSClient wsClient) {
		this.wsClient = wsClient;
	}

	public void stop() {
		try {
			wsClient.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Future<Boolean> postRequest(String URI, BiConsumer<Integer, String> action) {
		CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

		wsClient.url(URI).get()
				.whenComplete((response, throwable) -> {
					String body = response.getBody(string());
					action.accept(response.getStatus(), body);
					completableFuture.complete(true);
				});

		return completableFuture;
	}

}
