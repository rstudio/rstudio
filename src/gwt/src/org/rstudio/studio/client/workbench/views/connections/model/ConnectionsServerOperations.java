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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public interface ConnectionsServerOperations extends CryptoServerOperations
{
   void removeConnection(ConnectionId id, ServerRequestCallback<Void> callback);
 
   void getDisconnectCode(Connection connection, 
                          ServerRequestCallback<String> callback);
   
   void showSparkLog(Connection connection, 
                     ServerRequestCallback<Void> callback);
   
   void showSparkUI(Connection connection, 
                    ServerRequestCallback<Void> callback);
   
   void connectionListTables(Connection connection,
                             ServerRequestCallback<JsArrayString> callback);
   
   void connectionListFields(Connection connection,
                             String table,
                             ServerRequestCallback<JsArray<Field>> callback);
   
   void connectionPreviewTable(Connection connection,
                               String table,
                               ServerRequestCallback<Void> callback);
   
   void getNewSparkConnectionContext(
            ServerRequestCallback<NewSparkConnectionContext> callback);
   
   void installSpark(String sparkVersion,
                     String hadoopVersion,
                     ServerRequestCallback<ConsoleProcess> callback);
}
