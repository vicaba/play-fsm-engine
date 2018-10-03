package models.fsm_engine;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.util.ArrayList;
import java.util.List;

public class Main {

	public static void main(String[] args) {
		try {
			/*ActorSystem actorSystem = ActorSystem.create("engine_system");

			HTTPClient httpClient = new PlayHTTPClient();
			FSMEngine fsmEngine = new FSMEngine(httpClient);

			ActorRef actorRef = actorSystem.actorOf(Props.create(FSMActor.class, httpClient, fsmEngine), "FSM_Actor");

			actorRef.tell(new ChangeStateMessage(), ActorRef.noSender());*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static List<RouteDescriptor> getRoutesDescriptor(FSMEngine fsmEngine) {
		List<RouteDescriptor> routesDescriptor = new ArrayList<>();

		routesDescriptor.add(new RouteDescriptor("sendData", "post", "hola que tal", fsmEngine::insertData));

		routesDescriptor.add(new RouteDescriptor("FC", "post", "hola que tal", fsmEngine::insertData));

		return routesDescriptor;
	}
}
