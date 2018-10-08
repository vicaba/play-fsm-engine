package models.fsm_engine;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import models.fsm_engine.Exceptions.InitialStateNotFoundException;
import models.fsm_engine.Exceptions.OntologyNotFoundException;

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

	public ActorRef create(File file, UUID uuid) throws OntologyNotFoundException, InitialStateNotFoundException, FileNotFoundException {
		FSMEngine fsmEngine = new FSMEngine(httpClient, file);

		return actorSystem.actorOf(Props.create(FSMActor.class, httpClient, fsmEngine), FSMEngine.generateActorName(uuid));
	}
}
