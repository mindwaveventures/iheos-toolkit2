package gov.nist.toolkit.xdstools2.client.command.command;

import gov.nist.toolkit.xdstools2.client.initialization.XdsTools2Presenter;
import gov.nist.toolkit.xdstools2.shared.command.CommandContext;

import java.util.List;

/**
 * Created by onh2 on 11/3/16.
 */
public abstract class ReloadExternalSitesCommand extends GenericCommand<CommandContext,List<String>>{
    @Override
    public void run(CommandContext var1) {
        XdsTools2Presenter.data().getToolkitServices().reloadExternalSites(var1,this);
    }
}