/*
 * ApplicationServerOperations.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.application.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Agreement;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.PrefsServerOperations;

import com.google.gwt.core.client.JsArray;

public interface ApplicationServerOperations extends PrefsServerOperations
{   
   // establish new session for this client
   void clientInit(ServerRequestCallback<SessionInfo> requestCallback);

   // interrupt the current session
   void interrupt(ServerRequestCallback<Void> requestCallback);
   
   // abort the current session
   void abort(String nextSessionProject,
              ServerRequestCallback<Void> requestCallback);
     
   // agree to the application agreement
   void acceptAgreement(Agreement agreement, 
                        ServerRequestCallback<Void> requestCallback);
   
   // suspend the current session
   void suspendSession(boolean force,
                       ServerRequestCallback<Void> requestCallback) ;

   // handle unsaved changes completed
   void handleUnsavedChangesCompleted(
                        boolean handled,
                        ServerRequestCallback<Void> requestCallback);
   
   // quit the current session
   void quitSession(boolean saveWorkspace, 
                    String switchToProjectPath,
                    RVersionSpec switchToRVersion,
                    String hostPageUrl,
                    ServerRequestCallback<Boolean> requestCallback);
   
   // verify current credentials
   void updateCredentials();
   
   // event listener
   void stopEventListener();
   void ensureEventListener();
   
   // get an application URL
   String getApplicationURL(String pathName);
   
   String getFileUrl(FileSystemItem file);
   
   void suspendForRestart(SuspendOptions options,
                          ServerRequestCallback<Void> requestCallback);
   void ping(ServerRequestCallback<Void> requestCallback);

   public void checkForUpdates(
         boolean manual,
         ServerRequestCallback<UpdateCheckResult> requestCallback);

   public void getProductInfo(
         ServerRequestCallback<ProductInfo> requestCallback);
   
   void getNewSessionUrl(String hostPageUrl,
         boolean isProject, 
         String directory,
         RVersionSpec rVersion,
         ServerRequestCallback<String> callback);
   
   void getActiveSessions(
      String hostPageUrl,
      ServerRequestCallback<JsArray<ActiveSession>> callback);
   
   void getAvailableRVersions(
      ServerRequestCallback<JsArray<RVersionSpec>> callback);
   
   void getProjectRVersion(
         String projectDir,
         ServerRequestCallback<RVersionSpec> callback);
   
   void getProjectFilePath(
         String projectId,
         ServerRequestCallback<String> callback);
   
   void setSessionLabel(String label,
         ServerRequestCallback<Void> requestCallback);
}
