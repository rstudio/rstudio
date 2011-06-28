package org.rstudio.studio.client.projects.model;

import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface ProjectsServerOperations
{  
   void createProject(String projectDirectory, 
                      ServerRequestCallback<Void> requestCallback);

}
