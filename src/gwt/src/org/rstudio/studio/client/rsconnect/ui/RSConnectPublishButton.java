/*
 * RSConnectPublishButton.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuItem;

public class RSConnectPublishButton extends Composite
{
   public RSConnectPublishButton(
         RSConnectServerOperations server, 
         String contentPath)
   {
      HorizontalPanel panel = new HorizontalPanel();
      server_ = server;
      events_ = RStudioGinjector.INSTANCE.getEventBus();
      contentPath_ = contentPath;
      
      // create publish button itself
      publishButton_ = new ToolbarButton(
            RStudioGinjector.INSTANCE.getCommands()
                            .rsconnectDeploy().getImageResource(), 
            new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent arg0)
               {
                  onPublishClick(null);
                  
               }
            });
      
      publishButton_.setText("Publish");
      
      // hide the button until we know how it should be titled
      publishButton_.setVisible(false);
      panel.add(publishButton_);
      
      // create drop menu of previous deployments (we'll populate this if we
      // find any)
      publishMenu_ = new ToolbarPopupMenu();
      publishMenu_.setVisible(false);
      panel.add(publishMenu_);
      
      server_.getRSConnectDeployments(contentPath, 
            new ServerRequestCallback<JsArray<RSConnectDeploymentRecord>>()
      {
         @Override
         public void onResponseReceived(JsArray<RSConnectDeploymentRecord> recs)
         {
            setPreviousDeployments(recs);
         }
         
         @Override
         public void onError(ServerError error)
         {
            // we failed to find existing deployments; show the publish button 
            publishButton_.setVisible(true);
         }
      });

      initWidget(panel);
   }
   
   // Private methods --------------------------------------------------------
   
   private void onPublishClick(RSConnectDeploymentRecord previous)
   {
      events_.fireEvent(new RSConnectActionEvent(
            RSConnectActionEvent.ACTION_TYPE_DEPLOY, 
            contentPath_,
            previous));
   }
   
   private void setPreviousDeployments(JsArray<RSConnectDeploymentRecord> recs)
   {
      // if there are existing deployments, make the UI reflect that this is a
      // republish
      if (recs.length() > 0)
      {
         publishButton_.setText("Republish");
         for (int i  = 0; i < recs.length(); i++)
         {
            final RSConnectDeploymentRecord rec = recs.get(i);
            publishMenu_.addItem(new MenuItem(rec.getServer(), 
                  new Scheduler.ScheduledCommand()
            {
               
               @Override
               public void execute()
               {
                  onPublishClick(rec);
               }
            }));
         }
         publishMenu_.addSeparator();
         publishMenu_.addItem(new MenuItem("Other Destination", 
               new Scheduler.ScheduledCommand()
         {
            
            @Override
            public void execute()
            {
               onPublishClick(null);
            }
         }));
         publishMenu_.setVisible(true);
      }
      publishButton_.setVisible(true);
   }
 
   private final ToolbarButton publishButton_;
   private final ToolbarPopupMenu publishMenu_;

   private final RSConnectServerOperations server_;
   private final EventBus events_;

   private final String contentPath_;
}
