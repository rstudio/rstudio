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
import org.rstudio.core.client.command.EnabledChangedHandler;
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
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.model.RenderedDocPreview;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.events.RSConnectDeploymentCompletedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class RSConnectPublishButton extends Composite
   implements RSConnectDeploymentCompletedEvent.Handler,
              RPubsUploadStatusEvent.Handler
{
   public RSConnectPublishButton(int contentType, boolean showCaption,
         AppCommand boundCommand)
   {
      contentType_ = contentType;
      showCaption_ = showCaption;
      boundCommand_ = boundCommand;
      
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
      
      // compute initial visible state
      applyVisiblity();
   }
   
   @Inject
   public void initialize(RSConnectServerOperations server,
         EventBus events, 
         Commands commands,
         GlobalDisplay display,
         Provider<UIPrefs> pUiPrefs,
         Session session)
   {
      server_ = server;
      events_ = events;
      commands_ = commands;
      display_ = display;
      session_ = session;
      pUiPrefs_ = pUiPrefs;
      
      // initialize visibility if requested
      if (boundCommand_ != null) 
      {
         boundCommand_.addVisibleChangedHandler(
               new VisibleChangedHandler()
         {
            @Override
            public void onVisibleChanged(AppCommand command)
            {
               applyVisiblity();
            }
         });

         boundCommand_.addEnabledChangedHandler(
               new EnabledChangedHandler()
         {
            @Override
            public void onEnabledChanged(AppCommand command)
            {
               applyVisiblity();
            }
         });
      }
      
      events_.addHandler(RSConnectDeploymentCompletedEvent.TYPE, this);
      events_.addHandler(RPubsUploadStatusEvent.TYPE, this);
   }
   
   @Override
   public void setVisible(boolean visible)
   {
      boolean wasVisible = isVisible();
      super.setVisible(visible);
      
      // if becoming visible, repopulate the list of deployments if we haven't
      // already
      if (!wasVisible && visible)
         populateDeployments(false);
   }
   
   public void setContentPath(String contentPath, String outputPath)
   {
      contentPath_ = contentPath;
      outputPath_ = outputPath;
      if (isVisible())
         populateDeployments(false);
   }
   
   public void setRmdPreview(RmdPreviewParams params)
   {
      // TODO: we should be visible for all Rmd files
      if (params.isShinyDocument() || 
            (params.getResult().isHtml() &&
             params.getResult().getFormat() != null))
      {
         docPreview_ = new RenderedDocPreview(params);
         setContentPath(params.getResult().getTargetFile(),
               params.getOutputFile());
      }
      else
      {
         docPreview_ = null;
      }
      applyVisiblity();
   }
   
   public void setHtmlPreview(HTMLPreviewResult params)
   {
      if (params.getSucceeded())
      {
         setContentPath(params.getSourceFile(), params.getHtmlFile());
         docPreview_ = new RenderedDocPreview(params);
         applyVisiblity();
      }
   }

   public void setText(String text)
   {
      if (showCaption_)
         publishButton_.setText(text);
   }
   
   public void setContentType(int contentType)
   {
      // this can happen in the viewer pane, which hosts e.g. both HTML widgets
      // and R Markdown documents, each of which has its own publishing 
      // semantics
      int oldType = contentType_;
      contentType_ = contentType;
      if (oldType != contentType)
      {
         // moving to a document type: get its deployment status 
         if (contentType == RSConnect.CONTENT_TYPE_DOCUMENT)
            populateDeployments(true);
         
         // moving to a raw HTML type: erase the deployment list
         if (contentType == RSConnect.CONTENT_TYPE_HTML)
            setPreviousDeployments(null);
      }
      applyVisiblity();
   }
   
   public void setPublishHtmlSource(PublishHtmlSource source)
   {
      publishHtmlSource_ = source;
      setPreviousDeployments(null);
      applyVisiblity();
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
      populateDeployments(true);
   }

   @Override
   public void onRPubsPublishStatus(RPubsUploadStatusEvent event)
   {
      // make sure it applies to our context
      RPubsUploadStatusEvent.Status status = event.getStatus();
      
      if (StringUtil.isNullOrEmpty(status.getError()))
      {
         populateDeployments(true);
      }
   }
   

   // Private methods --------------------------------------------------------
   
   private void populateDeployments(boolean force)
   {
      // prevent reentrancy
      if (contentPath_ == null || populating_)
         return;
      
      // avoid populating if we've already set the deployments for this path
      // (unless we're forcefully repopulating)
      if (lastContentPath_ != null && lastContentPath_.equals(contentPath_) &&
            !force)
         return;
      
      // if this is a .R file, check for deployments of its parent path
      String contentPath = contentPath_;
      FileSystemItem fsiContent = FileSystemItem.createFile(contentPath_);
      if (fsiContent.getExtension().toLowerCase().equals(".r")) {
         contentPath = fsiContent.getParentPathString();
      }
      
      populating_ = true;
      server_.getRSConnectDeployments(contentPath, 
            outputPath_,
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
         if (publishHtmlSource_ == null) 
         {
            display_.showErrorMessage("Content Publish Failed",
                  "No HTML could be generated for the content.");
            return;
         }
         publishHtmlSource_.generatePublishHtml(
               new CommandWithArg<String>() 
               {
                  @Override
                  public void execute(String arg)
                  {
                     events_.fireEvent(RSConnectActionEvent.DeployHtmlEvent(
                           arg, publishHtmlSource_.getTitle()));
                  }
               });
      case RSConnect.CONTENT_TYPE_PLOT:
         // for plots, we need to generate the hosting HTML prior to publishing
         if (publishHtmlSource_ != null)
         {
            publishHtmlSource_.generatePublishHtml(
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
         // show first-time publish button caption
         if (showCaption_)
            publishButton_.setText("Publish");

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
   
   private void applyVisiblity()
   {
      setVisible(recomputeVisibility());
   }
   
   private boolean recomputeVisibility()
   {
      // if all publishing is disabled, hide ourselves 
      if (!session_.getSessionInfo().getAllowPublish() ||
          !pUiPrefs_.get().showPublishUi().getGlobalValue())
         return false;
      
      // if both internal and external publishing is disabled, hide ourselves
      if (!session_.getSessionInfo().getAllowExternalPublish() &&
          !pUiPrefs_.get().enableRStudioConnect().getGlobalValue())
         return false;
      
      // if we're bound to a command's visibility/enabled state, check that
      if (boundCommand_ != null && (!boundCommand_.isVisible() || 
            !boundCommand_.isEnabled()))
         return false;

      // if we have no content type, hide ourselves
      if (contentType_ == RSConnect.CONTENT_TYPE_NONE)
         return false;
      
      // if we do have a content type, ensure that we have actual content 
      // bound to it
      if ((contentType_ == RSConnect.CONTENT_TYPE_HTML || 
            contentType_ == RSConnect.CONTENT_TYPE_PLOT) &&
           publishHtmlSource_ == null)
         return false;
      
      if (contentType_ == RSConnect.CONTENT_TYPE_APP && 
          StringUtil.isNullOrEmpty(contentPath_))
         return false;

      if (contentType_ == RSConnect.CONTENT_TYPE_DOCUMENT && 
          docPreview_ == null)
         return false;

      // looks like we should be visible
      return true;
   }
   
   private final ToolbarButton publishButton_;
   private final ToolbarPopupMenu publishMenu_;

   private RSConnectServerOperations server_;
   private EventBus events_;
   private Commands commands_;
   private GlobalDisplay display_;
   private Session session_;
   private Provider<UIPrefs> pUiPrefs_;

   private String contentPath_;
   private String outputPath_;
   private int contentType_ = RSConnect.CONTENT_TYPE_NONE;
   private String lastContentPath_;
   private boolean populating_ = false;
   private RenderedDocPreview docPreview_;
   private PublishHtmlSource publishHtmlSource_;
   
   private final boolean showCaption_;
   private final AppCommand boundCommand_;

   private RSConnectDeploymentRecord defaultRec_;
}
