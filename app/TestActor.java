import akka.actor.AbstractActor;
import akka.actor.ActorRef;

import java.time.Duration;

public class TestActor extends AbstractActor {
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.matchAny(m -> {
					System.out.println("Message received!");
					System.out.println();
					ActorRef parent = getContext().getParent();
					System.out.println("Self = " + self().path());


					System.out.println(getContext().actorSelection("akka://app/user/actor_4").resolveOneCS(Duration.ofDays(1)).toCompletableFuture().join());
					//System.out.println(getContext().getSystem().actorSelection("akka://app/user/*").));
					System.out.println("Finished");
				}).build();
	}
}
