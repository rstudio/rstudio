/*
 * WorkspaceServerOperations.java
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
package org.rstudio.studio.client.workbench.views.workspace.model;

import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

public interface WorkspaceServerOperations
{
   // list all objects in the global namespace
   void listObjects(
         ServerRequestCallback<RpcObjectList<WorkspaceObjectInfo>> requestCallback);
   
   void removeAllObjects(boolean includeHidden,
                         ServerRequestCallback<Void> requestCallback);
      
   // set the value of an object in the global namespace
   void setObjectValue(String objectName,
                       String value,
                       ServerRequestCallback<Void> requestCallback);

   void getObjectValue(String objectName,
                  ServerRequestCallback<RpcObjectList<WorkspaceObjectInfo>> serverRequestCallback);

   void downloadDataFile(String dataFileURL,
                         ServerRequestCallback<DownloadInfo> requestCallback);
   
   void getDataPreview(
         String dataFilePath,
         ServerRequestCallback<DataPreviewResult> requestCallback);

   void getOutputPreview(
         String dataFilePath,
         boolean heading,
         String separator,
         String decimal,
         String quote,
         ServerRequestCallback<DataPreviewResult> requestCallback);
}
