/*
 * PanmirrorDOIServer.java
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


package org.rstudio.studio.client.panmirror.server;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.promise.PromiseServerRequestCallback;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;

import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;
import jsinterop.annotations.JsType;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;

@JsType
public class PanmirrorDOIServer
{
   public PanmirrorDOIServer() {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   void initialize(PanmirrorDOIServerOperations server)
   {
      server_ = server;
   }


   public Promise<JavaScriptObject> fetchCSL(String doi, int delayMs)
   {
      return new Promise<>((ResolveCallbackFn<JavaScriptObject> resolve, RejectCallbackFn reject) -> {
         server_.doiFetchCSL(
            doi,
            new PromiseServerRequestCallback<>(resolve, reject, constants_.lookingUpDOIProgress(), delayMs)
         );
      });
   }
   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);


   PanmirrorDOIServerOperations server_;
}
