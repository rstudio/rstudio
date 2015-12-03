package org.rstudio.studio.client.workbench.addins;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;

public interface AddinsServerOperations
{
   void getRAddins(ServerRequestCallback<RAddins> requestCallback);
   void executeRAddin(String commandId, ServerRequestCallback<Void> requestCallback);
}
