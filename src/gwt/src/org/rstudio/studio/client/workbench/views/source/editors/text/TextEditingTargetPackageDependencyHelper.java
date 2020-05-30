/*
 * TextEditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.events.AvailablePackagesReadyEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
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
      
      toggleHandlers(prefs_.autoDiscoverPackageDependencies().getGlobalValue());
      prefs_.autoDiscoverPackageDependencies().addValueChangeHandler((ValueChangeEvent<Boolean> event) -> {
         toggleHandlers(event.getValue());
      });
   }
   
   private void toggleHandlers(boolean enabled)
   {
      // always refresh handlers, just to be safe
      if (handlers_ != null)
      {
         for (HandlerRegistration handler : handlers_)
            handler.removeHandler();
         handlers_ = null;
      }
      
      handlers_ = new HandlerRegistration[] {
            
            docDisplay_.addSaveCompletedHandler((SaveFileEvent event) -> {
               if (!event.isAutosave())
                  discoverPackageDependencies();
            }),
            
            events_.addHandler(AvailablePackagesReadyEvent.TYPE, (AvailablePackagesReadyEvent event) -> {
               discoverPackageDependencies();
            }),
            
            docDisplay_.addAttachHandler((AttachEvent event) -> {
               if (!event.isAttached())
               {
                  for (HandlerRegistration handler : handlers_)
                     handler.removeHandler();
                  handlers_ = null;
               }
            })
            
      };
   }
   
   @Inject
   private void initialize(EventBus events,
                           UserPrefs prefs,
                           SourceServerOperations server)
   {
      events_ = events;
      prefs_  = prefs;
      server_ = server;
   }
   
   public void discoverPackageDependencies()
   {
      if (discoveringDependencies_)
         return;
      
      if (sentinel_ == null)
         return;
      
      if (!prefs_.autoDiscoverPackageDependencies().getGlobalValue())
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
   private HandlerRegistration[] handlers_;
   
   private final TextEditingTarget target_;
   private final DocUpdateSentinel sentinel_;
   private final DocDisplay docDisplay_;
   
   // Injected ----
   EventBus events_;
   UserPrefs prefs_;
   SourceServerOperations server_;
   
}
