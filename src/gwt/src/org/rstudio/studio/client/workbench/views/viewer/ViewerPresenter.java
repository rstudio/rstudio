/*
 * ViewerPresenter.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer;

import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.HtmlMessageListener;
import org.rstudio.core.client.SingleShotTimer;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.URIConstants;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.EnabledChangedEvent;
import org.rstudio.core.client.command.EnabledChangedHandler;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.zoom.ZoomUtils;
import org.rstudio.studio.client.plumber.events.PlumberAPIStatusEvent;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.ShinyDisconnectNotifier;
import org.rstudio.studio.client.shiny.ShinyDisconnectNotifier.ShinyDisconnectSource;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotUtils;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerClearedEvent;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigateEvent;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerPreviewRmdEvent;
import org.rstudio.studio.client.workbench.views.viewer.export.CopyViewerPlotToClipboardDesktopDialog;
import org.rstudio.studio.client.workbench.views.viewer.export.SaveViewerPlotAsImageDesktopDialog;
import org.rstudio.studio.client.workbench.views.viewer.model.ViewerServerOperations;

public class ViewerPresenter extends BasePresenter
                             implements ViewerNavigateEvent.Handler,
                                        ViewerPreviewRmdEvent.Handler,
                                        ViewerClearedEvent.Handler,
                                        ShinyApplicationStatusEvent.Handler,
                                        PlumberAPIStatusEvent.Handler,
                                        InterruptStatusEvent.Handler,
                                        ShinyDisconnectSource
{
   public interface Binder extends CommandBinder<Commands, ViewerPresenter> {}

   public interface Display extends WorkbenchView
   {
      void navigate(String url);
      void setExportEnabled(boolean exportEnabled);
      void previewRmd(RmdPreviewParams params);
      void previewShiny(ShinyApplicationParams params);
      void previewPlumber(PlumberAPIParams params);
      HandlerRegistration addLoadHandler(LoadHandler handler);
      String getUrl();
      String getTitle();
      void popout();
      void refresh();
      Size getViewerFrameSize();
   }

   @Inject
   public ViewerPresenter(Display display,
                          EventBus eventBus,
                          GlobalDisplay globalDisplay,
                          WorkbenchContext workbenchContext,
                          DependencyManager dependencyManager,
                          FileDialogs fileDialogs,
                          RemoteFileSystemContext fileSystemContext,
                          Commands commands,
                          Binder binder,
                          ViewerServerOperations server,
                          Source source,
                          Provider<UserPrefs> pUserPrefs,
                          Provider<UserState> pUserState,
                          HtmlMessageListener htmlMessageListener)
   {
      super(display);
      display_ = display;
      workbenchContext_ = workbenchContext;
      dependencyManager_ = dependencyManager;
      fileDialogs_ = fileDialogs;
      fileSystemContext_ = fileSystemContext;
      commands_ = commands;
      server_ = server;
      events_ = eventBus;
      globalDisplay_ = globalDisplay;
      source_ = source;
      pUserPrefs_ = pUserPrefs;
      pUserState_ = pUserState;
      htmlMessageListener_ = htmlMessageListener;

      binder.bind(commands, this);

      manageCommands(false);

      // show a stop button when the console is busy (the stop and
      // clear commands are mutually exclusive)
      commands_.viewerStop().setVisible(commands_.interruptR().isEnabled());
      commands_.viewerClear().setVisible(!commands_.viewerStop().isVisible());
      commands_.interruptR().addEnabledChangedHandler(
                                                new EnabledChangedHandler() {
         @Override
         public void onEnabledChanged(EnabledChangedEvent event)
         {
            commands_.viewerStop().setVisible(event.getCommand().isEnabled());
            commands_.viewerClear().setVisible(!event.getCommand().isEnabled());
            commands_.viewerClearAll().setVisible(!event.getCommand().isEnabled());
         }
      });

      eventBus.addHandler(ShinyApplicationStatusEvent.TYPE, this);
      eventBus.addHandler(PlumberAPIStatusEvent.TYPE, this);
      eventBus.addHandler(InterruptStatusEvent.TYPE, this);

      // listen for Shiny disconnection events
      shinyNotifier_ = new ShinyDisconnectNotifier(this);

      if (BrowseCap.isFirefox())
      {
         display_.addLoadHandler((evt) ->
         {
            // for Firefox, we need to suppress disconnection events during a
            // reload; this load signals that the reload is complete
            shinyNotifier_.unsuppress();
         });
      }
   }


   @Override
   public void onViewerCleared(ViewerClearedEvent event)
   {
      if (!event.isForStop())
         navigate(URIConstants.ABOUT_BLANK);
   }

   @Override
   public void onViewerNavigate(ViewerNavigateEvent event)
   {
      if (event.getURL().length() > 0)
      {
         manageCommands(true, event);

         if (event.getBringToFront()) {
            display_.bringToFront();
            htmlMessageListener_.allowOpenOnLoad();
         }

         // respect height request
         int ensureHeight = event.getHeight();
         if (ensureHeight < 0)
            display_.maximize();
         else if (ensureHeight > 0)
            display_.ensureHeight(ensureHeight);

         navigate(event.getURL());

         if (event.isHTMLWidget())
            updateZoomWindow(display_.getUrl());
      }
      else
      {
         manageCommands(false);

         navigate(URIConstants.ABOUT_BLANK);
      }
   }

   @Override
   public void onViewerPreviewRmd(ViewerPreviewRmdEvent event)
   {
      manageCommands(true);
      display_.bringToFront();
      if (event.getMaximize())
         display_.maximize();
      rmdPreviewParams_ = event.getParams();
      if (Desktop.hasDesktopFrame())
         Desktop.getFrame().setViewerUrl(StringUtil.notNull(event.getParams().getOutputUrl()));
      display_.previewRmd(event.getParams());
   }

   @Override
   public void onShinyApplicationStatus(ShinyApplicationStatusEvent event)
   {
      if (event.getParams().getViewerType() == UserPrefs.SHINY_VIEWER_TYPE_PANE &&
          event.getParams().getState() == ShinyApplicationParams.STATE_STARTED)
      {
         manageCommands(true);
         display_.bringToFront();
         if (Desktop.hasDesktopFrame())
            Desktop.getFrame().setViewerUrl(StringUtil.notNull(event.getParams().getUrl()));
         display_.previewShiny(event.getParams());
         runningShinyAppParams_ = event.getParams();
      }
   }

   @Override
   public void onPlumberAPIStatus(PlumberAPIStatusEvent event)
   {
      if (event.getParams().getViewerType() == UserPrefs.PLUMBER_VIEWER_TYPE_PANE &&
          event.getParams().getState() == PlumberAPIParams.STATE_STARTED)
      {
         // time out a bit to wait for swagger server to initialize
         SingleShotTimer.fire(100, () -> {
            manageCommands(true);
            display_.bringToFront();
            if (Desktop.hasDesktopFrame())
               Desktop.getFrame().setViewerUrl(StringUtil.notNull(event.getParams().getUrl()));
            display_.previewPlumber(event.getParams());
            runningPlumberAPIParams_ = event.getParams();
         });
      }
   }

   @Override
   public void onInterruptStatus(InterruptStatusEvent event)
   {
      if (runningPlumberAPIParams_ != null)
      {
         // Clear Plumber API from viewer pane when API stopped
         if (event.getStatus() != InterruptStatusEvent.INTERRUPT_COMPLETED)
            return;

         onViewerClear();
      }
   }

   @Override
   public String getShinyUrl()
   {
      return display_.getUrl();
   }

   @Override
   public void onShinyDisconnect()
   {
      // when Shiny applications disconnect themselves, clear them out of the the Viewer
      onViewerClear();
   }

   @Handler
   public void onViewerPopout()
   {
      display_.popout();
   }

   @Handler
   public void onViewerRefresh()
   {
      if (BrowseCap.isFirefox() && !StringUtil.isNullOrEmpty(getShinyUrl()))
      {
         // Firefox allows Shiny's disconnection notification (a "disconnected"
         // postmessage) through during the unload that occurs during refresh.
         // To keep this transient disconnection from being treated as an app
         // stop, we temporarily suppress it here.
         shinyNotifier_.suppress();
      }
      display_.refresh();
   }

   @Handler
   public void onViewerBack()
   {
      server_.viewerBack(new VoidServerRequestCallback());
   }

   @Handler
   public void onViewerForward()
   {
      server_.viewerForward(new VoidServerRequestCallback());
   }

   @Handler
   public void onViewerZoom()
   {
      Size windowSize = ZoomUtils.getZoomWindowSize(
            display_.getViewerFrameSize(), zoomWindowDefaultSize_);

      // open and activate window
      NewWindowOptions options = new NewWindowOptions();
      options.setName("_rstudio_viewer_zoom");
      options.setFocus(true);
      options.setCallback(new OperationWithInput<WindowEx>() {
         @Override
         public void execute(WindowEx input)
         {
            zoomWindow_ = input;
         }
      });

      String displayUrl = display_.getUrl().replaceAll(
         "capabilities=[^&]+",
         "no");

      globalDisplay_.openMinimalWindow(displayUrl,
            false,
            windowSize.width,
            windowSize.height,
            options);
   }

   @Handler
   public void onViewerSaveAsImage()
   {
      display_.bringToFront();

      final ProgressIndicator indicator =
         globalDisplay_.getProgressIndicator("Error");
      indicator.onProgress("Preparing to export plot...");

      // get the default directory
      FileSystemItem defaultDir = ExportPlotUtils.getDefaultSaveDirectory(
            workbenchContext_.getCurrentWorkingDir());

      // get context
      server_.getViewerExportContext(
         defaultDir.getPath(),
         new SimpleRequestCallback<SavePlotAsImageContext>() {

            @Override
            public void onResponseReceived(SavePlotAsImageContext context)
            {
               indicator.onCompleted();

               new SaveViewerPlotAsImageDesktopDialog(
                   globalDisplay_,
                   display_.getUrl(),
                   context,
                   ExportPlotOptions.adaptToSize(
                         pUserState_.get().exportViewerOptions().getValue().cast(),
                         display_.getViewerFrameSize()),
                   saveExportOptionsOperation_
               ).showModal();
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }
         });
   }

   @Handler
   public void onViewerSaveAsWebPage()
   {
      display_.bringToFront();

      if (saveAsWebPageDefaultPath_ == null)
         saveAsWebPageDefaultPath_ = workbenchContext_.getCurrentWorkingDir();

      dependencyManager_.withRMarkdown("Saving standalone web pages",
                                       new Command() {
         @Override
         public void execute()
         {
            fileDialogs_.saveFile(
                  "Save As Web Page",
                  fileSystemContext_,
                  saveAsWebPageDefaultPath_,
                  ".html",
                  false,
                  new ProgressOperationWithInput<FileSystemItem>(){

                     @Override
                     public void execute(final FileSystemItem targetFile,
                           ProgressIndicator indicator)
                     {
                        if (targetFile == null)
                        {
                           indicator.onCompleted();
                           return;
                        }

                        indicator.onProgress("Saving as web page...");

                        server_.viewerSaveAsWebPage(
                              targetFile.getPath(),
                              new VoidServerRequestCallback(indicator) {
                                 @Override
                                 public void onSuccess()
                                 {
                                    saveAsWebPageDefaultPath_ =
                                          targetFile.getParentPath();
                                 }
                              });
                     }
                  });
        }
      });
   }

   @Handler
   public void onViewerCopyToClipboard()
   {
      new CopyViewerPlotToClipboardDesktopDialog(
         display_.getUrl(),
         ExportPlotOptions.adaptToSize(
               pUserState_.get().exportViewerOptions().getValue().cast(),
               display_.getViewerFrameSize()),
         saveExportOptionsOperation_
      ).showModal();
   }

   @Handler
   public void onViewerSaveAllAndRefresh()
   {
      source_.handleUnsavedChangesBeforeExit(
         source_.getUnsavedChanges(Source.TYPE_FILE_BACKED),
         new Command() {
            @Override
            public void execute()
            {
               display_.refresh();
            }
         });
   }

   @Handler
   public void onViewerClear()
   {
      stop(false);
   }

   @Handler
   public void onViewerClearAll()
   {
      // confirm
      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,

         "Clear Viewer",

         "Are you sure you want to clear all of the items in the history?",

         new ProgressOperation() {
            public void execute(final ProgressIndicator indicator)
            {
               indicator.onProgress("Clearing viewer...");
               stop(false, true, indicator);
            }
         },

         true

       );
   }

   @Handler
   public void onViewerStop()
   {
      stop(true);
   }

   private void navigate(String url)
   {
      if (Desktop.hasDesktopFrame())
         Desktop.getFrame().setViewerUrl(StringUtil.notNull(url));
      display_.navigate(url);
   }

   private void updateZoomWindow(String url)
   {
      if (Desktop.hasDesktopFrame()) {
         Desktop.getFrame().setViewerUrl(StringUtil.notNull(url));
         Desktop.getFrame().reloadViewerZoomWindow(StringUtil.notNull(url));
      } else if ((zoomWindow_ != null) && !zoomWindow_.isClosed()) {
         zoomWindow_.setLocationHref(url);
      }
   }

   private void stop(boolean interruptR)
   {
      stop(interruptR, false, null);
   }

   private void stop(boolean interruptR,
                     boolean clearAll,
                     ProgressIndicator indicator)
   {
      // check whether this was a static widget (determine what we do
      // visa-vi widget history clearing/restoration)
      boolean wasStaticWidget = commands_.viewerZoom().isEnabled();

      manageCommands(false);
      navigate(URIConstants.ABOUT_BLANK);
      if (interruptR)
         commands_.interruptR().execute();
      server_.viewerStopped(new VoidServerRequestCallback());

      // If we were viewing a Shiny application, let the rest of the app know
      // that the application has been stopped
      if (runningShinyAppParams_ != null)
      {
         runningShinyAppParams_.setState(ShinyApplicationParams.STATE_STOPPED);
         events_.fireEvent(new ShinyApplicationStatusEvent(
               runningShinyAppParams_));
      }
      runningShinyAppParams_ = null;

      // Ditto for Plumber
      if (runningPlumberAPIParams_ != null)
      {
         runningPlumberAPIParams_.setState(PlumberAPIParams.STATE_STOPPED);
         events_.fireEvent(new PlumberAPIStatusEvent(runningPlumberAPIParams_));
      }
      runningPlumberAPIParams_ = null;

      events_.fireEvent(new ViewerClearedEvent(true));

      // if this was a static widget then clear the current widget
      if (clearAll)
         server_.viewerClearAll(new VoidServerRequestCallback(indicator));
      else if (wasStaticWidget)
         server_.viewerClearCurrent(new VoidServerRequestCallback(indicator));

      // otherwise restore the last static widget
      else
         server_.viewerCurrent(new VoidServerRequestCallback());
   }

   private void manageCommands(boolean enable)
   {
      manageCommands(enable, false, false, false);
   }

   private void manageCommands(boolean enable, ViewerNavigateEvent event)
   {
      manageCommands(enable,
                     event.isHTMLWidget(),
                     event.getHasNext(),
                     event.getHasPrevious());
   }

   private void manageCommands(boolean enable,
                               boolean isHTMLWidget,
                               boolean hasNext,
                               boolean hasPrevious)
   {
      commands_.viewerPopout().setEnabled(enable);
      commands_.viewerRefresh().setEnabled(enable);
      commands_.viewerClear().setEnabled(enable);
      commands_.viewerClearAll().setEnabled(enable);

      commands_.viewerBack().setEnabled(hasPrevious);
      commands_.viewerBack().setVisible(isHTMLWidget);
      commands_.viewerForward().setEnabled(hasNext);
      commands_.viewerForward().setVisible(isHTMLWidget);
      commands_.viewerZoom().setEnabled(enable);
      commands_.viewerZoom().setVisible(isHTMLWidget);

      boolean canSnapshot = Desktop.hasDesktopFrame();
      commands_.viewerSaveAsImage().setEnabled(enable && canSnapshot);
      commands_.viewerSaveAsImage().setVisible(isHTMLWidget && canSnapshot);
      commands_.viewerCopyToClipboard().setEnabled(enable && canSnapshot);
      commands_.viewerCopyToClipboard().setVisible(isHTMLWidget && canSnapshot);
      commands_.viewerSaveAsWebPage().setEnabled(enable);
      commands_.viewerSaveAsWebPage().setVisible(isHTMLWidget);

      display_.setExportEnabled(isHTMLWidget);
   }

   private OperationWithInput<ExportPlotOptions> saveExportOptionsOperation_ =
         new OperationWithInput<ExportPlotOptions>()
         {
            public void execute(ExportPlotOptions options)
            {
               UserState userState = pUserState_.get();
               if (!ExportPlotOptions.areEqual(
                               options,
                               userState.exportViewerOptions().getValue().cast()))
               {
                  userState.exportViewerOptions().setGlobalValue(options.cast());
                  userState.writeState();
               }
            }
         };

   private final Display display_;
   private final Commands commands_;
   private final GlobalDisplay globalDisplay_;
   private final WorkbenchContext workbenchContext_;
   private final ViewerServerOperations server_;
   private final EventBus events_;
   private final DependencyManager dependencyManager_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileSystemContext_;
   @SuppressWarnings("unused")
   private final Provider<UserPrefs> pUserPrefs_;
   private final Provider<UserState> pUserState_;
   private final Source source_;
   private final ShinyDisconnectNotifier shinyNotifier_;

   private FileSystemItem saveAsWebPageDefaultPath_ = null;

   private ShinyApplicationParams runningShinyAppParams_;
   private PlumberAPIParams runningPlumberAPIParams_;
   @SuppressWarnings("unused")
   private RmdPreviewParams rmdPreviewParams_;

   private WindowEx zoomWindow_ = null;
   private Size zoomWindowDefaultSize_ = null;

   private HtmlMessageListener htmlMessageListener_;

}
