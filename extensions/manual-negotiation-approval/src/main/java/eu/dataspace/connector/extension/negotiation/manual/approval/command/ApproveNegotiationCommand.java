package eu.dataspace.connector.extension.negotiation.manual.approval.command;

import org.eclipse.edc.spi.command.EntityCommand;

public class ApproveNegotiationCommand extends EntityCommand {

    public ApproveNegotiationCommand(String entityId) {
        super(entityId);
    }

}
