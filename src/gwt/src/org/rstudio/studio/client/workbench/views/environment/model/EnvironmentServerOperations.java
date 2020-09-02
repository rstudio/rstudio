/*
 * EnvironmentServerOperations.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.environment.model;

import java.util.List;

import com.google.gwt.core.client.JsArray;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

public interface EnvironmentServerOperations
{
   void listEnvironment(ServerRequestCallback<JsArray<RObject> > callback);

   void removeAllObjects(boolean includeHidden,
                         ServerRequestCallback<Void> requestCallback);

   void removeObjects(List<String> objectNames, 
                      ServerRequestCallback<Void> requestCallback);

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
                        ServerRequestCallback<Void> requestCallback);   
   
   void setEnvironment(String environmentName,
                       ServerRequestCallback<Void> requestCallback);
   
   void setEnvironmentFrame(int frame, 
                            ServerRequestCallback<Void> requestCallback);

   void getEnvironmentNames(
              String language,
              ServerRequestCallback<JsArray<EnvironmentFrame>> requestCallback);
   
   void getEnvironmentState(
              String language,
              String environment,
              ServerRequestCallback<EnvironmentContextData> requestCallback);
   
   void setEnvironmentMonitoring(boolean monitoring,
              ServerRequestCallback<Void> requestCallback);

   void getObjectContents(
              String objectName,
              ServerRequestCallback<ObjectContents> requestCallback);
   
   void requeryContext(ServerRequestCallback<Void> requestCallback);
   
   void environmentSetLanguage(String language,
                               ServerRequestCallback<Void> requestCallback);
   
   void isFunctionMasked(String functionName,
                         String packageName,
                         ServerRequestCallback<Boolean> requestCallback);
}
