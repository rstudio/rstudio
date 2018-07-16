/*
 * TextEditingTarget.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;

public class TextEditingTargetPackageDependencyHelper
{
   public TextEditingTargetPackageDependencyHelper(TextEditingTarget target,
                                                   DocUpdateSentinel sentinel,
                                                   DocDisplay docDisplay)
   {
      target_ = target;
      sentinel_ = sentinel;
      docDisplay_ = docDisplay;
      
      init();
   }
   
   private void init()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      docDisplay_.addSaveCompletedHandler((SaveFileEvent event) -> {
         discoverPackageDependencies();
      });
   }
   
   @Inject
   private void initialize(SourceServerOperations server)
   {
      server_ = server;
   }
   
   public void discoverPackageDependencies()
   {
      if (discoveringDependencies_)
         return;
      
      if (sentinel_ == null)
         return;
      
      discoveringDependencies_ = true;
      server_.discoverPackageDependencies(
            sentinel_.getId(),
            docDisplay_.getFileType().getDefaultExtension(),
            new ServerRequestCallback<JsArrayString>()
            {
               @Override
               public void onResponseReceived(JsArrayString response)
               {
                  discoveringDependencies_ = false;
                  if (response.length() > 0)
                     target_.showRequiredPackagesMissingWarning(JsUtil.toList(response));
               }
               
               @Override
               public void onError(ServerError error)
               {
                  discoveringDependencies_ = false;
                  Debug.logError(error);
               }
            });
   }
   
   boolean discoveringDependencies_ = false;
   
   private final TextEditingTarget target_;
   private final DocUpdateSentinel sentinel_;
   private final DocDisplay docDisplay_;
   
   // Injected ----
   SourceServerOperations server_;
   
   
}
