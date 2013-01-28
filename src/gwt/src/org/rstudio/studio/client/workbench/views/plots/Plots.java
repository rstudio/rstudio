/*
 * Plots.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.plots;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Panel;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.NativeScreen;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.HasCustomizableToolbar;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.studio.client.application.events.DeferredInitCompletedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptHandler;
import org.rstudio.studio.client.workbench.views.plots.events.LocatorEvent;
import org.rstudio.studio.client.workbench.views.plots.events.LocatorHandler;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsZoomSizeChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsState;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlot;
import org.rstudio.studio.client.workbench.views.plots.ui.manipulator.ManipulatorChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.ui.manipulator.ManipulatorManager;

public class Plots extends BasePresenter implements PlotsChangedHandler,
                                                    LocatorHandler,
                                                    ConsolePromptHandler,
                                                    DeferredInitCompletedEvent.Handler,
                                                    PlotsZoomSizeChangedEvent.Handler
{
   public interface Parent extends HasWidgets, HasCustomizableToolbar
   {
   }
   
  
   public interface Display extends WorkbenchView, HasResizeHandlers
   {
      void showEmptyPlot();
      void showPlot(String plotUrl);
      String getPlotUrl();
      
      void refresh();
   
      Panel getPlotsSurface();
      
      Parent getPlotsParent();
      Size getPlotFrameSize();
   }
      

   @Inject
   public Plots(final Display view,
                GlobalDisplay globalDisplay,
                WorkbenchContext workbenchContext,
                Provider<UIPrefs> uiPrefs,
                Commands commands,
                EventBus events,
                final PlotsServerOperations server,
                Session session)
   {
      super(view);
      view_ = view;
      globalDisplay_ = globalDisplay;
      workbenchContext_ = workbenchContext;
      uiPrefs_ = uiPrefs;
      server_ = server;
      session_ = session;
      exportPlot_ = GWT.create(ExportPlot.class);
      zoomWindow_ = null;
      zoomWindowDefaultSize_ = null;
      
      locator_ = new Locator(view.getPlotsParent());
      locator_.addSelectionHandler(new SelectionHandler<Point>()
      {
         public void onSelection(SelectionEvent<Point> e)
         {
            org.rstudio.studio.client.workbench.views.plots.model.Point p = null;
            if (e.getSelectedItem() != null)
               p = org.rstudio.studio.client.workbench.views.plots.model.Point.create(
                     e.getSelectedItem().getX(),
                     e.getSelectedItem().getY()
               );
            server.locatorCompleted(p, new SimpleRequestCallback<Void>());
         }
      });

      // manipulator
      manipulatorManager_ = new ManipulatorManager(
         view_.getPlotsSurface(),
         commands,
        
         new ManipulatorChangedHandler()
         { 
            @Override
            public void onManipulatorChanged(JSONObject values)
            { 
               server_.setManipulatorValues(values, 
                                            new ManipulatorRequestCallback()); 
            }           
         },
         
         new ClickHandler() 
         {
            @Override
            public void onClick(ClickEvent event)
            {
              server_.manipulatorPlotClicked(event.getX(), 
                                             event.getY(), 
                                             new ManipulatorRequestCallback());
               
            }   
         }
      );
      
      events.addHandler(DeferredInitCompletedEvent.TYPE, this);
      events.addHandler(PlotsZoomSizeChangedEvent.TYPE, this);
}
   
   public void onPlotsChanged(PlotsChangedEvent event)
   {
      // get the event
      PlotsState plotsState = event.getPlotsState();
        
      // clear progress 
      view_.setProgress(false);
      manipulatorManager_.setProgress(false);
      
      // if this is the empty plot then clear the display
      // NOTE: we currently return a zero byte PNG as our "empty.png" from
      // the server. this is shown as a blank pane by Webkit, however
      // firefox shows the full URI of the empty.png rather than a blank
      // pane. therefore, we put in this workaround. 
      if (plotsState.getFilename().startsWith("empty."))
      {
         view_.showEmptyPlot(); 
      }
      else
      {
         String url = server_.getGraphicsUrl(plotsState.getFilename());
         view_.showPlot(url);
      }
      
      // activate the plots tab if requested
      if (plotsState.getActivatePlots())
         view_.bringToFront();
      
      // update plot size
      plotSize_ = new Size(plotsState.getWidth(), plotsState.getHeight());

      // manipulator
      manipulatorManager_.setManipulator(plotsState.getManipulator(),
                                         plotsState.getShowManipulator());
      
      // locator
      if (locator_.isActive())
         locate();
      
      
      // reload zoom window if we have one
      if (Desktop.isDesktop())
         Desktop.getFrame().reloadZoomWindow();
      else if ((zoomWindow_ != null) && !zoomWindow_.isClosed())
         zoomWindow_.reload();
   }

   void onNextPlot()
   {
      view_.bringToFront();
      setChangePlotProgress();
      server_.nextPlot(new PlotRequestCallback());
   }

   void onPreviousPlot()
   {
      view_.bringToFront();
      setChangePlotProgress();
      server_.previousPlot(new PlotRequestCallback());
   }
   
   void onRemovePlot()
   {
      // delete plot gesture indicates we are done with locator
      safeClearLocator();
      
      // confirm
      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
            
         "Remove Plot",
         
         "Are you sure you want to remove the current plot?",
  
         new ProgressOperation() {
            public void execute(final ProgressIndicator indicator)
            {
               indicator.onProgress("Removing plot...");
               server_.removePlot(new VoidServerRequestCallback(indicator));
            }
         },
      
         true
      
       );
      
      view_.bringToFront();
   }

   void onClearPlots()
   {      
      // clear plots gesture indicates we are done with locator
      safeClearLocator();
      
      // confirm
      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
            
         "Clear Plots",
         
         "Are you sure you want to clear all of the plots in the history?",
  
         new ProgressOperation() {
            public void execute(final ProgressIndicator indicator)
            {
               indicator.onProgress("Clearing plots...");
               server_.clearPlots(new VoidServerRequestCallback(indicator));
            }
         },
      
         true
      
       );
    }

   
   void onSavePlotAsImage()
   {
      view_.bringToFront();
      
      final ProgressIndicator indicator = 
         globalDisplay_.getProgressIndicator("Error");
      indicator.onProgress("Preparing to export plot...");

      // get the default directory
      FileSystemItem defaultDir = ExportPlot.getDefaultSaveDirectory(
            workbenchContext_.getCurrentWorkingDir());

      // get context
      server_.getSavePlotContext(
         defaultDir.getPath(),
         new SimpleRequestCallback<SavePlotAsImageContext>() {

            @Override
            public void onResponseReceived(SavePlotAsImageContext context)
            {
               indicator.onCompleted();

               exportPlot_.savePlotAsImage(globalDisplay_,
                     server_, 
                     context, 
                     ExportPlotOptions.adaptToSize(
                           uiPrefs_.get().exportPlotOptions().getValue(),
                           getPlotSize()),
                     saveExportOptionsOperation_);  
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }           
         });
   }
   
   void onSavePlotAsPdf()
   {
      view_.bringToFront();
      
      final ProgressIndicator indicator = 
         globalDisplay_.getProgressIndicator("Error");
      indicator.onProgress("Preparing to export plot...");

      // get the default directory
      final FileSystemItem defaultDir = ExportPlot.getDefaultSaveDirectory(
            workbenchContext_.getCurrentWorkingDir());

      // get context
      server_.getUniqueSavePlotStem(
         defaultDir.getPath(),
         new SimpleRequestCallback<String>() {

            @Override
            public void onResponseReceived(String stem)
            {
               indicator.onCompleted();

               Size size = getPlotSize();
               final SavePlotAsPdfOptions currentOptions = 
                   SavePlotAsPdfOptions.adaptToSize(
                         uiPrefs_.get().savePlotAsPdfOptions().getValue(),
                         pixelsToInches(size.width),
                         pixelsToInches(size.height));
               
               
               exportPlot_.savePlotAsPdf(
                 globalDisplay_,
                 server_, 
                 session_.getSessionInfo(),
                 defaultDir,
                 stem,
                 currentOptions, 
                 new OperationWithInput<SavePlotAsPdfOptions>() {
                    @Override
                    public void execute(SavePlotAsPdfOptions options)
                    {
                       if (!SavePlotAsPdfOptions.areEqual(
                                                options,
                                                currentOptions))
                       {
                          UIPrefs prefs = uiPrefs_.get();
                          prefs.savePlotAsPdfOptions().setGlobalValue(options);
                          prefs.writeUIPrefs();    
                       }
                    }    
                 }) ;  
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }           
         });
   }
      
   
   
   void onCopyPlotToClipboard()
   {
      view_.bringToFront();
      
      exportPlot_.copyPlotToClipboard(
                              server_, 
                              ExportPlotOptions.adaptToSize(
                                    uiPrefs_.get().exportPlotOptions().getValue(),
                                    getPlotSize()),
                              saveExportOptionsOperation_);    
   }
   
   private double pixelsToInches(int pixels)
   {
      return (double)pixels / 96.0;
   }
   
   private OperationWithInput<ExportPlotOptions> saveExportOptionsOperation_ =
      new OperationWithInput<ExportPlotOptions>() 
      {
         public void execute(ExportPlotOptions options)
         {
            UIPrefs uiPrefs = uiPrefs_.get();
            if (!ExportPlotOptions.areEqual(
                            options,
                            uiPrefs.exportPlotOptions().getValue()))
            {
               uiPrefs.exportPlotOptions().setGlobalValue(options);
               uiPrefs.writeUIPrefs();
            }
         }
      };
   
   
   void onZoomPlot()
   {
      int width, height;
      if (zoomWindowDefaultSize_ != null)
      {
         // trim based on available screen size
         NativeScreen screen = NativeScreen.get();
         width = Math.min(screen.getAvailWidth(), 
                          zoomWindowDefaultSize_.width);
         height = Math.min(screen.getAvailHeight(), 
                           zoomWindowDefaultSize_.height); 
      }
      else
      {
         final int PADDING = 20;
   
         Size currentPlotSize = view_.getPlotFrameSize();
   
         // calculate ideal heigh and width. try to be as large as possible
         // within the bounds of the current client size
         Size bounds = new Size(Window.getClientWidth() - PADDING,
                                Window.getClientHeight() - PADDING);
   
         float widthRatio = bounds.width / ((float)currentPlotSize.width);
         float heightRatio = bounds.height / ((float)currentPlotSize.height);
         float ratio = Math.min(widthRatio, heightRatio);
   
         // constrain initial width to between 300 and 1,200 pixels
         width = Math.max(300, (int) (ratio * currentPlotSize.width));
         width = Math.min(1200, width);
         
         // constrain initial height to between 300 and 900 pixels
         height = Math.max(300, (int) (ratio * currentPlotSize.height));
         height = Math.min(900, height);
      }
      
      
      // determine whether we should scale (see comment in ImageFrame.onLoad
      // for why we wouldn't want to scale)
      int scale = 1;
      if (Desktop.isDesktop() && BrowseCap.isMacintosh())
         scale = 0;
      
      // compose url string
      String url = server_.getGraphicsUrl("plot_zoom?" +
                                          "width=" + width + "&" +
                                          "height=" + height + "&" +
                                          "scale=" + scale);


      // open and activate window
      NewWindowOptions options = new NewWindowOptions();
      options.setName("_rstudio_zoom");
      options.setFocus(true);
      options.setCallback(new OperationWithInput<WindowEx>() {
         @Override
         public void execute(WindowEx input)
         {
            zoomWindow_ = input;
         }
      });
      globalDisplay_.openMinimalWindow(url,
                                       false,
                                       width,
                                       height,
                                       options);
   }

   void onRefreshPlot()
   {
      view_.bringToFront();
      view_.setProgress(true);
      server_.refreshPlot(new PlotRequestCallback());
   }
   
   @Override
   public void onDeferredInitCompleted(DeferredInitCompletedEvent event)
   {
      boolean showErrors = !workbenchContext_.isRestartInProgress();
      server_.refreshPlot(new PlotRequestCallback(showErrors));
   }
   
   void onShowManipulator()
   {
      manipulatorManager_.showManipulator();
   }

   public Display getView()
   {
      return view_;
   }
   
   private void safeClearLocator()
   {
      if (locator_.isActive())
      {
         server_.locatorCompleted(null, new SimpleRequestCallback<Void>() {
            @Override
            public void onError(ServerError error)
            {
               // ignore errors (this method is meant to be used "quietly"
               // so that if the server has a problem with clearing
               // locator state (e.g. because it has already exited the
               // locator state) we don't bother the user with it. worst
               // case if this fails then the user will see that the console
               // is still pending the locator command and the Done and Esc
               // gestures will still be available to clear the Locator
            }
         });
      }
   }
   
   private void setChangePlotProgress()
   {
      if (!Desktop.isDesktop())
         view_.setProgress(true);
   }
   
   private class PlotRequestCallback extends ServerRequestCallback<Void>
   {
      public PlotRequestCallback()
      {
         this(true);
      }
      
      public PlotRequestCallback(boolean showErrors)
      {
         showErrors_ = showErrors;
      }
      
      @Override
      public void onResponseReceived(Void response)
      {
         // we don't clear the progress until the GraphicsOutput
         // event is received (enables us to wait for rendering
         // to complete before clearing progress)
      }

      @Override
      public void onError(ServerError error)
      {
         view_.setProgress(false);
         
         if (showErrors_)
         {
            globalDisplay_.showErrorMessage("Server Error", 
                                            error.getUserMessage());
         }
      }
      
      private final boolean showErrors_;
   }

   public void onLocator(LocatorEvent event)
   {
      view_.bringToFront();
      locate();
   }

   private void locate()
   {
      locator_.locate(view_.getPlotUrl(), getPlotSize());
   }

   public void onConsolePrompt(ConsolePromptEvent event)
   {
      locator_.clearDisplay();
   }
   
   @Override
   public void onPlotsZoomSizeChanged(PlotsZoomSizeChangedEvent event)
   {
      zoomWindowDefaultSize_ = new Size(event.getWidth(), event.getHeight());
   }
   
   private Size getPlotSize()
   {
      // NOTE: the reason we capture the plotSize_ from the PlotChangedEvent
      // is that the server can actually change the size of the plot
      // (e.g. for CairoSVG the width and height must be multiples of 4)
      // in order for locator to work properly we need to use this size 
      // rather than size of our current plot frame
      
      if (plotSize_ != null) // first try to use the last size reported
         return plotSize_ ;
      else                   // then fallback to frame size
         return view_.getPlotFrameSize();
   }
   
   private class ManipulatorRequestCallback extends ServerRequestCallback<Void>
   {
      public ManipulatorRequestCallback()
      {
         manipulatorManager_.setProgress(true);
      }
      
      @Override
      public void onResponseReceived(Void response)
      {
         // we don't clear the progress until the GraphicsOutput
         // event is received (enables us to wait for rendering
         // to complete before clearing progress)
      }

      @Override
      public void onError(ServerError error)
      {
         manipulatorManager_.setProgress(false);
         globalDisplay_.showErrorMessage("Server Error", 
                                         error.getUserMessage());
         
      }
      
   }

   private final Display view_;
   private final GlobalDisplay globalDisplay_;
   private final PlotsServerOperations server_;
   private final WorkbenchContext workbenchContext_;
   private final Session session_;
   private final Provider<UIPrefs> uiPrefs_;
   private final Locator locator_;
   private final ManipulatorManager manipulatorManager_;
   private WindowEx zoomWindow_;
   private Size zoomWindowDefaultSize_;
   
   // export plot impl
   private final ExportPlot exportPlot_ ;
   
   // size of most recently rendered plot
   Size plotSize_ = null;
}
