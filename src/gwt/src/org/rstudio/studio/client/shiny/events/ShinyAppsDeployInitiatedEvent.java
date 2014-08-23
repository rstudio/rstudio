/*
 * ShinyAppsDeployInitiatedEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.shiny.events;

import org.rstudio.studio.client.shiny.model.ShinyAppsDeploymentRecord;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ShinyAppsDeployInitiatedEvent extends GwtEvent<ShinyAppsDeployInitiatedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onShinyAppsDeployInitiated(ShinyAppsDeployInitiatedEvent event);
   }

   public static final GwtEvent.Type<ShinyAppsDeployInitiatedEvent.Handler> TYPE =
      new GwtEvent.Type<ShinyAppsDeployInitiatedEvent.Handler>();
   
   public ShinyAppsDeployInitiatedEvent(String path, 
                                        String sourceFile,
                                        boolean launchBrowser, 
                                        ShinyAppsDeploymentRecord record)
   {
      path_ = path;
      record_ = record;
      sourceFile_ = sourceFile;
      launchBrowser_ = launchBrowser;
   }
   
   public ShinyAppsDeploymentRecord getRecord()
   {
      return record_;
   }
   
   public String getPath()
   {
      return path_; 
   }
   
   public String getSourceFile()
   {
      return sourceFile_;
   }
   
   public boolean getLaunchBrowser()
   {
      return launchBrowser_; 
   }

   @Override
   protected void dispatch(ShinyAppsDeployInitiatedEvent.Handler handler)
   {
      handler.onShinyAppsDeployInitiated(this);
   }

   @Override
   public GwtEvent.Type<ShinyAppsDeployInitiatedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private ShinyAppsDeploymentRecord record_;
   private String path_;
   private String sourceFile_;
   private boolean launchBrowser_;
}
