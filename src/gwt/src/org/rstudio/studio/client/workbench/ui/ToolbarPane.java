/*
 * ToolbarPane.java
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
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.AutoGlassPanel;

public abstract class ToolbarPane extends LazyPanel implements RequiresResize,
                                                               HasEnsureVisibleHandlers,
                                                               HasEnsureHeightHandlers
{
   public Widget asWidget()
   {
      ensureWidget();
      return this;
   }

   public void setProgress(boolean progress)
   {
      ensureWidget();

      if (progress)
      {
         progressPanel_.showProgress(progressDelayMs_);
      }
      else
      {
         progressPanel_.setWidget(mainWidget_);
         dockPanel_.forceLayout();
      }
   }

   @Override
   protected Widget createWidget()
   {
      dockPanel_ = new DockLayoutPanel(Style.Unit.PX);
      dockPanel_.setSize("100%", "100%");

      mainToolbar_ = createMainToolbar();
      if (mainToolbar_ != null)
         addToolbar(mainToolbar_);

      secondaryToolbar_ = createSecondaryToolbar();
      if (secondaryToolbar_ !=null)
         addToolbar(secondaryToolbar_);

      mainWidget_ = createMainWidget();
      mainWidget_.setSize("100%", "100%");

      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setSize("100%", "100%");
      progressPanel_.setWidget(mainWidget_);

      dockPanel_.add(progressPanel_);

      AutoGlassPanel glassPanel = new AutoGlassPanel(dockPanel_);
      glassPanel.setSize("100%", "100%");
      return glassPanel;
   }

   public void onResize()
   {
      Widget child = getWidget();
      if (child != null && child instanceof RequiresResize)
      {
         // there are DOM states where the child onResize throws
         // an exception (related to windows splitter animation)
         // so we muffle exceptions here
         try
         {
            ((RequiresResize) child).onResize();
         }
         catch(Exception ex)
         {
         }
      }
   }

   protected abstract Widget createMainWidget();

   protected Toolbar createMainToolbar()
   {
      return null;
   }

   protected SecondaryToolbar createSecondaryToolbar()
   {
      return null;
   }

   public int getToolbarsHeight()
   {
      return (mainToolbar_ != null ? mainToolbar_.getOffsetHeight() : 0)
            + (secondaryToolbar_ != null ? secondaryToolbar_.getOffsetHeight() : 0);
   }

   protected void setProgressDelay(int delayMs)
   {
      progressDelayMs_ = delayMs;
   }

   private void addToolbar(Toolbar toolbar)
   {
      dockPanel_.addNorth(toolbar, toolbar.getHeight());
   }

   public void bringToFront()
   {
      ensureWidget();
      fireEvent(new EnsureVisibleEvent());
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public HandlerRegistration addEnsureHiddenHandler(EnsureHiddenEvent.Handler handler)
   {
      return addHandler(handler, EnsureHiddenEvent.TYPE);
   }

   public HandlerRegistration addEnsureHeightHandler(EnsureHeightEvent.Handler handler)
   {
      return addHandler(handler, EnsureHeightEvent.TYPE);
   }

   public void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent());
   }

   public void ensureHidden()
   {
      fireEvent(new EnsureHiddenEvent());
   }

   public void ensureHeight(int height)
   {
      fireEvent(new EnsureHeightEvent(height));
   }

   public void maximize()
   {
      fireEvent(new EnsureHeightEvent(EnsureHeightEvent.MAXIMIZED));
   }

   public boolean isMainToolbarVisible()
   {
      return mainToolbar_.isVisible();
   }

   public void setMainToolbarVisible(boolean visible)
   {
      setToolbarVisibility(visible, mainToolbar_);
   }

   public void setSecondaryToolbarVisible(boolean visible)
   {
      if (secondaryToolbar_ != null)
         setToolbarVisibility(visible, secondaryToolbar_);
   }

   private void setToolbarVisibility(final boolean visible,
                                     final Toolbar toolbar)
   {
      if (visible == toolbar.isVisible())
         return;

      toolbar.setVisible(visible);
      dockPanel_.setWidgetSize(toolbar, visible ? toolbar.getHeight()
                                                : 0);
   }

   private DockLayoutPanel dockPanel_;
   protected Toolbar mainToolbar_;
   protected Toolbar secondaryToolbar_;
   private Widget mainWidget_;
   private SimplePanelWithProgress progressPanel_;
   private int progressDelayMs_ = 200;
}
