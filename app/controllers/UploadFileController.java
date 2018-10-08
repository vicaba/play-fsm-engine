package controllers;

import akka.actor.ActorRef;
import models.fsm_engine.ChangeStateMessage;
import models.fsm_engine.FSMEngine;
import models.fsm_engine.FsmEngineFactory;
import play.mvc.*;

import javax.inject.Inject;
import java.io.File;
import java.util.UUID;

public class UploadFileController extends Controller {
	private final FsmEngineFactory fsmEngineFactory;

	@Inject
	public UploadFileController(FsmEngineFactory fsmEngineFactory) {
		this.fsmEngineFactory = fsmEngineFactory;
	}

	public Result upload() {
		Http.MultipartFormData<File> body = request().body().asMultipartFormData();
		Http.MultipartFormData.FilePart<File> picture = body.getFile("picture");
		if (picture != null) {
			String fileName = picture.getFilename();
			String contentType = picture.getContentType();
			File file = picture.getFile();

			try {
				final var uuid = UUID.randomUUID();

				ActorRef actorRef = fsmEngineFactory.create(file, uuid);

				actorRef.path();
				actorRef.tell(new ChangeStateMessage(), ActorRef.noSender());

				return redirect(routes.FsmClientController.createFsmClient(uuid.toString()));
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
