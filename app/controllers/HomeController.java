package controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.util.Timeout;
import models.fsm_engine.FSMActor;
import models.fsm_engine.HTTPClient;
import play.mvc.*;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import views.html.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {
    @Inject HTTPClient client;
    @Inject ActorSystem actorSystem;

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {


        return ok("<h1>HOLA<\\h1>");
    }

}
