package org.rstudio.studio.client.common.vcs;

import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

import java.util.ArrayList;

public interface VCSServerOperations
{
   void vcsAdd(ArrayList<String> paths,
               ServerRequestCallback<Void> requestCallback);
   void vcsRemove(ArrayList<String> paths,
                  ServerRequestCallback<Void> requestCallback);
   void vcsRevert(ArrayList<String> paths,
                  ServerRequestCallback<Void> requestCallback);
}
