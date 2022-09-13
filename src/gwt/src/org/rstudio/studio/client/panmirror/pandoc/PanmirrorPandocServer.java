/*
 * PanmirrorPandocServer.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.panmirror.pandoc;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.promise.PromiseServerRequestCallback;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;

import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;

import jsinterop.annotations.JsType;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;


@JsType
public class PanmirrorPandocServer {
   
  
   public PanmirrorPandocServer() {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(PanmirrorPandocServerOperations server)
   {
      server_ = server;
   }
   
   public Promise<JavaScriptObject> getCapabilities()
   {
      return new Promise<>((ResolveCallbackFn<JavaScriptObject> resolve, RejectCallbackFn reject) -> {
         server_.pandocGetCapabilities(
            new PromiseServerRequestCallback<>(resolve, reject)
         );
      });
   }

   public Promise<JavaScriptObject> markdownToAst(String markdown, String format, JsArrayString options)
   {
      // rsession pandoc back-end doesn't handle empty stdiput well (SyncProcess.run doesn't
      // ever write stdin if it's empty)
      final String input = !StringUtil.isNullOrEmpty(markdown) ? markdown : " ";
      
      return new Promise<>((ResolveCallbackFn<JavaScriptObject> resolve, RejectCallbackFn reject) -> {
         
         server_.pandocMarkdownToAst(
            input, format, options, 
            new PromiseServerRequestCallback<>(resolve, reject)
         );
      });
   }
   
   public Promise<String> astToMarkdown(JavaScriptObject ast, String format, JsArrayString options)
   {
      return new Promise<>((ResolveCallbackFn<String> resolve, RejectCallbackFn reject) -> {
         server_.pandocAstToMarkdown(
            ast, format, options, 
            new PromiseServerRequestCallback<>(resolve, reject)
         );
      });
   }
   
   public Promise<JavaScriptObject> getBibliography(String file, JsArrayString bibliographies, String refBlock, String etag)
   {
      return new Promise<>((ResolveCallbackFn<JavaScriptObject> resolve, RejectCallbackFn reject) -> {       
          server_.pandocGetBibliography(
            file,
            bibliographies,
            refBlock,
            etag,
            new PromiseServerRequestCallback<>(resolve, reject, constants_.readingBibliographyProgressText(), 1500)
         );
      });
   }
   
   public Promise<Boolean> addToBibliography(String bibliography, boolean project, String id, String sourceAsJson, String sourceAsBibTeX)
   {
      return new Promise<>((ResolveCallbackFn<Boolean> resolve, RejectCallbackFn reject) -> {       
         server_.pandocAddToBibliography(
           bibliography,
           project,
           id,
           sourceAsJson,
           sourceAsBibTeX,
           new PromiseServerRequestCallback<>(resolve, reject, constants_.savingBibliographyProgressText(), 1500)
        );
     }); 
   }
   
   public Promise<String> citationHTML(String file, String sourceAsJson, String csl)
   {
      return new Promise<>((ResolveCallbackFn<String> resolve, RejectCallbackFn reject) -> {
         server_.pandocCitationHTML(
            file, sourceAsJson, csl,
            new PromiseServerRequestCallback<>(resolve, reject)
         );
      });
   }


   public Promise<String> listExtensions(String format)
   {
      return new Promise<>((ResolveCallbackFn<String> resolve, RejectCallbackFn reject) -> {
         server_.pandocListExtensions(format, new PromiseServerRequestCallback<>(resolve, reject));
      });
   }

   private PanmirrorPandocServerOperations server_;
   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);
}
