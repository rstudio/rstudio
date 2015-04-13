/*
 * ProjectsServerOperations.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.model;

import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.remote.RResult;
import org.rstudio.studio.client.workbench.prefs.model.PrefsServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.JsArrayString;

public interface ProjectsServerOperations extends PrefsServerOperations,
                                                  SourceServerOperations
{  
   void getNewProjectContext(ServerRequestCallback<NewProjectContext> callback);
   
   void createProject(String projectFile,
                      NewPackageOptions newPackageOptions,
                      NewShinyAppOptions newShinyAppOptions,
                      ServerRequestCallback<Void> callback);
   
   void packageSkeleton(String packageName,
                        String packageDirectory,
                        JsArrayString sourceFiles,
                        boolean usingRcpp,
                        ServerRequestCallback<RResult<Void>> callback);
   
   void readProjectOptions(ServerRequestCallback<RProjectOptions> callback);
   
   void writeProjectOptions(RProjectOptions options,
                            ServerRequestCallback<Void> callback);
   
   void writeProjectVcsOptions(RProjectVcsOptions options,
                               ServerRequestCallback<Void> callback);
   
   void analyzeProject(ServerRequestCallback<Void> callback);
}
