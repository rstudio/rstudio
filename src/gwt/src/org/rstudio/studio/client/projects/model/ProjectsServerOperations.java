/*
 * ProjectsServerOperations.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.model;

import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.PrefsServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

public interface ProjectsServerOperations extends PrefsServerOperations,
                                                  SourceServerOperations
{  
   void createProject(String projectDirectory,
                      NewPackageOptions newPackageOptions,
                      ServerRequestCallback<Void> callback);
    
   void readProjectOptions(ServerRequestCallback<RProjectOptions> callback);
   
   void writeProjectOptions(RProjectOptions options,
                            ServerRequestCallback<Void> callback);
   
   void writeProjectVcsOptions(RProjectVcsOptions options,
                               ServerRequestCallback<Void> callback);
}
