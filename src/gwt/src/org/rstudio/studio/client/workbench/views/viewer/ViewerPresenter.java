/*
 * ViewerPresenter.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
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
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.zoom.ZoomUtils;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.model.ShinyViewerType;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotUtils;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.source.SourceShim;
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
                                        ShinyApplicationStatusEvent.Handler
{
   public interface Binder extends CommandBinder<Commands, ViewerPresenter> {}
   
   public interface Display extends WorkbenchView
   {
      void navigate(String url);
      void setExportEnabled(boolean exportEnabled);
      void previewRmd(RmdPreviewParams params);
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
                          SourceShim sourceShim,
                          Provider<UIPrefs> pUIPrefs)
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
      sourceShim_ = sourceShim;
      pUIPrefs_ = pUIPrefs;
      
      binder.bind(commands, this);
      
      manageCommands(false);
      
      // show a stop button when the console is busy (the stop and 
      // clear commands are mutually exclusive)
      commands_.viewerStop().setVisible(commands_.interruptR().isEnabled());
      commands_.viewerClear().setVisible(!commands_.viewerStop().isVisible());
      commands_.interruptR().addEnabledChangedHandler(
                                                new EnabledChangedHandler() {
         @Override
         public void onEnabledChanged(AppCommand command)
         {
            commands_.viewerStop().setVisible(command.isEnabled());
            commands_.viewerClear().setVisible(!command.isEnabled());
            commands_.viewerClearAll().setVisible(!command.isEnabled());
         }
      });
      
      eventBus.addHandler(ShinyApplicationStatusEvent.TYPE, this);
      initializeEvents();
   }
   

   @Override
   public void onViewerCleared(ViewerClearedEvent event)
   {
      if (!event.isForStop())
         navigate(ViewerPane.ABOUT_BLANK);
   }

   @Override
   public void onViewerNavigate(ViewerNavigateEvent event)
   { 
      if (event.getURL().length() > 0)
      {
         manageCommands(true, event);
         
         if (event.getBringToFront())
            display_.bringToFront();
      
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
         
         navigate(ViewerPane.ABOUT_BLANK);
      }
   }
   
   @Override
   public void onViewerPreviewRmd(ViewerPreviewRmdEvent event)
   {
      manageCommands(true);
      display_.bringToFront();
      if (!event.isRefresh())
         display_.maximize();
      rmdPreviewParams_ = event.getParams();
      if (Desktop.isDesktop())
         Desktop.getFrame().setViewerUrl(event.getParams().getOutputUrl());
      display_.previewRmd(event.getParams());
   }
   
   @Override
   public void onShinyApplicationStatus(ShinyApplicationStatusEvent event)
   {
      if (event.getParams().getViewerType() == 
            ShinyViewerType.SHINY_VIEWER_PANE &&
          event.getParams().getState() == 
            ShinyApplicationParams.STATE_STARTED)
      {
         manageCommands(true);
         display_.bringToFront();
         navigate(event.getParams().getUrl());
         runningShinyAppParams_ = event.getParams();
      }
   }

   @Handler
   public void onViewerPopout() { display_.popout(); }
   @Handler
   public void onViewerRefresh() { display_.refresh(); }
   
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
      
      globalDisplay_.openMinimalWindow(display_.getUrl(),
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
                         pUIPrefs_.get().exportViewerOptions().getValue(),
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
               pUIPrefs_.get().exportViewerOptions().getValue(),
               display_.getViewerFrameSize()),
         saveExportOptionsOperation_
      ).showModal();;    
   }
   
   @Handler
   public void onViewerSaveAllAndRefresh()
   {
      sourceShim_.handleUnsavedChangesBeforeExit(
         sourceShim_.getUnsavedChanges(),
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
      if (Desktop.isDesktop())
         Desktop.getFrame().setViewerUrl(url);
      display_.navigate(url);
   }
   
   private void updateZoomWindow(String url)
   {
      if (Desktop.isDesktop())
         Desktop.getFrame().reloadViewerZoomWindow(url);
      else if ((zoomWindow_ != null) && !zoomWindow_.isClosed())
         zoomWindow_.setLocationHref(url);
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
      navigate(ViewerPane.ABOUT_BLANK);
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
      
      boolean canSnapshot = Desktop.isDesktop();
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
               UIPrefs uiPrefs = pUIPrefs_.get();
               if (!ExportPlotOptions.areEqual(
                               options,
                               uiPrefs.exportViewerOptions().getValue()))
               {
                  uiPrefs.exportViewerOptions().setGlobalValue(options);
                  uiPrefs.writeUIPrefs();
               }
            }
         };
   
   
   private native void initializeEvents() /*-{  
      var thiz = this;   
      $wnd.addEventListener(
            "message",
            $entry(function(e) {
               thiz.@org.rstudio.studio.client.workbench.views.viewer.ViewerPresenter::onMessage(Ljava/lang/String;Ljava/lang/String;)(e.data, e.origin);
            }),
            true);
   }-*/;
   
   private void onMessage(String data, String origin)
   {  
      if ("disconnected".equals(data))
      {
         // ensure the frame url starts with the specified origin
         if (display_.getUrl().startsWith(origin))
            onViewerClear();
      }
   }
   
   private String normalizeUrl(String url)
   {
      if (url.endsWith("/"))
         return url.substring(0, url.length()-1);
      else
         return url;
   }
   
   private final Display display_ ;
   private final Commands commands_;
   private final GlobalDisplay globalDisplay_;
   private final WorkbenchContext workbenchContext_;
   private final ViewerServerOperations server_;
   private final EventBus events_;
   private final DependencyManager dependencyManager_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileSystemContext_;
   private final Provider<UIPrefs> pUIPrefs_;
   private final SourceShim sourceShim_; 
   
   private FileSystemItem saveAsWebPageDefaultPath_ = null;
   
   private ShinyApplicationParams runningShinyAppParams_;
   private RmdPreviewParams rmdPreviewParams_;
   
   private WindowEx zoomWindow_ = null;
   private Size zoomWindowDefaultSize_ = null;
}