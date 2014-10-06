package models.akka;

import models.akka.messages.ConfigMessage;
import models.akka.messages.ErrorMessage;
import models.akka.messages.ResultMessage;
import models.akka.messages.StatusMessage;
import models.akka.messages.UpdateMessage;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;


public class LinkerActor extends UntypedActor
{
    private UpdateMessage um = new UpdateMessage("Step 1: ", 0, "Initialization");
    private ResultMessage rm = null;
    private ErrorMessage  em = null;


    @Override
    public void onReceive(Object message) throws Exception
    {
        if(message instanceof ConfigMessage) {
            ActorRef child = this.getContext().actorOf(Props.create(LinkerActorWorker.class));
            child.tell(message, null);
            child.tell(akka.actor.PoisonPill.getInstance(), null);  // kill the child after the work is complete...
        }

        if(message instanceof UpdateMessage) {
            UpdateMessage um = (UpdateMessage) message;
            this.um = um;
        }

        if(message instanceof StatusMessage) {
            if(em != null) {
                getSender().tell(em, getSelf());
                this.getSelf().tell(akka.actor.PoisonPill.getInstance(), null);
            } else if(rm != null) {
                getSender().tell(rm, getSelf());
                this.getSelf().tell(akka.actor.PoisonPill.getInstance(), null);
            } else {
                getSender().tell(um.toString(), getSelf());
            }
        }

        if(message instanceof ResultMessage) {
            ResultMessage rm = (ResultMessage) message;
            this.rm = rm;
        }

        if(message instanceof ErrorMessage) {
            ErrorMessage em = (ErrorMessage) message;
            this.em = em;
        }
    }
}