/*
 * PanmirrorCrossrefServerOperations.java
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


import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import elemental2.core.JsObject;



public interface PanmirrorZoteroServerOperations 
{
   void zoteroDetectLocalConfig(ServerRequestCallback<JsObject> callback);
   
   void zoteroValidateWebAPIKey(String key, ServerRequestCallback<Boolean> callback);
   
   void zoteroGetCollections(String file,
                             JsArrayString collections,
                             JsArray<PanmirrorZoteroCollectionSpec> cached, 
                             boolean useCache,
                             ServerRequestCallback<JavaScriptObject> callback);
   
   void zoteroGetLibraryNames(ServerRequestCallback<JavaScriptObject> callback);
   
   void zoteroGetActiveCollectionSpecs(String file, 
                                       JsArrayString collections,
                                       ServerRequestCallback<JavaScriptObject> callback);

   void zoteroBetterBibtexExport(JsArrayString itemKeys, 
                                 String translatorId, 
                                 int libraryID,
                                 ServerRequestCallback<JavaScriptObject> callback);
   

}
