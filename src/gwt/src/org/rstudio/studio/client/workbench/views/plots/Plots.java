/*
 * Plots.java
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
package org.rstudio.studio.client.workbench.views.plots;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.Size;
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
import org.rstudio.studio.client.common.zoom.ZoomUtils;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotUtils;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.plots.events.LocatorEvent;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsZoomSizeChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsState;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlot;
import org.rstudio.studio.client.workbench.views.plots.ui.manipulator.ManipulatorChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.ui.manipulator.ManipulatorManager;

public class Plots extends BasePresenter implements PlotsChangedEvent.Handler,
                                                    LocatorEvent.Handler,
                                                    ConsolePromptEvent.Handler,
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

      PlotsSurface getPlotsSurface();

      Parent getPlotsParent();
      Size getPlotFrameSize();
   }


   @Inject
   public Plots(final Display view,
                GlobalDisplay globalDisplay,
                WorkbenchContext workbenchContext,
                Provider<UserState> userState,
                Commands commands,
                EventBus events,
                final PlotsServerOperations server,
                Session session)
   {
      super(view);
      view_ = view;
      globalDisplay_ = globalDisplay;
      workbenchContext_ = workbenchContext;
      userState_ = userState;
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
            {
               p = org.rstudio.studio.client.workbench.views.plots.model.Point.create(
                     e.getSelectedItem().getX(),
                     e.getSelectedItem().getY()
               );

               // re-scale points for OS X Quartz
               if (BrowseCap.isMacintoshDesktop())
               {
                  p = org.rstudio.studio.client.workbench.views.plots.model.Point.create(
                        p.getX() * (72.0 / 96.0),
                        p.getY() * (72.0 / 96.0));
               }

            }

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
               int x = Double.valueOf(event.getX()).intValue();
               int y = Double.valueOf(event.getY()).intValue();

               // Re-scale for OSX Quartz
               if (BrowseCap.isMacintoshDesktop())
               {
                  x *= (72.0 / 96.0);
                  y *= (72.0 / 96.0);
               }

               server_.manipulatorPlotClicked(x, y, new ManipulatorRequestCallback());
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
      if (Desktop.hasDesktopFrame())
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
      FileSystemItem defaultDir = ExportPlotUtils.getDefaultSaveDirectory(
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
                           userState_.get().exportPlotOptions().getValue().cast(),
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
      final FileSystemItem defaultDir = ExportPlotUtils.getDefaultSaveDirectory(
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
                         userState_.get().savePlotAsPdfOptions().getValue().cast();

               exportPlot_.savePlotAsPdf(
                 globalDisplay_,
                 server_,
                 session_.getSessionInfo(),
                 defaultDir,
                 stem,
                 currentOptions,
                 pixelsToInches(size.width),
                 pixelsToInches(size.height),
                 new OperationWithInput<SavePlotAsPdfOptions>() {
                    @Override
                    public void execute(SavePlotAsPdfOptions options)
                    {
                       if (!SavePlotAsPdfOptions.areEqual(
                                                options,
                                                currentOptions))
                       {
                          UserState state = userState_.get();
                          state.savePlotAsPdfOptions().setGlobalValue(options);
                          state.writeState();
                       }
                    }
                 });
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
                                    userState_.get().exportPlotOptions().getValue().cast(),
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
            UserState userState = userState_.get();
            if (!ExportPlotOptions.areEqual(
                            options,
                            userState.exportPlotOptions().getValue().cast()))
            {
               userState.exportPlotOptions().setGlobalValue(options);
               userState.writeState();
            }
         }
      };


   void onZoomPlot()
   {
      Size windowSize = ZoomUtils.getZoomWindowSize(
                              view_.getPlotFrameSize(), zoomWindowDefaultSize_);

      // determine whether we should scale (see comment in ImageFrame.onLoad
      // for why we wouldn't want to scale)
      int scale = 1;
      if (Desktop.hasDesktopFrame() && BrowseCap.isMacintosh())
         scale = 0;

      // compose url string
      String url = server_.getGraphicsUrl("plot_zoom?" +
                                          "width=" + windowSize.width + "&" +
                                          "height=" + windowSize.height + "&" +
                                          "scale=" + scale);

      // open the window
      ZoomUtils.openZoomWindow(
         "_rstudio_zoom",
         url,
         windowSize,
         new OperationWithInput<WindowEx>() {
            @Override
            public void execute(WindowEx input)
            {
               zoomWindow_ = input;
            }
         }
      );
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
      server_.refreshPlot(new PlotRequestCallback(false));
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
         return plotSize_;
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
   private final Provider<UserState> userState_;
   private final Locator locator_;
   private final ManipulatorManager manipulatorManager_;
   private WindowEx zoomWindow_;
   private Size zoomWindowDefaultSize_;

   // export plot impl
   private final ExportPlot exportPlot_;

   // size of most recently rendered plot
   Size plotSize_ = null;
}
