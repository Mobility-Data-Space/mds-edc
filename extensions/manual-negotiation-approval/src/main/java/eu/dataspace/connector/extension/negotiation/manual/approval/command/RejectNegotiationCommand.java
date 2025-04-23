package eu.dataspace.connector.extension.negotiation.manual.approval.command;

import org.eclipse.edc.spi.command.EntityCommand;

public class RejectNegotiationCommand extends EntityCommand {

    public RejectNegotiationCommand(String entityId) {
        super(entityId);
    }

}
