/*
 * RSConnectActionEvent.java
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
package org.rstudio.studio.client.rsconnect.events;

import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RenderedDocPreview;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RSConnectActionEvent extends GwtEvent<RSConnectActionEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onRSConnectAction(RSConnectActionEvent event);
   }

   public static final GwtEvent.Type<RSConnectActionEvent.Handler> TYPE =
      new GwtEvent.Type<RSConnectActionEvent.Handler>();
   
   public static RSConnectActionEvent ConfigureAppEvent(String path)
   {
      return new RSConnectActionEvent(ACTION_TYPE_CONFIGURE, 
            RSConnect.CONTENT_TYPE_APP, path, null, null, null, null);
   }

   public static RSConnectActionEvent DeployAppEvent(String path, 
         int contentType, RSConnectDeploymentRecord fromPrevious)
   {
      return new RSConnectActionEvent(ACTION_TYPE_DEPLOY, 
            contentType, path, null, null, null, fromPrevious);
   }
   
   public static RSConnectActionEvent DeployDocEvent(RenderedDocPreview params,
         int type, RSConnectDeploymentRecord fromPrevious)
   {
      return new RSConnectActionEvent(ACTION_TYPE_DEPLOY,
            type,
            params.getSourceFile(),
            params.getOutputFile(), 
            null,
            params,
            fromPrevious);
   }
   
   public static RSConnectActionEvent DeployPlotEvent(String htmlFile, 
         RSConnectDeploymentRecord fromPrevious)
   {
      return new RSConnectActionEvent(ACTION_TYPE_DEPLOY, 
            RSConnect.CONTENT_TYPE_PLOT, null, htmlFile, "Current Plot",
            null, fromPrevious);
   }

   public static RSConnectActionEvent DeployHtmlEvent(int contentType,
         String sourceFile, String htmlFile, String selfContainedDesc, 
         RSConnectDeploymentRecord fromPrevious)
   {
      return new RSConnectActionEvent(ACTION_TYPE_DEPLOY, 
            contentType, sourceFile, htmlFile, 
            selfContainedDesc, null, fromPrevious);
   }

   public static RSConnectActionEvent DeployAPIEvent(String path, 
         int contentType, RSConnectDeploymentRecord fromPrevious)
   {
      return new RSConnectActionEvent(ACTION_TYPE_DEPLOY, 
            contentType, path, null, null, null, fromPrevious);
   }
 
   private RSConnectActionEvent(int action, int contentType, String path, 
                               String htmlFile, String selfContainedDesc, 
                               RenderedDocPreview fromPreview,
                               RSConnectDeploymentRecord fromPrevious)
   {
      action_ = action;
      contentType_ = contentType;
      path_ = path;
      fromPrevious_ = fromPrevious;
      docPreview_ = fromPreview;
      description_ = selfContainedDesc;
      
      // determine location of static content, if any
      if (htmlFile != null)
         htmlFile_ = htmlFile;
      else if (fromPreview != null)
         htmlFile_ = fromPreview.getOutputFile();
      else
         htmlFile_ = null;
   }
   
   public String getPath()
   {
      return path_;
   }
   
   public int getAction()
   {
      return action_;
   }
   
   public RenderedDocPreview getFromPreview()
   {
      return docPreview_;
   }
   
   public RSConnectDeploymentRecord getFromPrevious()
   {
      return fromPrevious_;
   }
   
   public int getContentType()
   {
      return contentType_;
   }
   
   public String getHtmlFile()
   {
      return htmlFile_;
   }
   
   public String getDescription()
   {
      return description_;
   }
   
   @Override
   protected void dispatch(RSConnectActionEvent.Handler handler)
   {
      handler.onRSConnectAction(this);
   }

   @Override
   public GwtEvent.Type<RSConnectActionEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   public static int ACTION_TYPE_DEPLOY = 0;
   public static int ACTION_TYPE_CONFIGURE = 1;
   
   private final String path_;
   private final RenderedDocPreview docPreview_;
   private final int action_;
   private final RSConnectDeploymentRecord fromPrevious_;
   private final int contentType_;
   private final String htmlFile_;
   private final String description_;
}
