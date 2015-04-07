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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.VisibleChangedHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.rpubs.events.RPubsUploadStatusEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.model.RenderedDocPreview;
import org.rstudio.studio.client.rsconnect.model.StaticHtmlGenerator;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.events.RSConnectDeploymentCompletedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

public class RSConnectPublishButton extends Composite
   implements RSConnectDeploymentCompletedEvent.Handler,
              RPubsUploadStatusEvent.Handler
{
   public RSConnectPublishButton(int contentType, boolean showCaption,
         boolean manageVisiblity)
   {
      // TODO: are we responsible for hiding ourselves if e.g. internal and 
      // external publishing is disabled?
      
      contentType_ = contentType;
      showCaption_ = showCaption;
      manageVisiblity_ = manageVisiblity;
      
      // create root widget
      HorizontalPanel panel = new HorizontalPanel();
      
      // create publish button itself
      publishButton_ = new ToolbarButton(
            RStudioGinjector.INSTANCE.getCommands()
                            .rsconnectDeploy().getImageResource(), 
            new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent arg0)
               {
                  onPublishClick(defaultRec_);
               }
            });
      
      if (showCaption_)
         publishButton_.setText("Publish");
      panel.add(publishButton_);
      
      // create drop menu of previous deployments/other commands
      publishMenu_ = new ToolbarPopupMenu();
      ToolbarButton publishMenuButton = new ToolbarButton(publishMenu_, false);
      panel.add(publishMenuButton);
      
      // initialize composite widget
      initWidget(panel);

      // initialize injected members
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(RSConnectServerOperations server,
         EventBus events, 
         Commands commands,
         GlobalDisplay display)
   {
      server_ = server;
      events_ = events;
      commands_ = commands;
      display_ = display;
      
      // initialize visibility if requested
      if (manageVisiblity_) 
      {
         setVisible(commands_.rsconnectDeploy().isVisible());
         commands_.rsconnectDeploy().addVisibleChangedHandler(
               new VisibleChangedHandler()
         {
            @Override
            public void onVisibleChanged(AppCommand command)
            {
               setVisible(commands_.rsconnectDeploy().isVisible());
            }
         });
      }
      
      events_.addHandler(RSConnectDeploymentCompletedEvent.TYPE, this);
      events_.addHandler(RPubsUploadStatusEvent.TYPE, this);
   }
   
   @Override
   public void setVisible(boolean visible)
   {
      super.setVisible(visible);
      
      // if becoming visible, repopulate the list of deployments if we haven't
      // already
      if (visible)
         populateDeployments();
   }
   
   public void setContentPath(String contentPath)
   {
      contentPath_ = contentPath;
      if (isVisible())
         populateDeployments();
   }
   
   public void setRmdPreview(RmdPreviewParams params)
   {
      // TODO: we should be visible for all Rmd files
      if (params.isShinyDocument() || 
            (params.getResult().isHtml() &&
             params.getResult().getFormat() != null))
      {
         setVisible(true);
         docPreview_ = new RenderedDocPreview(params);
         setContentPath(params.getResult().getTargetFile());
      }
      else
      {
         setVisible(false);
         docPreview_ = null;
      }
   }
   
   public void setHtmlPreview(HTMLPreviewResult params)
   {
      if (params.getSucceeded())
      {
         setVisible(true);
         setContentPath(params.getSourceFile());
         docPreview_ = new RenderedDocPreview(params);
      }
   }

   public void setText(String text)
   {
      if (showCaption_)
         publishButton_.setText(text);
   }
   
   public void setContentType(int contentType)
   {
      contentType_ = contentType;
   }
   
   public void setHtmlGenerator(StaticHtmlGenerator generator)
   {
      htmlGenerator_ = generator;
      setPreviousDeployments(null);
   }


   @Override
   public void onRSConnectDeploymentCompleted(
         RSConnectDeploymentCompletedEvent event)
   {
      if (!event.succeeded())
         return;
      
      // when a deployment is successful, refresh ourselves. Consider: it's 
      // a little wasteful to do this whether or not the deployment was for 
      // the content on which this button is hosted, but there are unlikely to
      // be more than a couple publish buttons at any one time, and this is
      // cheap (just hits the local disk)
      lastContentPath_ = null;
      populateDeployments();
   }

   @Override
   public void onRPubsPublishStatus(RPubsUploadStatusEvent event)
   {
      // make sure it applies to our context
      RPubsUploadStatusEvent.Status status = event.getStatus();
      
      if (StringUtil.isNullOrEmpty(status.getError()))
      {
         lastContentPath_ = null;
         populateDeployments();
      }
   }

   // Private methods --------------------------------------------------------
   
   private void populateDeployments()
   {
      // prevent reentrancy
      if (contentPath_ == null || populating_)
         return;
      
      // avoid populating if we've already set the deployments for this path
      if (lastContentPath_ != null && lastContentPath_.equals(contentPath_))
         return;
      
      // if this is a .R file, check for deployments of its parent path
      String contentPath = contentPath_;
      FileSystemItem fsiContent = FileSystemItem.createFile(contentPath_);
      if (fsiContent.getExtension().toLowerCase().equals(".r")) {
         contentPath = fsiContent.getParentPathString();
      }
      
      populating_ = true;
      server_.getRSConnectDeployments(contentPath, 
            new ServerRequestCallback<JsArray<RSConnectDeploymentRecord>>()
      {
         @Override
         public void onResponseReceived(JsArray<RSConnectDeploymentRecord> recs)
         {
            populating_ = false;
            lastContentPath_ = contentPath_;
            setPreviousDeployments(recs);
         }
         
         @Override
         public void onError(ServerError error)
         {
            // mark population finished, but allow a retry 
            populating_ = false;
         }
      });
   }
   
   private void onPublishClick(RSConnectDeploymentRecord previous)
   {
      switch (contentType_)
      {
      case RSConnect.CONTENT_TYPE_HTML:
      case RSConnect.CONTENT_TYPE_PLOT:
         // for plots, we need to generate the hosting HTML prior to publishing
         if (htmlGenerator_ != null)
         {
            htmlGenerator_.generateStaticHtml("Plot", "", 
                  new CommandWithArg<String>()
                  {
                     @Override
                     public void execute(String htmlFile)
                     {
                        events_.fireEvent(
                              RSConnectActionEvent.DeployPlotEvent(htmlFile));
                     }
                  });
         }
         break;
      case RSConnect.CONTENT_TYPE_APP:
         // Shiny application
         events_.fireEvent(RSConnectActionEvent.DeployAppEvent(
               contentPath_, previous));
         break;
      case RSConnect.CONTENT_TYPE_DOCUMENT:
         // All R Markdown variants (single/multiple and static/Shiny)
         events_.fireEvent(RSConnectActionEvent.DeployDocEvent(
               docPreview_, previous));
         break;
      default: 
         // should never happen 
         display_.showErrorMessage("Can't Publish " + 
            RSConnect.contentTypeDesc(contentType_), 
            "The content type '" + 
            RSConnect.contentTypeDesc(contentType_) + 
            "' is not currently supported for publishing.");
      }
   }
   
   private void setPreviousDeployments(JsArray<RSConnectDeploymentRecord> recs)
   {
      // clear existing deployment menu, if any
      publishMenu_.clearItems();

      // if there are existing deployments, make the UI reflect that this is a
      // republish
      if (recs != null && recs.length() > 0)
      {
         if (showCaption_)
            publishButton_.setText("Republish");

         // find the default (last deployed record)--this needs to be done as
         // a first pass so we can identify the associated menu item in one
         // pass 
         for (int i  = 0; i < recs.length(); i++)
         {
            final RSConnectDeploymentRecord rec = recs.get(i);
            if (defaultRec_ == null || defaultRec_.getWhen() < rec.getWhen())
            {
               defaultRec_ = rec;
            }
         }

         // build the deployment menu
         for (int i  = 0; i < recs.length(); i++)
         {
            final RSConnectDeploymentRecord rec = recs.get(i);
            final DeploymentMenuItem menuItem = new DeploymentMenuItem(rec, 
                  rec == defaultRec_, new Command()
               {
                  @Override
                  public void execute()
                  {
                     onPublishClick(rec);
                  }
               });
            publishMenu_.addItem(menuItem);
         }
         
         publishMenu_.addSeparator();
         publishMenu_.addItem(new MenuItem(
               AppCommand.formatMenuLabel(
                     commands_.rsconnectDeploy().getImageResource(), 
                     "Other Destination...", null),
               true,
               new Scheduler.ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     onPublishClick(null);
                  }
               }));
      }
      else
      {
         // no existing deployments to redeploy to, so just offer to make a new
         // one
         publishMenu_.addItem(new MenuItem(
               AppCommand.formatMenuLabel(
                     commands_.rsconnectDeploy().getImageResource(), 
                     "Publish " + RSConnect.contentTypeDesc(contentType_) + 
                     "...", null),
               true,
               new Scheduler.ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     onPublishClick(defaultRec_);
                  }
               }));
      }

      publishMenu_.addSeparator();
      publishMenu_.addItem(
            commands_.rsconnectManageAccounts().createMenuItem(false));
   }
   
   private final ToolbarButton publishButton_;
   private final ToolbarPopupMenu publishMenu_;

   private RSConnectServerOperations server_;
   private EventBus events_;
   private Commands commands_;
   private GlobalDisplay display_;

   private String contentPath_;
   private int contentType_;
   private String lastContentPath_;
   private boolean populating_ = false;
   private RenderedDocPreview docPreview_;
   private StaticHtmlGenerator htmlGenerator_;

   private final boolean showCaption_;
   private final boolean manageVisiblity_;

   private RSConnectDeploymentRecord defaultRec_;
}
