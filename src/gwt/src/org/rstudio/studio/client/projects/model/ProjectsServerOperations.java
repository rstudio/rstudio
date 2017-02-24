/*
 * ProjectsServerOperations.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public interface ProjectsServerOperations extends PrefsServerOperations,
                                                  SourceServerOperations
{  
   void validateProjectPath(String projectPath, 
                            ServerRequestCallback<Boolean> callback);
   
   void getNewProjectContext(ServerRequestCallback<NewProjectContext> callback);
     
   void createProject(String projectFile,
                      NewPackageOptions newPackageOptions,
                      NewShinyAppOptions newShinyAppOptions,
                      ProjectTemplateOptions projectTemplateOptions,
                      ServerRequestCallback<Void> callback);
   
   void createProjectFile(String projectFile,
                          ServerRequestCallback<Boolean> callback);
   
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
   
   void getProjectSharedUsers(
         ServerRequestCallback<JsArray<ProjectUserRole>> callback);
   
   void setProjectSharedUsers(JsArrayString users, 
                              ServerRequestCallback<SharingResult> callback);
   
   void validateSharingConfig(
         ServerRequestCallback<SharingConfigResult> callback);
   
   void getAllServerUsers(ServerRequestCallback<JsArrayString> callback);
   
   void getSharedProjects(
         int maxProjects,
         ServerRequestCallback<JsArray<SharedProjectDetails>> callback);
   
   void setCurrentlyEditing(String path,
         String id,
         ServerRequestCallback<Void> callback);
   
   void reportCollabDisconnected(String path, 
         String id, 
         ServerRequestCallback<Void> callback);
   
   void getProjectUser(String sessionId, 
         ServerRequestCallback<ProjectUser> callback);
   
   void setFollowingUser(String sessionId,
         ServerRequestCallback<Void> callback);
}
