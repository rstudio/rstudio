/*
 * RSConnectDeployInitiatedEvent.java
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
package org.rstudio.studio.client.rsconnect.events;

import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSettings;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RSConnectDeployInitiatedEvent extends GwtEvent<RSConnectDeployInitiatedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onRSConnectDeployInitiated(RSConnectDeployInitiatedEvent event);
   }

   public static final GwtEvent.Type<RSConnectDeployInitiatedEvent.Handler> TYPE =
      new GwtEvent.Type<RSConnectDeployInitiatedEvent.Handler>();
   
   public RSConnectDeployInitiatedEvent(String path, 
                                        RSConnectPublishSettings settings,
                                        String sourceFile,
                                        boolean launchBrowser, 
                                        RSConnectDeploymentRecord record)
   {
      path_ = path;
      sourceFile_ = sourceFile;
      launchBrowser_ = launchBrowser;
      record_ = record;
      settings_ = settings;
   }
   
   public RSConnectDeploymentRecord getRecord()
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
   
   public RSConnectPublishSettings getSettings()
   {
      return settings_;
   }

   @Override
   protected void dispatch(RSConnectDeployInitiatedEvent.Handler handler)
   {
      handler.onRSConnectDeployInitiated(this);
   }

   @Override
   public GwtEvent.Type<RSConnectDeployInitiatedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final RSConnectDeploymentRecord record_;
   private final String path_;
   private final String sourceFile_;
   private final boolean launchBrowser_;
   private final RSConnectPublishSettings settings_;
}
