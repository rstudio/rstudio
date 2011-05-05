/*
 * Plots.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots;

import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Panel;
import com.google.inject.Inject;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.HasCustomizableToolbar;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptHandler;
import org.rstudio.studio.client.workbench.views.plots.events.LocatorEvent;
import org.rstudio.studio.client.workbench.views.plots.events.LocatorHandler;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.model.ExportOptions;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotExportContext;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsState;
import org.rstudio.studio.client.workbench.views.plots.model.PrintOptions;
import org.rstudio.studio.client.workbench.views.plots.ui.ExportDialog;
import org.rstudio.studio.client.workbench.views.plots.ui.PrintDialog;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlotDialog;
import org.rstudio.studio.client.workbench.views.plots.ui.manipulator.ManipulatorChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.ui.manipulator.ManipulatorManager;

public class Plots extends BasePresenter implements PlotsChangedHandler,
                                                    LocatorHandler,
                                                    ConsolePromptHandler
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
                Commands commands,
                final PlotsServerOperations server,
                FileDialogs fileDialogs,
                RemoteFileSystemContext fileSystemContext,
                Session session)
   {
      super(view);
      view_ = view;
      globalDisplay_ = globalDisplay;
      server_ = server;
      fileDialogs_ = fileDialogs;
      fileSystemContext_ = fileSystemContext;
      session_ = session;
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
               // show progress
               manipulatorManager_.setProgress(true);
               
               // set values
               server_.setManipulatorValues(values, 
                                            new ServerRequestCallback<Void>() {
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
               });
            }
         });
      
      
      new JSObjectStateValue("plotspane", "exportOptions", true,
                             session.getSessionInfo().getClientState(), false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            if (value != null)
               exportOptions_ = value.cast();
            lastKnownState_ = exportOptions_;
         }

         @Override
         protected JsObject getValue()
         {
            return exportOptions_.cast();
         }

         @Override
         protected boolean hasChanged()
         {
            if (!ExportOptions.areEqual(lastKnownState_, exportOptions_))
            {
               lastKnownState_ = exportOptions_;
               return true;
            }

            return false;
         }

         private ExportOptions lastKnownState_;
      };
      
      new JSObjectStateValue("plotspane", "exportPlotOptions", true,
            session.getSessionInfo().getClientState(), false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            if (value != null)
               exportPlotOptions_ = value.cast();
            lastKnownState_ = exportPlotOptions_;
         }

         @Override
         protected JsObject getValue()
         {
            return exportPlotOptions_.cast();
         }

         @Override
         protected boolean hasChanged()
         {
            if (!ExportPlotOptions.areEqual(lastKnownState_, 
                                            exportPlotOptions_))
            {
               lastKnownState_ = exportPlotOptions_;
               return true;
            }

            return false;
         }

         private ExportPlotOptions lastKnownState_;
      };
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

      // activate plots tab if requested
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

   void onPrintPlot()
   {
      view_.bringToFront();
      new PrintDialog(new OperationWithInput<PrintOptions>() {
         public void execute(PrintOptions options)
         {
            // build print url
            String printURL = server_.getGraphicsUrl("plot.pdf");
            printURL += "?";
            printURL += "width=" + options.getWidth();
            printURL += "&";
            printURL += "height=" + options.getHeight();
            
            // open new window with "printed" pdf
            globalDisplay_.openWindow(printURL);
         }
      }).showModal();
     
   }
   
   void onExportPlotAsImage()
   {
      view_.bringToFront();
      
      // show the dialog 
      new ExportDialog(
            server_,
            exportOptions_,
            new OperationWithInput<ExportOptions>() {
               public void execute(ExportOptions input)
               {
                 // update default options
                 exportOptions_ = input;
                 session_.persistClientState(); 
               }
      }).showModal();
   }
   
   void onExportPlot()
   {
      view_.bringToFront();
      
      final ProgressIndicator indicator = 
                                 globalDisplay_.getProgressIndicator("Error");
      indicator.onProgress("Preparing to export plot...");

      server_.getPlotExportContext(
         new SimpleRequestCallback<PlotExportContext>() {

            @Override
            public void onResponseReceived(PlotExportContext context)
            {
               indicator.onCompleted();
               
               new ExportPlotDialog(
                     server_, 
                     fileDialogs_,
                     fileSystemContext_,
                     context,
                     exportPlotOptions_,  
                     new OperationWithInput<ExportPlotOptions>() 
                     {
                        public void execute(ExportPlotOptions options)
                        {
                           // update default options
                           exportPlotOptions_ = options;
                           session_.persistClientState();
                        }
                     }).showModal();
               
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }           
         });     
   }
   
   void onZoomPlot()
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

      int width = Math.max(300, (int) (ratio * currentPlotSize.width));
      int height = Math.max(300, (int) (ratio * currentPlotSize.height));

      // compose url string
      String url = server_.getGraphicsUrl("plot_zoom?" +
                                          "width=" + width + "&" +
                                          "height=" + height);


      // open and activate window
      globalDisplay_.openMinimalWindow(url,
                                       false,
                                       width,
                                       height,
                                       "_rstudio_zoom",
                                       true);
   }

   void onRefreshPlot()
   {
      view_.bringToFront();
      view_.setProgress(true);
      server_.refreshPlot(new PlotRequestCallback());
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
         globalDisplay_.showErrorMessage("Server Error", 
                                         error.getUserMessage());
      }
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

   private final Display view_;
   private final GlobalDisplay globalDisplay_;
   private final PlotsServerOperations server_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileSystemContext_;
   private final Session session_;
   private final Locator locator_;
   private final ManipulatorManager manipulatorManager_;
   
   // default export options
   private ExportOptions exportOptions_ = ExportOptions.create(
                                                   ExportOptions.PNG_TYPE,
                                                   500, 
                                                   400);
   
   // default export options
   private ExportPlotOptions exportPlotOptions_ = ExportPlotOptions.create(
                                                   "PNG",
                                                   550, 
                                                   450,
                                                   false,
                                                   false);
   
   // size of most recently rendered plot
   Size plotSize_ = null;
}
