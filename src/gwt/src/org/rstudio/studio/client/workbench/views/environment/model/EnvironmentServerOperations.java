/*
 * EnvironmentServerOperations.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.environment.model;

import java.util.List;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;

import com.google.gwt.core.client.JsArray;

public interface EnvironmentServerOperations
{
   void listEnvironment(ServerRequestCallback<JsArray<RObject> > callback);

   void removeAllObjects(boolean includeHidden,
                         ServerRequestCallback<VoidResponse> requestCallback);

   void removeObjects(List<String> objectNames, 
                      ServerRequestCallback<VoidResponse> requestCallback);

   void downloadDataFile(String dataFileURL,
                         ServerRequestCallback<DownloadInfo> requestCallback);

   void getDataPreview(
           String dataFilePath,
           ServerRequestCallback<DataPreviewResult> requestCallback);

   void getOutputPreview(
           String dataFilePath,
           String encoding,
           boolean heading,
           String separator,
           String decimal,
           String quote,
           String comment,
           ServerRequestCallback<DataPreviewResult> requestCallback);

   void setContextDepth(int newContextDepth,
                        ServerRequestCallback<VoidResponse> requestCallback);   
   
   void setEnvironment(String environmentName,
                       ServerRequestCallback<VoidResponse> requestCallback);
   
   void setEnvironmentFrame(int frame, 
                            ServerRequestCallback<VoidResponse> requestCallback);

   void getEnvironmentNames(
              String language,
              ServerRequestCallback<JsArray<EnvironmentFrame>> requestCallback);
   
   void getEnvironmentState(
              String language,
              String environment,
              ServerRequestCallback<EnvironmentContextData> requestCallback);
   
   void setEnvironmentMonitoring(boolean monitoring,
              ServerRequestCallback<VoidResponse> requestCallback);

   void getObjectContents(
              String objectName,
              ServerRequestCallback<ObjectContents> requestCallback);
   
   void requeryContext(ServerRequestCallback<VoidResponse> requestCallback);
   
   void environmentSetLanguage(String language,
                               ServerRequestCallback<VoidResponse> requestCallback);
   
   void isFunctionMasked(String functionName,
                         String packageName,
                         ServerRequestCallback<Boolean> requestCallback);

   void getMemoryUsageReport(ServerRequestCallback<MemoryUsageReport> requestCallback);
}
