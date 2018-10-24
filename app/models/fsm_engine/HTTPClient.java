package models.fsm_engine;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;


public interface HTTPClient {
	CompletionStage<Boolean> getRequest(String url, BiConsumer<Integer, String> action, int timeoutInMs);

	CompletionStage<Boolean> postRequest(String url, String bodyToSend, BiConsumer<Integer, String> action, int timeoutInMs);
}
