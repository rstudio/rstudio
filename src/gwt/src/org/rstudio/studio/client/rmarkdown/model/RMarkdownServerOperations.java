/*
 * RMarkdownServerOperations.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.rmarkdown.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public interface RMarkdownServerOperations extends CryptoServerOperations
{
   void getRMarkdownContext(
            ServerRequestCallback<RMarkdownContext> requestCallback);
       
   void renderRmd(String file, int line, String format, String encoding,
                  String paramsFile, boolean asTempfile, int type,
                  String existingOutputFile, String workingDir,
                  String viewerType,
                  ServerRequestCallback<Boolean> requestCallback);
   
   void renderRmdSource(String source,
                        ServerRequestCallback<Boolean> requestCallback);
   
   void maybeCopyWebsiteAsset(String file,
                         ServerRequestCallback<Boolean> requestCallback);
   
   void terminateRenderRmd(boolean normal, 
                           ServerRequestCallback<Void> requestCallback);
   
   void rmdOutputFormat(String file, 
                        String encoding, 
                        ServerRequestCallback<String> requestCallback);
   
   void convertToYAML(JavaScriptObject input, 
                      ServerRequestCallback<RmdYamlResult> requestCallback);

   void convertFromYAML(String input, 
                        ServerRequestCallback<RmdYamlData> requestCallback);

   void getRmdTemplates(
          ServerRequestCallback<JsArray<RmdDocumentTemplate>> requestCallback);
   
   void createRmdFromTemplate(String filePath, 
                              String templatePath, 
                              boolean createDirectory, 
                              ServerRequestCallback<RmdCreatedTemplate> requestCallback);
   
   void getRmdTemplate(String templatePath, 
                       ServerRequestCallback<RmdTemplateContent> requestCallback);
   
   public String getApplicationURL(String pathName);
   
   public String getFileUrl(FileSystemItem file);
   
   void prepareForRmdChunkExecution(String id,
                ServerRequestCallback<RmdExecutionState> requestCallback);

   void getRmdOutputInfo(String target,
                ServerRequestCallback<RmdOutputInfo> resultCallback);
   
   void refreshChunkOutput(String docPath, String docId, String contextId,
                           String requestId, String chunkId,
                           ServerRequestCallback<NotebookDocQueue> requestCallback);
   
   void setChunkConsole(String docId, String chunkId, int commitMode, 
                        int execMode, int execScope, String options, 
                        int pixelWidth, int characterWidth, 
                        ServerRequestCallback<RmdChunkOptions> requestCallback);
   
   void createNotebookFromCache(String rmdPath, String outputPath, 
         ServerRequestCallback<NotebookCreateResult> requestCallback);
   
   void replayNotebookPlots(String docId, String initialChunkId, int pixelWidth, 
         int pixelHeight, ServerRequestCallback<String> requestCallback);

   void replayNotebookChunkPlots(String docId, String chunkId, int pixelWidth, 
         int pixelHeight, ServerRequestCallback<String> requestCallback);

   void cleanReplayNotebookChunkPlots(String docId, String chunkId, 
         ServerRequestCallback<Void> requestCallback);
   
   void executeNotebookChunks(NotebookDocQueue queue, 
         ServerRequestCallback<Void> requestCallback);
   
   void updateNotebookExecQueue(NotebookQueueUnit unit, int op, 
         String beforeChunkId, ServerRequestCallback<Void> requestCallback);
   
   void executeAlternateEngineChunk(String docId,
                                    String chunkId,
                                    int commitMode,
                                    String engine,
                                    String code,
                                    JsObject options,
                                    ServerRequestCallback<String> requestCallback);
   
   void interruptChunk(String docId,
                       String chunkId,
                       ServerRequestCallback<Void> requestCallback);
}
