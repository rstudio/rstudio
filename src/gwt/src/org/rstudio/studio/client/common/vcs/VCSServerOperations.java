package org.rstudio.studio.client.common.vcs;

import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

import java.util.ArrayList;

public interface VCSServerOperations
{
   void vcsRevert(ArrayList<String> paths,
                  ServerRequestCallback<Void> requestCallback);
}
