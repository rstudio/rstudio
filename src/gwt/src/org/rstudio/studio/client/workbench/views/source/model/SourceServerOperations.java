/*
 * SourceServerOperations.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.events.GetEditorContextEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.notebook.CompileNotebookOptions;
import org.rstudio.studio.client.notebook.CompileNotebookResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintServerOperations;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.ObjectExplorerServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.IconvListResult;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;

import java.util.HashMap;
import java.util.List;

/**
 * The server manages a "working list" of documents that are being edited by
 * the user. The working list must contain the current "live" contents of
 * the tab (within a tolerance of a few seconds of latency) regardless of
 * whether the user has actually hit save. 
 */
public interface SourceServerOperations extends FilesServerOperations, 
                                                CodeToolsServerOperations,
                                                CodeSearchServerOperations,
                                                CppServerOperations,
                                                TexServerOperations,
                                                HTMLPreviewServerOperations,
                                                BuildServerOperations,
                                                PresentationServerOperations,
                                                LintServerOperations,
                                                RSConnectServerOperations,
                                                ObjectExplorerServerOperations
{
   /**
    * Create a new, empty document, without a path but with a unique ID, and
    * appends it to the current working list.
    *
    * The unique ID is necessary to distinguish between multiple docs that
    * have never been saved.
    */
   void newDocument(String fileType,
                    String contents,
                    JsObject properties,
                    ServerRequestCallback<SourceDocument> requestCallback);

   /**
    * Opens a document from disk, assigns it a unique ID, and appends it to
    * the current working list.
    */
   void openDocument(String path,
                     String fileType,
                     String encoding,
                     ServerRequestCallback<SourceDocument> requestCallback);

   /**
    * Saves the given contents for the given ID, and optionally saves it to
    * a path on disk.
    *
    * If path is null, then this is an autosave operation--nothing is written
    * to the persistent area of disk, whether this ID has a path associated
    * with it or not.
    *
    * If path is non-null, then it's a Save or Save As operation. Data will
    * be written to the actual file path, and the ID will now be associated
    * with that path.
    */
   void saveDocument(String id,
                     String path,
                     String fileType,
                     String encoding,
                     String foldSpec,
                     JsArray<ChunkDefinition> chunkOutput,
                     String contents,
                     ServerRequestCallback<String> requestCallback);

   /**
    * Same as saveDocument, but instead of sending the full contents, just
    * a diff is sent, along with a hash of the contents it expects the server
    * to currently have (before the diff is applied). 
    *
    * Note in particular that the semantics for the path parameter is the
    * same as saveDocument.
    *
    * If the return value is null, the save failed for some reason and
    * saveDocument() should be used as a fallback. If the return value is
    * non-null, it is the hash value of the new contents.
    */
   void saveDocumentDiff(String id,
                         String path,
                         String fileType,
                         String encoding,
                         String foldSpec,
                         JsArray<ChunkDefinition> chunkOutput,
                         String replacement,
                         int offset,
                         int length,
                         String hash,
                         ServerRequestCallback<String> requestCallback);

   void checkForExternalEdit(
         String id,
         ServerRequestCallback<CheckForExternalEditResult> requestCallback);

   void ignoreExternalEdit(String id,
                           ServerRequestCallback<Void> requestCallback);

   /**
    * Removes an item from the working list.
    */
   void closeDocument(String id, ServerRequestCallback<Void> requestCallback);

   /**
    * Clears the working list.
    */
   void closeAllDocuments(ServerRequestCallback<Void> requestCallback);

   void setSourceDocumentOnSave(String id, boolean shouldSourceOnSave,
                                ServerRequestCallback<Void> requestCallback);

   void saveActiveDocument(String contents,
                           boolean sweave,
                           String rnwWeave,
                           ServerRequestCallback<Void> requestCallback);

   /**
    * Applies the values in the given HashMap to the document's property bag.
    * This does NOT replace all of the doc's properties on the server; any
    * properties that already exist but are not present in the HashMap, are
    * left unchanged. If a HashMap entry has a null value, that property
    * should be removed. 
    * These properties are durable (they exist even after the document closed).
    * This makes them suitable for long-term document meta-data such as 
    * publishing history. However, note that they are actually associated with
    * the path rather than the document and are not currently cleaned up if
    * files are deleted. Therefore they can be "resurrected" to be associated
    * with a different file if another file with the same path is created.
    */
   void modifyDocumentProperties(String id, HashMap<String, String> properties,
                                 ServerRequestCallback<Void> requestCallback);
   
   void getDocumentProperties(String path, 
                              ServerRequestCallback<JsObject> requestCallback);

   void revertDocument(String id,
                       String fileType,
                       ServerRequestCallback<SourceDocument> requestCallback);
   
   void reopenWithEncoding(
                       String id,
                       String encoding,
                       ServerRequestCallback<SourceDocument> requestCallback);

   void removeContentUrl(String contentUrl,
                         ServerRequestCallback<Void> requestCallback);

   void detectFreeVars(String code,
                       ServerRequestCallback<JsArrayString> requestCallback);

   void iconvlist(ServerRequestCallback<IconvListResult> requestCallback);
   
   void getSourceTemplate(String name,
                          String template,
                          ServerRequestCallback<String> requestCallback);
   
   void createRdShell(String name,
                      String type,
                      ServerRequestCallback<RdShellResult> requestCallback);
   

   void createNotebook(
         CompileNotebookOptions options,
         ServerRequestCallback<CompileNotebookResult> requestCallback);
   
   void isReadOnlyFile(String path, 
                       ServerRequestCallback<Boolean> requestCallback);
   
   void getScriptRunCommand(String interpreter,
                            String path,
                            ServerRequestCallback<String> requestCallback);
   
   void getMinimalSourcePath(String path, 
                             ServerRequestCallback<String> requestCallback);
   
   void setDocOrder(
         List<String> order, ServerRequestCallback<Void> requestCallback); 
   
   void removeCachedData(String cacheKey, 
                         ServerRequestCallback<Void> requestCallback);
   
   void ensureFileExists(String path,
                         ServerRequestCallback<Boolean> requestCallback);
   
   public void getFileContents(String path,
                               String encoding,
                               ServerRequestCallback<String> requestCallback);
   
   public void executeRCode(String code,
                            ServerRequestCallback<String> requestCallback);
   
   public void getSourceDocument(String docId,
                ServerRequestCallback<SourceDocument> requestCallback);
   
   public void createShinyApp(String appName,
                              String appType,
                              String appDir,
                              ServerRequestCallback<JsArrayString> requestCallback);
   
   public void getEditorContextCompleted(GetEditorContextEvent.SelectionData data,
                                         ServerRequestCallback<Void> requestCallback);
   
   public void setSourceDocumentDirty(String docId, boolean dirty,
         ServerRequestCallback<Void> requestCallback);
   
   public void extractRmdFromNotebook(String inputPath,
         ServerRequestCallback<SourceDocumentResult> requestCallback);

   public void defaultSqlConnectionName(ServerRequestCallback<String> requestCallback);
}
