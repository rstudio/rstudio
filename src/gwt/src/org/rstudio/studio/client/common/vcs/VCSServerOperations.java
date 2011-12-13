package org.rstudio.studio.client.common.vcs;

import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface VCSServerOperations extends CryptoServerOperations
{
   void vcsClone(VcsCloneOptions options,
                 ServerRequestCallback<ConsoleProcess> requestCallback);

}
