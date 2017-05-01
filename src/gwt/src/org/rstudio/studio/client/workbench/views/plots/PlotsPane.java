/*
 * PlotsPane.java
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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.widget.ImageFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.zoom.ZoomUtils;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.ui.PlotsToolbar;

import java.util.Iterator;

public class PlotsPane extends WorkbenchPane implements Plots.Display,
      HasResizeHandlers
{
   @Inject
   public PlotsPane(Commands commands, PlotsServerOperations server,
         DependencyManager dependencies)
   {
      super("Plots");
      commands_ = commands;
      server_ = server;
      dependencies_ = dependencies;
      ensureWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      publishButton_ = new RSConnectPublishButton(
            RSConnect.CONTENT_TYPE_PLOT, true, commands_.savePlotAsImage());
      publishButton_.setPublishHtmlSource(new PublishHtmlSource()
      {
         @Override
         public String getTitle()
         {
            return "Current Plot";
         }
         
         @Override
         public void generatePublishHtml(
               final CommandWithArg<String> onComplete)
         {
            dependencies_.withRMarkdown("Publishing plots", new Command()
            {
               @Override
               public void execute()
               {
                 final Size size = ZoomUtils.getZoomedSize(
                       getPlotFrameSize(), 
                       new Size(400, 350), 
                       new Size(800, 700));
                  server_.plotsCreateRPubsHtml(
                     "Plot", 
                     "", 
                     size.width,
                     size.height,
                     new SimpleRequestCallback<String>() 
                     {
                        @Override
                        public void onResponseReceived(String rpubsHtmlFile)
                        {
                           onComplete.execute(rpubsHtmlFile);
                        }
                     });
                 }
             });
         }
      });
      plotsToolbar_ = new PlotsToolbar(commands_, publishButton_);
      return plotsToolbar_;
   }

   @Override
   protected Widget createMainWidget()
   {
      panel_ = new LayoutPanel();
      panel_.addStyleName("ace_editor_theme");
      panel_.setSize("100%", "100%");

      frame_ = new ImageFrame();
      frame_.setStyleName("rstudio-HelpFrame");
      frame_.setMarginWidth(0);
      frame_.setMarginHeight(0);
      frame_.setUrl("about:blank");
      frame_.setSize("100%", "100%");
      ElementIds.assignElementId(frame_.getElement(), 
                                 ElementIds.PLOT_IMAGE_FRAME);

      panel_.add(frame_);
      panel_.setWidgetTopBottom(frame_, 0, Unit.PX, 0, Unit.PX);
      panel_.setWidgetLeftRight(frame_, 0, Unit.PX, 0, Unit.PX);

      // Stops mouse events from being routed to the iframe, which would
      // interfere with dragging the workbench pane sizer. also provide
      // a widget container where adornments can be added on top fo the
      // plots panel (e.g. manipulator button)
      plotsSurface_ = new FlowPanel();
      plotsSurface_.setSize("100%", "100%");
      panel_.add(plotsSurface_);
      panel_.setWidgetTopBottom(plotsSurface_, 0, Unit.PX, 0, Unit.PX);
      panel_.setWidgetLeftRight(plotsSurface_, 0, Unit.PX, 0, Unit.PX);
      
      // return the panel
      return panel_;
   }

   @Override
   public void setProgress(boolean enabled)
   {
      // also set frame to about:blank during progress
      if (enabled)
         frame_.setImageUrl(null);

      super.setProgress(enabled);
   }

   public void showEmptyPlot()
   {
      frame_.setImageUrl(null);
      plotsToolbar_.invalidateSeparators();
   }

   public void showPlot(String plotUrl)
   {
      // save plot url for refresh
      plotUrl_ = plotUrl;

      // use frame.contentWindow.location.replace to avoid having the plot
      // enter the browser's history
      frame_.setImageUrl(plotUrl);
      plotsToolbar_.invalidateSeparators();
   }
       
   public String getPlotUrl()
   {
      return plotUrl_;
   }
   

   public void refresh()
   {
      if (plotUrl_ != null)
         frame_.setImageUrl(plotUrl_);
   }

   public Panel getPlotsSurface()
   {
      return plotsSurface_;
   }
   
   public Plots.Parent getPlotsParent()
   {
      return plotsParent_;    
   }
   
   public Size getPlotFrameSize()
   {
      return new Size(frame_.getOffsetWidth(), frame_.getOffsetHeight());
   }

   @Override
   public void onResize()
   {
      super.onResize();
      int width = getOffsetWidth();
      ResizeEvent.fire(this, width, getOffsetHeight());
      
      if (width > 0 && publishButton_ != null)
      {
         publishButton_.setShowCaption(width > 500);
      }
   }

   public HandlerRegistration addResizeHandler(ResizeHandler resizeHandler)
   {
      return addHandler(resizeHandler, ResizeEvent.getType());
   }

   private final Commands commands_;
   private final PlotsServerOperations server_;
   private final DependencyManager dependencies_;
   
   private RSConnectPublishButton publishButton_;
   private LayoutPanel panel_;
   private ImageFrame frame_;
   private String plotUrl_;
   private PlotsToolbar plotsToolbar_ = null;
   private FlowPanel plotsSurface_ = null;
   private Plots.Parent plotsParent_ = new Plots.Parent() { 
      public void add(Widget w)
      {
         panel_.add(w);   
      }
      
      public boolean remove(Widget w)
      {
         return panel_.remove(w);
      }
      
      public Iterator<Widget> iterator()
      {
         return panel_.iterator();
      }

      public void clear()
      {
         panel_.clear();
      }
      
      public void installCustomToolbar(Customizer customizer)
      {
         plotsToolbar_.installCustomToolbar(customizer);       
      }
      
      public void removeCustomToolbar()
      {
         plotsToolbar_.removeCustomToolbar();        
      }
   };
   
}
