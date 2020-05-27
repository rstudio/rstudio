/*
 * PanelWithToolbars.java
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.IsWidgetWithHeight;

public class PanelWithToolbars extends ResizeComposite
{
   public PanelWithToolbars(Toolbar toolbar, Widget mainWidget)
   {
      this(toolbar, mainWidget, null);
   }

   public PanelWithToolbars(Toolbar toolbar,
                            Widget mainWidget,
                            IsWidgetWithHeight statusBar)
   {
      this(toolbar, null, mainWidget, statusBar);
   }
   
   public PanelWithToolbars(Toolbar toolbar,
                            Toolbar secondaryToolbar,
                            Widget mainWidget,
                            IsWidgetWithHeight statusBar)
   {
      mainWidget_ = mainWidget;

      panel_ = new DockLayoutPanel(Unit.PX);
      panel_.addNorth(toolbar, toolbar.getHeight());
      
      if (secondaryToolbar != null) {
         panel_.addNorth(secondaryToolbar, secondaryToolbar.getHeight());
         secondaryToolbar.getWrapper().addStyleName(ThemeStyles.INSTANCE.tallerToolbarWrapper());
      }
      
      if (statusBar != null) {
         statusBarWidget_ = statusBar.asWidget();
         panel_.addSouth(statusBarWidget_, statusBar.getHeight());
      }

      panel_.add(mainWidget_);

      initWidget(panel_);
   }
   
   public void setMainWidget(Widget mainWidget)
   {
      panel_.remove(mainWidget_);
      panel_.add(mainWidget);
      mainWidget_ = mainWidget;
   }
   
   public void showStatusBar(boolean show)
   {
      if (statusBarWidget_ != null)
         panel_.setWidgetHidden(statusBarWidget_, !show);
   }
   

   public void insertNorth(Widget widget, double size, Widget before) 
   {
      if (before == null)
         before = mainWidget_;
      panel_.insertNorth(widget, size, before);
      panel_.forceLayout();
   }

   public void remove(Widget widget)
   {
      panel_.remove(widget);
      panel_.forceLayout();
   }

   private DockLayoutPanel panel_;
   private Widget mainWidget_;
   private Widget statusBarWidget_ = null;
}
