/*
 * RSConnectPublishButton.java
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
package org.rstudio.studio.client.rsconnect.ui;

import java.util.ArrayList;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.EnabledChangedEvent;
import org.rstudio.core.client.command.EnabledChangedHandler;
import org.rstudio.core.client.command.VisibleChangedEvent;
import org.rstudio.core.client.command.VisibleChangedHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.rpubs.events.RPubsUploadStatusEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputInfo;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeployInitiatedEvent;
import org.rstudio.studio.client.rsconnect.events.RSConnectDeploymentCompletedEvent;
import org.rstudio.studio.client.rsconnect.model.PlotPublishMRUList;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSettings;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishSource;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.model.RenderedDocPreview;
import org.rstudio.studio.client.rsconnect.model.PlotPublishMRUList.Entry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
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

   public RSConnectPublishButton(String host, int contentType, boolean showCaption,
         AppCommand boundCommand)
   {
      host_ = host;
      contentType_ = contentType;
      showCaption_ = showCaption;
      boundCommand_ = boundCommand;
      
      // create root widget
      HorizontalPanel panel = new HorizontalPanel();
      
      // create publish button itself
      publishButton_ = new ToolbarButton(
            ToolbarButton.NoText,
            RStudioGinjector.INSTANCE.getCommands().rsconnectDeploy().getTooltip(),
            RStudioGinjector.INSTANCE.getCommands().rsconnectDeploy().getImageResource(),
            new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent arg0)
               {
                  onPublishButtonClick();
               }
            });

      panel.add(publishButton_);
      
      // create drop menu of previous deployments/other commands
      publishMenu_ = new DeploymentPopupMenu();
      publishMenuButton_ = new ToolbarMenuButton(ToolbarButton.NoText, "Publish options", publishMenu_, true);
      panel.add(publishMenuButton_);
      
      // initialize composite widget
      initWidget(panel);

      // initialize injected members
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // compute initial visible state
      applyVisibility();
      applyCaption("Publish");
      setPreviousDeployments(null);
      
      // give ourselves some breathing room on the right
      getElement().getStyle().setMarginRight(4, Unit.PX);    
   }
   
   @Inject
   public void initialize(RSConnectServerOperations server,
         RMarkdownServerOperations rmdServer,
         EventBus events, 
         Commands commands,
         GlobalDisplay display,
         Provider<UserPrefs> pUserPrefs,
         Provider<UserState> pUserState,
         Session session,
         PlotPublishMRUList plotMru)
   {
      server_ = server;
      rmdServer_ = rmdServer;
      events_ = events;
      commands_ = commands;
      display_ = display;
      session_ = session;
      pUserPrefs_ = pUserPrefs;
      pUserState_ = pUserState;
      plotMru_ = plotMru;
      
      // initialize visibility if requested
      if (boundCommand_ != null) 
      {
         boundCommand_.addVisibleChangedHandler(
               new VisibleChangedHandler()
         {
            @Override
            public void onVisibleChanged(VisibleChangedEvent event)
            {
               applyVisibility();
            }
         });

         boundCommand_.addEnabledChangedHandler(
               new EnabledChangedHandler()
         {
            @Override
            public void onEnabledChanged(EnabledChangedEvent event)
            {
               applyVisibility();
            }
         });
      }
      
      events_.addHandler(RSConnectDeploymentCompletedEvent.TYPE, this);
      events_.addHandler(RPubsUploadStatusEvent.TYPE, this);
   }
   
   public void onPublishInvoked(Command onPublishInvoked)
   {
      onPublishInvoked_ = onPublishInvoked;
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
         setContentType(params.isWebsiteRmd() ? 
               RSConnect.CONTENT_TYPE_WEBSITE :
               RSConnect.CONTENT_TYPE_DOCUMENT);
         docPreview_ = new RenderedDocPreview(params);
         setContentPath(params.getResult().getTargetFile(),
               params.getOutputFile());
      }
      else
      {
         docPreview_ = null;
      }
      applyVisibility();
   }
   
   public void setShinyPreview(ShinyApplicationParams params)
   {
      String ext = params.getPath() == null ? "" :
            FileSystemItem.getExtensionFromPath(params.getPath()).toLowerCase();
      setContentPath(params.getPath(), "");
      setContentType(ext == ".r" ?
                        RSConnect.CONTENT_TYPE_APP_SINGLE :
                        RSConnect.CONTENT_TYPE_APP);
   }
   
   public void setPlumberPreview(PlumberAPIParams params)
   {
      setContentPath(params.getPath(), "");
      setContentType(RSConnect.CONTENT_TYPE_PLUMBER_API);
   }
    
   public void setHtmlPreview(HTMLPreviewResult params)
   {
      if (params.getSucceeded())
      {
         setContentPath(params.getSourceFile(), params.getHtmlFile());
         docPreview_ = new RenderedDocPreview(params);
         applyVisibility();
      }
   }
   
   public void setRmd(String rmd, boolean isStatic)
   {
      docPreview_ = new RenderedDocPreview(rmd, "", isStatic);
      setContentPath(rmd, "");

      SessionInfo sessionInfo = session_.getSessionInfo();
      String buildType = sessionInfo.getBuildToolsType();

      boolean setType = false;
      if (buildType == SessionInfo.BUILD_TOOLS_WEBSITE)
      {
         // if this is an Rmd with a content path
         if (contentType_ == RSConnect.CONTENT_TYPE_DOCUMENT &&
             !StringUtil.isNullOrEmpty(contentPath_))
         {
            // ...and if the content path is within the website dir,
            String websiteDir = sessionInfo.getBuildTargetDir();
            if (contentPath_.startsWith(websiteDir))
            {
               setType = true;
               setContentType(RSConnect.CONTENT_TYPE_WEBSITE);
            }
         }
      }

      // if we haven't set the type yet, apply it
      if (!setType)
         setContentType(RSConnect.CONTENT_TYPE_DOCUMENT);

      applyVisibility();
   }
   
   public void setIsStatic(boolean isStatic)
   {
      if (docPreview_ != null)
      {
        docPreview_.setIsStatic(isStatic);
      }
      applyVisibility();
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
             contentType == RSConnect.CONTENT_TYPE_APP ||
             contentType == RSConnect.CONTENT_TYPE_APP_SINGLE ||
             contentType == RSConnect.CONTENT_TYPE_PLUMBER_API ||
             contentType == RSConnect.CONTENT_TYPE_WEBSITE)
            populateDeployments(true);
         
         // moving to a raw HTML type: erase the deployment list
         if (contentType == RSConnect.CONTENT_TYPE_HTML ||
             contentType == RSConnect.CONTENT_TYPE_PRES)
            setPreviousDeployments(null);
      }
      applyVisibility();
   }
   
   public void setPublishHtmlSource(PublishHtmlSource source)
   {
      publishHtmlSource_ = source;
      setPreviousDeployments(null);
      applyVisibility();
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
         applyVisibility();
      }
   }
   
   public static boolean isAnyRmdRenderPending()
   {
      return anyRmdRenderPending_;
   }
   
   public void invokePublish()
   {
      onPublishButtonClick();
   }

   @Override
   protected void onAttach()
   {
      super.onAttach();

      ElementIds.assignElementId(
            publishButton_, ElementIds.PUBLISH_ITEM + "_" + host_);
      ElementIds.assignElementId(
            publishMenuButton_, ElementIds.PUBLISH_SHOW_DEPLOYMENTS + "_" + host_);
   }

   // Private methods --------------------------------------------------------

   private void onPublishButtonClick()
   {
      // let host know if requested
      if (onPublishInvoked_ != null)
         onPublishInvoked_.execute();
      
      // if the publish button is clicked without the droplist ever being 
      // invoked, then we need to grab the list of existing deployments to
      // determine what the default one will be.
      if (defaultRec_ == null && populatedPath_ == null)
      {
         rebuildPopupMenu(new ToolbarPopupMenu.DynamicPopupMenuCallback()
         {
            @Override
            public void onPopupMenu(ToolbarPopupMenu menu)
            {
               onPublishRecordClick(defaultRec_);
            }
         });
      }
      else
      {
         onPublishRecordClick(defaultRec_);
      }
   }
   
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
   
   private void onPublishRecordClick(final RSConnectDeploymentRecord previous)
   {
      // preflight check: if this deployment record belongs to a server that isn't registered,
      // display a friendly message
      if (previous != null && !previous.isServerRegistered())
      {
         // compute the host/server name to display; use the server alias by default, but pick up
         // the host name instead if it's been recorded (older releases of rsconnect didn't
         // save this information)
         String host = previous.getServer();
         if (!StringUtil.isNullOrEmpty(previous.getHostUrl()))
            host = StringUtil.getAuthorityFromUrl(previous.getHostUrl());

         display_.showMessage(GlobalDisplay.MSG_WARNING, 
               host + " Not Registered", 
               "This copy of the content has been published to the server " +
               "'" + host + "', " +
               "but you currently do not have any accounts registered on that server. \n\n" +
               "Connect an account on the server " +
               "'" + host + "' " + 
               "to update the application, or publish the content to a different server.");
      }
      
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
                              RSConnectActionEvent.DeployPlotEvent(htmlFile,
                                    previous));
                     }
                  });
         }
         break;
      case RSConnect.CONTENT_TYPE_APP:
      case RSConnect.CONTENT_TYPE_APP_SINGLE:
         // Shiny application
         events_.fireEvent(RSConnectActionEvent.DeployAppEvent(
               contentPath_, contentType_, previous));
         break;
      case RSConnect.CONTENT_TYPE_DOCUMENT:
      case RSConnect.CONTENT_TYPE_WEBSITE:
         // All R Markdown variants (single/multiple and static/Shiny)
         if (docPreview_.getSourceFile() == null)
         {
            display_.showErrorMessage("Unsaved Document", 
                  "Unsaved documents cannot be published. Save the document " +
                  "before publishing it.");
            break;
         }
         fireDeployDocEvent(previous);
         break;
      case RSConnect.CONTENT_TYPE_PLUMBER_API:
         events_.fireEvent(RSConnectActionEvent.DeployAPIEvent(contentPath_, contentType_, previous));
         break;
      default: 
         // should never happen 
         display_.showErrorMessage("Can't publish " + 
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
                     onPublishRecordClick(rec);
                  }
               });
            publishMenu_.addItem(menuItem);
         }
         
         publishMenu_.addItem(new MenuItem(
               AppCommand.formatMenuLabel(null, "Clear List", null),
               true,
               new Scheduler.ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     String appLabel = StringUtil.isNullOrEmpty(applicationPath_)
                           ? "this application"
                           : "'" + applicationPath_ + "'";
                     
                     display_.showYesNoMessage(
                           GlobalDisplay.MSG_INFO,
                           "Clear List",
                           "Are you sure you want to remove all local deployment history for " + appLabel + "?",
                           false,
                           () -> { forgetDeployment(); },
                           null,
                           false);
                  }
               }));
         
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
                     onPublishRecordClick(null);
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
                     onPublishRecordClick(defaultRec_);
                  }
               }));
      }

      // if it's a plot, show an MRU of recently deployed plot "names"
      if (contentType_ == RSConnect.CONTENT_TYPE_PLOT)
      {
         plotMru_.addPlotMruEntries(publishMenu_, 
               new OperationWithInput<PlotPublishMRUList.Entry>()
         {
            @Override
            public void execute(Entry plot)
            {
               republishPlot(plot);
            }
         });
      }
      publishMenu_.addSeparator();
      publishMenu_.addItem(
            commands_.rsconnectManageAccounts().createMenuItem(false));
   }
   
   private void republishPlot(final PlotPublishMRUList.Entry plot)
   {
      if (publishHtmlSource_ != null)
      {
         publishHtmlSource_.generatePublishHtml(
               new CommandWithArg<String>()
               {
                  @Override
                  public void execute(String htmlFile)
                  {
                     RSConnectPublishSource source = 
                           new RSConnectPublishSource(htmlFile, null, 
                                 true, true, false, "Plot", contentType_);
                     ArrayList<String> deployFiles = new ArrayList<String>();
                     deployFiles.add(FilePathUtils.friendlyFileName(htmlFile));
                     RSConnectPublishSettings settings = 
                           new RSConnectPublishSettings(
                                 deployFiles, 
                                 new ArrayList<String>(), 
                                 new ArrayList<String>(), 
                                 false, true);
                     events_.fireEvent(
                           new RSConnectDeployInitiatedEvent(source, settings,
                                 true, RSConnectDeploymentRecord.create(
                                       plot.name, null, null, plot.account, 
                                       plot.server)));
                  }
               });
      }
   }
   
   private void applyVisibility()
   {
      publishMenuButton_.setVisible(recomputeMenuVisibility());
      setVisible(recomputeVisibility());
   }
   
   // recomputes visibility for the popup menu that offers republish
   // destinations
   private boolean recomputeMenuVisibility()
   {
      if (pUserState_.get().enableRsconnectPublishUi().getGlobalValue())
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
      else if (contentType_ == RSConnect.CONTENT_TYPE_APP ||
               contentType_ == RSConnect.CONTENT_TYPE_APP_SINGLE)
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
          !pUserState_.get().showPublishUi().getGlobalValue())
         return false;
      
      // if both internal and external publishing is disabled, hide ourselves
      if (!session_.getSessionInfo().getAllowExternalPublish() &&
          !pUserState_.get().enableRsconnectPublishUi().getGlobalValue())
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
      
      if ((contentType_ == RSConnect.CONTENT_TYPE_APP ||
           contentType_ == RSConnect.CONTENT_TYPE_APP_SINGLE) && 
          StringUtil.isNullOrEmpty(contentPath_))
         return false;

      // If publishing to Connect is disabled, then we can't publish APIs
      if (contentType_ == RSConnect.CONTENT_TYPE_PLUMBER_API &&
          !pUserState_.get().enableRsconnectPublishUi().getGlobalValue())
      {
         return false;
      }
      
      // Only show publishing button in editor for Plumber APIs
      if (contentType_ == RSConnect.CONTENT_TYPE_PLUMBER_API && !StringUtil.equals(host_, HOST_EDITOR))
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
      if (populating_)
      {
         if (callback != null)
            callback.onPopupMenu(menu);
         return;
      }

      // handle case where we don't have a content path (i.e. plots)
      if (contentPath_ == null)
      {
         setPreviousDeployments(null);
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
   
      // compute the application path from the actual document path
      applicationPath_ = contentPath_;
      boolean parent = false;

      // if this is a Shiny application or API and an .R file is being invoked, check
      // for deployments of its parent path (single-file apps have
      // CONTENT_TYPE_APP_SINGLE and their own deployment records; APIs are always deployed
      // via parent path)
      if ((contentType_ == RSConnect.CONTENT_TYPE_APP || 
            contentType_ == RSConnect.CONTENT_TYPE_PLUMBER_API) &&
          StringUtil.getExtension(contentPath_).equalsIgnoreCase("r")) 
         parent = true;
      
      if (contentType_ == RSConnect.CONTENT_TYPE_WEBSITE)
      {
         if (docPreview_ != null && 
               docPreview_.isStatic() && 
               !StringUtil.isNullOrEmpty(docPreview_.getWebsiteDir()))
         {
            // preview of a static website, use the website path to search for deployments
            applicationPath_ = docPreview_.getWebsiteDir(); 
         }
         else
         {
            // if this is a document in website, use the parent path
            parent = true;
         }
      }

      // apply parent path if needed
      if (parent)
      {
         FileSystemItem fsiContent = FileSystemItem.createFile(contentPath_);
         applicationPath_ = fsiContent.getParentPathString();
      }

      populating_ = true;
      server_.getRSConnectDeployments(
            applicationPath_, 
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
   
   private void forgetDeployment()
   {
      server_.forgetRSConnectDeployments(
            applicationPath_ == null ? contentPath_ : applicationPath_,
            StringUtil.notNull(outputPath_),
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void response)
               {
                  populateDeployments(true);
                  
                  String appLabel = StringUtil.isNullOrEmpty(applicationPath_)
                        ? "this application"
                        : "'" + applicationPath_ + "'";
                  
                  display_.showMessage(
                        GlobalDisplay.MSG_INFO,
                        "Clear List",
                        "Local deployment history for " + appLabel + " successfully removed.");
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }

   // fetch render output info if necessary and then fire deployment event
   private void fireDeployDocEvent(final RSConnectDeploymentRecord previous)
   {
      // prevent re-entrancy
      if (rmdInfoPending_)
         return;
      
      if (StringUtil.isNullOrEmpty(docPreview_.getOutputFile()))
      {
         rmdInfoPending_ = true;
         rmdServer_.getRmdOutputInfo(contentPath_,
               new ServerRequestCallback<RmdOutputInfo>()
               {
                  @Override
                  public void onResponseReceived(RmdOutputInfo response)
                  {
                     RenderedDocPreview preview = new RenderedDocPreview(contentPath_,
                           response.output_file_exists ? response.output_file : "", true);
                     events_.fireEvent(RSConnectActionEvent.DeployDocEvent(
                           preview, contentType_, previous));
                     rmdInfoPending_ = false;
                  }
            
                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                     display_.showErrorMessage("Content Publish Failed",
                           "Unable to determine file to be published. Click Knit or Preview " +
                           "to render it again, then click the Publish button above the rendered document.");
                     rmdInfoPending_ = false;
                  }
               });
      }
      else
      {
         events_.fireEvent(RSConnectActionEvent.DeployDocEvent(docPreview_, contentType_, previous));
      }
   }
   
   public final static String HOST_EDITOR = "editor";
   public final static String HOST_PLOTS = "plots_pane";
   public final static String HOST_PRESENTATION = "presentation";
   public final static String HOST_PROFILER = "profiler";
   public final static String HOST_RMD_OUTPUT = "rmd_output";
   public final static String HOST_SHINY_APP = "shiny_app";
   public final static String HOST_HTML_PREVIEW = "html_preview";
   public final static String HOST_VIEWER = "viewer";
   public final static String HOST_PLUMBER_API = "plumber_api";
   
   private final ToolbarButton publishButton_;
   private final DeploymentPopupMenu publishMenu_;
   private ToolbarMenuButton publishMenuButton_;

   private RSConnectServerOperations server_;
   private RMarkdownServerOperations rmdServer_;
   private EventBus events_;
   private Commands commands_;
   private GlobalDisplay display_;
   private Session session_;
   @SuppressWarnings("unused")
   private Provider<UserPrefs> pUserPrefs_;
   private Provider<UserState> pUserState_;
   private PlotPublishMRUList plotMru_;

   private String contentPath_;
   private String applicationPath_;
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
   private boolean rmdInfoPending_ = false;
   
   private final AppCommand boundCommand_;
   private Command onPublishInvoked_;

   private RSConnectDeploymentRecord defaultRec_;
   private final String host_;   
   private static boolean anyRmdRenderPending_ = false;
}
