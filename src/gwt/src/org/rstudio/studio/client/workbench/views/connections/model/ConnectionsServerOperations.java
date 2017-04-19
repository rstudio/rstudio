/*
 * ConnectionsServerOperations.java
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
package org.rstudio.studio.client.workbench.views.connections.model;

import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.remote.RResult;

import com.google.gwt.core.client.JsArray;

public interface ConnectionsServerOperations extends CryptoServerOperations
{
   void removeConnection(ConnectionId id, ServerRequestCallback<Void> callback);
 
   void connectionDisconnect(ConnectionId connectionId, 
                             ServerRequestCallback<Void> callback);
   
   void connectionExecuteAction(ConnectionId connectionId, 
                                String action,
                                ServerRequestCallback<Void> callback);
   
   void connectionListObjects(ConnectionId connectionId,
                              ConnectionObjectSpecifier object,
                              ServerRequestCallback<JsArray<DatabaseObject>> callback);
   
   void connectionListFields(ConnectionId connectionId,
                             ConnectionObjectSpecifier object,
                             ServerRequestCallback<JsArray<Field>> callback);
   
   void connectionPreviewObject(ConnectionId connectionId,
                                ConnectionObjectSpecifier object,
                                ServerRequestCallback<Void> callback);
   
   void getNewConnectionContext(
            ServerRequestCallback<NewConnectionContext> callback);
   
   void installSpark(String sparkVersion,
                     String hadoopVersion,
                     ServerRequestCallback<ConsoleProcess> callback);

   void launchEmbeddedShinyConnectionUI(String packageName,
                                        String connectionName,
                                        ServerRequestCallback<RResult<Void>> serverRequestCallback);

   void connectionTest(String code, 
                       ServerRequestCallback<String> callback);
}
