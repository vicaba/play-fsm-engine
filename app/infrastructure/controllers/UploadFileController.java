package infrastructure.controllers;

import akka.actor.ActorRef;
import akka.routing.Router;
import domain.fsm.engine.OnStateChangeMessage;
import domain.fsm.engine.FsmEngineFactory;
import domain.fsm.engine.exceptions.InitialStateNotFoundException;
import domain.fsm.engine.exceptions.OntologyNotFoundException;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.*;

import javax.inject.Inject;
import java.io.File;
import java.util.UUID;

public class UploadFileController extends Controller {
	private final FsmEngineFactory fsmEngineFactory;
	private final FormFactory formFactory;

	@Inject
	public UploadFileController(FsmEngineFactory fsmEngineFactory, FormFactory formFactory) {
		this.fsmEngineFactory = fsmEngineFactory;
		this.formFactory = formFactory;
	}

	public Result upload() {
		Http.MultipartFormData<File> body = request().body().asMultipartFormData();
		Http.MultipartFormData.FilePart<File> ontology = body.getFile("ontology");
		if (ontology != null) {
			String fileName = ontology.getFilename();
			String contentType = ontology.getContentType();
			File file = ontology.getFile();

			try {
				DynamicForm requestData = formFactory.form().bindFromRequest();
				String uuidString = requestData.get("fsm_id");
				String fsmIri = requestData.get("fsm_iri");

				UUID uuid = UUID.fromString(uuidString);

				try {
					String serverUrl = "http://localhost:9000";
					System.out.println("Server URL = " + serverUrl);

					ActorRef actorRef = fsmEngineFactory.create(file, fsmIri, serverUrl, uuid);

					actorRef.path();
					actorRef.tell(new OnStateChangeMessage(), ActorRef.noSender());

					return redirect(infrastructure.controllers.routes.FsmClientController.createFsmClient(uuid.toString()));
				} catch (OntologyNotFoundException e) {
					return badRequest("FSM not found, the FSM IRI must be fully specified");
				} catch (InitialStateNotFoundException e) {
					return badRequest("Initial state not found");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			flash("error", "Bad ontology file");
			return badRequest();
		} else {
			flash("error", "Missing file");
			return badRequest();
		}
	}
}
