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
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.events.AvailablePackagesReadyEvent;
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
      
      // check package dependencies on user-requested saves
      docDisplay_.addSaveCompletedHandler((SaveFileEvent event) -> {
         if (!event.isAutosave())
            discoverPackageDependencies();
      });
      
      // check package dependencies whenever the set of available packages
      // is ready if we haven't yet checked before (this allows for us
      // to check dependencies in open files at the start of a session)
      events_.addHandler(AvailablePackagesReadyEvent.TYPE, (AvailablePackagesReadyEvent event) -> {
         discoverPackageDependencies();
      });
   }
   
   @Inject
   private void initialize(EventBus events,
                           SourceServerOperations server)
   {
      events_ = events;
      server_ = server;
   }
   
   public void discoverPackageDependencies()
   {
      if (discoveringDependencies_)
         return;
      
      if (sentinel_ == null)
         return;
      
      boolean canDiscoverDependencies =
            docDisplay_.getFileType().isR() ||
            docDisplay_.getFileType().isRmd() ||
            docDisplay_.getFileType().isRnw();
      
      if (!canDiscoverDependencies)
         return;
      
      discoveringDependencies_ = true;
      server_.discoverPackageDependencies(
            sentinel_.getId(),
            docDisplay_.getFileType().getDefaultExtension(),
            new ServerRequestCallback<AvailablePackagesReadyEvent.Data>()
            {
               @Override
               public void onResponseReceived(AvailablePackagesReadyEvent.Data response)
               {
                  discoveringDependencies_ = false;
                  
                  if (!response.isReady())
                     return;
                     
                  JsArrayString packages = response.getPackages();
                  if (packages.length() > 0)
                     target_.showRequiredPackagesMissingWarning(JsUtil.toList(packages));
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
   EventBus events_;
   SourceServerOperations server_;
   
}
