import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Main {
	public static void main(String[] args) {
		ActorSystem actorSystem = ActorSystem.create("app");

		ActorRef actor1 = actorSystem.actorOf(Props.create(TestActor.class), "actor_1");
		ActorRef actor2 = actorSystem.actorOf(Props.create(TestActor.class), "actor_2");
		ActorRef actor3 = actorSystem.actorOf(Props.create(TestActor.class), "actor_3");
		ActorRef actor4 = actorSystem.actorOf(Props.create(TestActor.class), "actor_4");

		actor1.tell("hola", actor1);
		System.out.println("Message told to actor_1");
	}
}
