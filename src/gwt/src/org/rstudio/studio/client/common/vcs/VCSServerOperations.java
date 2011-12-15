package org.rstudio.studio.client.common.vcs;

import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

public interface VCSServerOperations extends CryptoServerOperations
{
   void askpassCompleted(String value, boolean remember,
         ServerRequestCallback<Void> requestCallback);
   
   void vcsClone(VcsCloneOptions options,
                 ServerRequestCallback<ConsoleProcess> requestCallback);

}
