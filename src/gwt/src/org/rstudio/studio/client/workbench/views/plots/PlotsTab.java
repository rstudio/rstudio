/*
 * PlotsTab.java
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

import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.plots.events.LocatorEvent;
import org.rstudio.studio.client.workbench.views.plots.events.PlotsChangedEvent;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsState;

public class PlotsTab extends DelayLoadWorkbenchTab<Plots>
   implements HasResizeHandlers, PlotsChangedEvent.Handler
{
   public interface Binder extends CommandBinder<Commands, PlotsShim> {}

   public abstract static class PlotsShim extends DelayLoadTabShim<Plots, PlotsTab>
         implements PlotsChangedEvent.Handler, LocatorEvent.Handler, ConsolePromptEvent.Handler
   {
      boolean loaded = false;
      PlotsState delayLoadPlotsState;

      @Handler
      public abstract void onNextPlot();
      @Handler
      public abstract void onPreviousPlot();
      @Handler
      public abstract void onRemovePlot();
      @Handler
      public abstract void onClearPlots();
      @Handler
      public abstract void onZoomPlot();
      @Handler
      public abstract void onSavePlotAsImage();
      @Handler
      public abstract void onSavePlotAsPdf();
      @Handler
      public abstract void onCopyPlotToClipboard();
      @Handler
      public abstract void onRefreshPlot();
      @Handler
      public abstract void onShowManipulator();

      @Override
      protected void onDelayLoadSuccess(Plots plots)
      {
         loaded = true;
         super.onDelayLoadSuccess(plots);

         Widget child = plots.asWidget();
         ((HasResizeHandlers)child).addResizeHandler(new ResizeHandler()
         {
            public void onResize(ResizeEvent event)
            {
               ResizeEvent.fire(getParentTab(),
                                event.getWidth(),
                                event.getHeight());
            }
         });

         if (delayLoadPlotsState != null)
         {
            plots.onPlotsChanged(new PlotsChangedEvent(delayLoadPlotsState));
            delayLoadPlotsState = null;
         }
      }
   }

   @Inject
   public PlotsTab(PlotsShim shim,
                   EventBus events,
                   Binder binder,
                   Commands commands,
                   PlotsServerOperations server)
   {
      super("Plots", shim);
      binder.bind(commands, shim);
      commands_ = commands;
      shim_ = shim;
      events.addHandler(PlotsChangedEvent.TYPE, this);
      events.addHandler(LocatorEvent.TYPE, shim);
      events.addHandler(ConsolePromptEvent.TYPE, shim);

      // disable all commands
      commands_.nextPlot().setEnabled(false);
      commands_.previousPlot().setEnabled(false);
      commands_.savePlotAsImage().setEnabled(false);
      commands_.savePlotAsPdf().setEnabled(false);
      commands_.copyPlotToClipboard().setEnabled(false);
      commands_.zoomPlot().setEnabled(false);
      commands_.removePlot().setEnabled(false);
      commands_.clearPlots().setEnabled(false);
      commands_.refreshPlot().setEnabled(false);
      commands_.showManipulator().setEnabled(false);
   }

   public HandlerRegistration addResizeHandler(ResizeHandler handler)
   {
      return handlers_.addHandler(ResizeEvent.getType(), handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   public void onPlotsChanged(PlotsChangedEvent event)
   {
      shim_.delayLoadPlotsState = null;

      PlotsState plotsState = event.getPlotsState();

      // zero or one plot -- next and previous disabled
      if (plotsState.getPlotCount() <= 1)
      {
         commands_.nextPlot().setEnabled(false);
         commands_.previousPlot().setEnabled(false);
      }
      // first plot (only next is enabled)
      else if (plotsState.getPlotIndex() == 0)
      {
         commands_.nextPlot().setEnabled(true);
         commands_.previousPlot().setEnabled(false);
      }
      // last plot (only back is enabled)
      else if (plotsState.getPlotIndex() ==
              (plotsState.getPlotCount() - 1))
      {
         commands_.nextPlot().setEnabled(false);
         commands_.previousPlot().setEnabled(true);
      }
      // both enabled
      else
      {
         commands_.nextPlot().setEnabled(true);
         commands_.previousPlot().setEnabled(true);
      }

      // other commands which are only enabled if there is at least
      // one plot alive
      boolean hasPlots = plotsState.getPlotCount() >= 1;
      commands_.savePlotAsImage().setEnabled(hasPlots);
      commands_.savePlotAsPdf().setEnabled(hasPlots);
      commands_.copyPlotToClipboard().setEnabled(hasPlots);
      commands_.zoomPlot().setEnabled(hasPlots);
      commands_.removePlot().setEnabled(hasPlots);
      commands_.clearPlots().setEnabled(hasPlots);
      commands_.refreshPlot().setEnabled(hasPlots);
      commands_.showManipulator().setEnabled(hasPlots);

      if (plotsState.getActivatePlots() || shim_.loaded)
      {
         shim_.onPlotsChanged(event);
      }
      else
      {
         shim_.delayLoadPlotsState = plotsState;
      }
   }

   private final HandlerManager handlers_ = new HandlerManager(this);
   private final PlotsShim shim_;
   private Commands commands_;
}
