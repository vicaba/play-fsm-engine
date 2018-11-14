package domain.fsm.engine;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import domain.fsm.engine.exceptions.InitialStateNotFoundException;
import domain.fsm.engine.exceptions.OntologyNotFoundException;
import domain.http.HTTPClient;
import application.fsm.FSMActor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.UUID;

public class FsmEngineFactory {
	private final ActorSystem actorSystem;
	private final HTTPClient httpClient;

	@Inject
	public FsmEngineFactory(ActorSystem actorSystem, HTTPClient httpClient) {
		this.actorSystem = actorSystem;
		this.httpClient = httpClient;
	}

	public ActorRef create(File file, String fsmIri, String serverURI, UUID uuid) throws OntologyNotFoundException, InitialStateNotFoundException, FileNotFoundException {
		FSMEngine fsmEngine = new FSMEngine(file, fsmIri,serverURI + "/" + uuid, httpClient, uuid);

		return actorSystem.actorOf(Props.create(FSMActor.class, fsmEngine), FSMEngine.generateActorName(uuid));
	}
}
