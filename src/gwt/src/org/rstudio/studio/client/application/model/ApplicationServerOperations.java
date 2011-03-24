/*
 * ApplicationServerOperations.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JsArray;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Agreement;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public interface ApplicationServerOperations
{   
   // establish new session for this client
   void clientInit(ServerRequestCallback<SessionInfo> requestCallback);

   // interrupt the current session
   void interrupt(ServerRequestCallback<Void> requestCallback);
   
   // abort the current session
   void abort(ServerRequestCallback<Void> requestCallback);
   
   // get the http log
   void httpLog(ServerRequestCallback<JsArray<HttpLogEntry>> requestCallback);
   
   // agree to the application agreement
   void acceptAgreement(Agreement agreement, 
                        ServerRequestCallback<Void> requestCallback);
   
   // suspend the current session
   void suspendSession(ServerRequestCallback<Void> requestCallback) ;

   // check for the current save action
   void getSaveAction(ServerRequestCallback<SaveAction> requestCallback);

   // quit the current session
   void quitSession(boolean saveWorkspace, 
                    ServerRequestCallback<Void> requestCallback);
   
   // verify current credentials
   void updateCredentials();
   
   // get an application URL
   String getApplicationURL(String pathName);
}
