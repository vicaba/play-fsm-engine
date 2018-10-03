package models.fsm_engine;

import java.util.concurrent.Future;
import java.util.function.BiConsumer;


public interface HTTPClient {
	Future<Boolean> postRequest(String URI, BiConsumer<Integer, String> action);
	void stop();
}
