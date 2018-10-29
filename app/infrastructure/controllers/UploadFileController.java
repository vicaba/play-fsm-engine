package infrastructure.controllers;

import akka.actor.ActorRef;
import controllers.routes;
import domain.fsm.engine.OnStateChangeMessage;
import domain.fsm.engine.FsmEngineFactory;
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
				String uuidString = requestData.get("ws_id");

				UUID uuid = UUID.fromString(uuidString);

				ActorRef actorRef = fsmEngineFactory.create(file, uuid);

				actorRef.path();
				actorRef.tell(new OnStateChangeMessage(), ActorRef.noSender());

				return redirect(infrastructure.controllers.routes.FsmClientController.createFsmClient(uuid.toString()));
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
