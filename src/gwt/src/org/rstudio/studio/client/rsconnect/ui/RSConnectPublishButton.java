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
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentCompletedEvent;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.model.RenderedDocPreview;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
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

   class DeploymentPopupMenu extends ToolbarPopupMenu
   {
      @Override
      public void getDynamicPopupMenu(final 
            ToolbarPopupMenu.DynamicPopupMenuCallback callback)
      {
         rebuildPopupMenu(callback);
      }
   }

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
      
      panel.add(publishButton_);
      
      // create drop menu of previous deployments/other commands
      publishMenu_ = new DeploymentPopupMenu();
      publishMenuButton_ = new ToolbarButton(publishMenu_, false);
      panel.add(publishMenuButton_);
      
      // initialize composite widget
      initWidget(panel);

      // initialize injected members
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // compute initial visible state
      applyVisiblity();
      applyCaption("Publish");
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
      boolean wasVisible = visible_;
      visible_ = visible;
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
   
   public void setRmd(String rmd, boolean isStatic)
   {
      docPreview_ = new RenderedDocPreview(rmd, "", isStatic);
      setContentPath(rmd, "");
      setContentType(RSConnect.CONTENT_TYPE_DOCUMENT);
      applyVisiblity();
   }
   
   public void setIsStatic(boolean isStatic)
   {
      if (docPreview_ != null)
      {
        docPreview_.setIsStatic(isStatic);
      }
      applyVisiblity();
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
         if (contentType == RSConnect.CONTENT_TYPE_DOCUMENT ||
             contentType == RSConnect.CONTENT_TYPE_APP)
            populateDeployments(true);
         
         // moving to a raw HTML type: erase the deployment list
         if (contentType == RSConnect.CONTENT_TYPE_HTML ||
             contentType == RSConnect.CONTENT_TYPE_PRES)
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
   

   public void setShowCaption(boolean show)
   {
      if (showCaption_ != show)
      {
         showCaption_ = show;
         applyCaption();
      }
   }
   
   public void setManuallyHidden(boolean hide)
   {
      if (manuallyHidden_ != hide)
      {
         manuallyHidden_ = hide;
         applyVisiblity();
      }
   }

   // Private methods --------------------------------------------------------
   
   private void populateDeployments(final boolean force)
   {
      // force menu to think this is a new path to check for deployments
      if (force)
         populatedPath_ = null;
      
      // if we don't need to recompute the caption, stop now
      if (!showCaption_)
         return;

      rebuildPopupMenu(null);
   }
   
   private void onPublishClick(final RSConnectDeploymentRecord previous)
   {
      switch (contentType_)
      {
      case RSConnect.CONTENT_TYPE_HTML:
      case RSConnect.CONTENT_TYPE_PRES:
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
                           contentType_, contentPath_, arg, 
                           publishHtmlSource_.getTitle(), previous));
                  }
               });
         break;
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
      defaultRec_ = null;

      // if there are existing deployments, make the UI reflect that this is a
      // republish
      if (recs != null && recs.length() > 0)
      {
         applyCaption("Republish");

         // find the default (last deployed record)--this needs to be done as
         // a first pass so we can identify the associated menu item in one
         // pass 
         for (int i  = 0; i < recs.length(); i++)
         {
            final RSConnectDeploymentRecord rec = recs.get(i);
            if (rec == null)
               continue;
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
         applyCaption("Publish");

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
      publishMenuButton_.setVisible(recomputeMenuVisiblity());
      setVisible(recomputeVisibility());
   }
   
   // recomputes visibility for the popup menu that offers republish
   // destinations
   private boolean recomputeMenuVisiblity()
   {
      if (pUiPrefs_.get().enableRStudioConnect().getGlobalValue())
      {
         // always show the menu when RSConnect is enabled
         return true;
      }
      else if (contentType_ == RSConnect.CONTENT_TYPE_DOCUMENT &&
          docPreview_ != null)
      {
         // show the menu for Shiny documents
         return !docPreview_.isStatic();
      }
      else if (contentType_ == RSConnect.CONTENT_TYPE_APP)
      {
         // show the menu for Shiny apps
         return true;
      }
      
      // hide the menu for everything else
      return false;
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
            contentType_ == RSConnect.CONTENT_TYPE_PLOT ||
            contentType_ == RSConnect.CONTENT_TYPE_PRES) &&
           publishHtmlSource_ == null)
         return false;
      
      if (contentType_ == RSConnect.CONTENT_TYPE_APP && 
          StringUtil.isNullOrEmpty(contentPath_))
         return false;

      if (contentType_ == RSConnect.CONTENT_TYPE_DOCUMENT && 
          docPreview_ == null)
         return false;
      
      // if we don't have static output and RStudio Connect isn't enabled
      if (contentType_ == RSConnect.CONTENT_TYPE_DOCUMENT && 
          StringUtil.isNullOrEmpty(outputPath_) &&
          !pUiPrefs_.get().enableRStudioConnect().getGlobalValue() &&
          docPreview_ != null && 
          docPreview_.isStatic())
      {
         return false;
      }
      
      if (manuallyHidden_)
         return false;

      // looks like we should be visible
      return true;
   }
   
   private void applyCaption(String caption)
   {
      caption_ = caption;
      applyCaption();
   }

   private void applyCaption()
   {
      publishButton_.setText(showCaption_ ? caption_ : "");
   }
   
   // rebuilds the popup menu--this can happen when the menu is invoked; it can
   // also happen when the button is created if we're aggressively checking
   // publish status
   private void rebuildPopupMenu(final 
         ToolbarPopupMenu.DynamicPopupMenuCallback callback)
   {
      final ToolbarPopupMenu menu = publishMenu_;

      // prevent reentrancy
      if (contentPath_ == null || populating_)
      {
         if (callback != null)
            callback.onPopupMenu(menu);
         return;
      }
      
      // avoid populating if we've already set the deployments for this path
      // (unless we're forcefully repopulating)
      if (populatedPath_ != null && populatedPath_.equals(contentPath_))
      {
         if (callback != null)
            callback.onPopupMenu(menu);
         return;
      }
   
      // if this is a Shiny application and an .R file is being invoked, check
      // for deployments of its parent path
      String contentPath = contentPath_;
      if (contentType_ == RSConnect.CONTENT_TYPE_APP &&
          StringUtil.getExtension(contentPath_).equalsIgnoreCase("r")) {
         FileSystemItem fsiContent = FileSystemItem.createFile(contentPath_);
         contentPath = fsiContent.getParentPathString();
      }
      populating_ = true;
      server_.getRSConnectDeployments(contentPath, 
            outputPath_ == null ? "" : outputPath_,
            new ServerRequestCallback<JsArray<RSConnectDeploymentRecord>>()
      {
         @Override
         public void onResponseReceived(JsArray<RSConnectDeploymentRecord> recs)
         {
            populatedPath_ = contentPath_;
            populating_ = false;
            setPreviousDeployments(recs);
            if (callback != null)
               callback.onPopupMenu(menu);
         }
         
         @Override
         public void onError(ServerError error)
         {
            populating_ = false;
            if (callback != null)
               callback.onPopupMenu(menu);
         }
      });
   }
   
   private final ToolbarButton publishButton_;
   private final DeploymentPopupMenu publishMenu_;
   private ToolbarButton publishMenuButton_;

   private RSConnectServerOperations server_;
   private EventBus events_;
   private Commands commands_;
   private GlobalDisplay display_;
   private Session session_;
   private Provider<UIPrefs> pUiPrefs_;

   private String contentPath_;
   private String outputPath_;
   private int contentType_ = RSConnect.CONTENT_TYPE_NONE;
   private String populatedPath_;
   private boolean populating_ = false;
   private boolean showCaption_ = true;
   private RenderedDocPreview docPreview_;
   private PublishHtmlSource publishHtmlSource_;
   private String caption_;
   private boolean manuallyHidden_ = false;
   private boolean visible_ = false;
   
   private final AppCommand boundCommand_;

   private RSConnectDeploymentRecord defaultRec_;
}
