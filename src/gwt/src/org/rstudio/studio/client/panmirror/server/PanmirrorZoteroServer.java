/*
 * PanmirrorZoteroServer.java
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

package org.rstudio.studio.client.panmirror.server;

import org.rstudio.core.client.promise.PromiseServerRequestCallback;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;

import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorZoteroServer
{
   public PanmirrorZoteroServer()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   void initialize(PanmirrorZoteroServerOperations server)
   {
      server_ = server;
   }

   public Promise<Boolean> validateWebAPIKey(String key)
   {
      return new Promise<Boolean>((ResolveCallbackFn<Boolean> resolve, RejectCallbackFn reject) -> {
         server_.zoteroValidateWebAPIKey(key,
               new PromiseServerRequestCallback<Boolean>(resolve, reject));
      });
   }

   public Promise<JavaScriptObject> getCollections(String file, JsArrayString collections,
                                                   JsArray<PanmirrorZoteroCollectionSpec> cached,
                                                   boolean useCache)
   {
      return new Promise<JavaScriptObject>(
            (ResolveCallbackFn<JavaScriptObject> resolve, RejectCallbackFn reject) -> {
               server_.zoteroGetCollections(file, collections, cached, useCache,
                     new PromiseServerRequestCallback<JavaScriptObject>(resolve, reject, "Loading Collections...", 2000));
            });
   }

   public Promise<JavaScriptObject> getLibraryNames()
   {
      return new Promise<JavaScriptObject>(
            (ResolveCallbackFn<JavaScriptObject> resolve, RejectCallbackFn reject) -> {
               server_.zoteroGetLibraryNames(
                     new PromiseServerRequestCallback<JavaScriptObject>(resolve, reject));
            });
   }
   
   public Promise<JavaScriptObject> getActiveCollectionSpecs(String file, JsArrayString collections)
   {
      return new Promise<JavaScriptObject>(
            (ResolveCallbackFn<JavaScriptObject> resolve, RejectCallbackFn reject) -> {
               server_.zoteroGetActiveCollectionSpecs(file, collections,
                  new PromiseServerRequestCallback<JavaScriptObject>(resolve, reject, "Reading Collections...", 2000));
            });
   }
   
   
   
   public Promise<JavaScriptObject> betterBibtexExport(JsArrayString itemKeys, 
                                                       String translatorId, 
                                                       int libraryID)
   {
      return new Promise<JavaScriptObject>((ResolveCallbackFn<JavaScriptObject> resolve, RejectCallbackFn reject) -> {
         server_.zoteroBetterBibtexExport(
            itemKeys, 
            translatorId, 
            libraryID, 
            new PromiseServerRequestCallback<JavaScriptObject>(resolve, reject)
         );
      });
   }

   PanmirrorZoteroServerOperations server_;
}
