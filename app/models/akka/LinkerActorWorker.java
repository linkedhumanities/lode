package models.akka;

import java.util.List;

import models.akka.messages.ConfigMessage;
import models.akka.messages.ErrorMessage;
import models.akka.messages.ResultMessage;
import models.akka.messages.UpdateMessage;
import models.entity.EntityContainer;
import models.states.Endpoint;
import sparql.query.Content;
import sparql.update.Linker;
import akka.actor.UntypedActor;


public class LinkerActorWorker extends UntypedActor
{

    @Override
    public void onReceive(Object message)
    {
        ConfigMessage cm = (ConfigMessage) message;

        EntityContainer desc = Content.getEntityContainerbyID(cm.getObject());
        if(desc == null) {
            this.context().parent().tell(new ErrorMessage(4044, new Exception("The entity container is null (" + cm.getObject() + ").")), null);
            return;
        }

        if(desc.getProperties().size() == 0) {
            Content.show(cm.getObject());
            desc = Content.getEntityContainerbyID(cm.getObject());
        }

        this.context().parent().tell(new UpdateMessage("Step 1: ", 1, "Initialization"), null);

        List<EntityContainer> suggestions = null;
        try {
            suggestions = Linker.getProposals(cm.getObject(), Endpoint.valueOf(cm.getDestination().toUpperCase()), cm.getMethod(), this.context().parent());
        } catch(Exception e) {
            this.context().parent().tell(new ErrorMessage(4041, e), null);
            return;
        }

        this.context().parent().tell(new UpdateMessage("Step 4: ", 100, "Complete"), null);
        this.context().parent().tell(new ResultMessage(cm.getDestination(), desc, suggestions, cm.getMethod()), null);
    }
}