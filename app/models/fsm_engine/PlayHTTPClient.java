package models.fsm_engine;

import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSBodyWritables;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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

	public CompletionStage<Boolean> getRequest(String url, BiConsumer<Integer, String> postAction, int timeoutInMs) {
		return doRequest("GET", url, postAction, null, timeoutInMs);
	}

	public CompletionStage<Boolean> postRequest(String url, String bodyToSend, BiConsumer<Integer, String> postAction, int timeoutInMs) {
		return doRequest("POST", url, postAction, bodyToSend, timeoutInMs);
	}

	private CompletionStage<Boolean> doRequest(String method, String url, BiConsumer<Integer, String> postAction, String bodyToSend, int timeoutInMs) {
		CompletionStage<WSResponse> responseStage;

		WSRequest request = wsClient.url(url);
		request.setRequestTimeout(Duration.of(timeoutInMs, ChronoUnit.MILLIS));

		switch (method) {
			case "GET":
				responseStage = request.get();
				break;
			case "POST":
				responseStage = request.post(bodyToSend);
				break;
			default:
				return null;
		}

		return responseStage.handle((wsResponse, exception) -> {
			if (wsResponse == null) {
				System.out.println("There was an error with the http call, check the URL!");
			} else {
				System.out.println(wsResponse.getStatus() + " STATUS");
				String b = wsResponse.getBody(string());
				postAction.accept(wsResponse.getStatus(), b);
			}

			return true;
		});
	}
}
