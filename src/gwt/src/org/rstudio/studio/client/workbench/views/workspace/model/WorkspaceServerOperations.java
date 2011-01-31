/*
 * WorkspaceServerOperations.java
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
package org.rstudio.studio.client.workbench.views.workspace.model;

import com.google.gwt.core.client.JsArray;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

public interface WorkspaceServerOperations
{
   // list all objects in the global namespace
   void listObjects(
         ServerRequestCallback<RpcObjectList<WorkspaceObjectInfo>> requestCallback);
   
   void removeAllObjects(ServerRequestCallback<Void> requestCallback);
      
   // set the value of an object in the global namespace
   void setObjectValue(String objectName,
                       String value,
                       ServerRequestCallback<Void> requestCallback);

   void getObjectValue(String objectName,
                  ServerRequestCallback<RpcObjectList<WorkspaceObjectInfo>> serverRequestCallback);

   void saveWorkspace(String filename,
                      ServerRequestCallback<Void> serverRequestCallback);

   void loadWorkspace(String filename,
                      ServerRequestCallback<Void> serverRequestCallback);
   
 
   void listGoogleSpreadsheets(
         String titlePattern,             // null for all spreadsheets
         int maxResults,
         ServerRequestCallback<JsArray<GoogleSpreadsheetInfo>> requestCallback);
   
   void importGoogleSpreadsheet(GoogleSpreadsheetImportSpec importSpec,
                                ServerRequestCallback<Void> requestCallback);

   void downloadDataFile(String dataFileURL,
                         ServerRequestCallback<DownloadInfo> requestCallback);
   
   void getDataPreview(
         String dataFilePath,
         ServerRequestCallback<DataPreviewResult> requestCallback);

   void getOutputPreview(
         String dataFilePath,
         boolean heading,
         String separator,
         String quote,
         ServerRequestCallback<DataPreviewResult> requestCallback);
}
