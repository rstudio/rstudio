/*
 * MemUsageWidget.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MiniPieWidget;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.core.client.widget.UserPrefMenuItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.environment.ViewEnvironmentConstants;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsage;

public class MemUsageWidget extends Composite
{
   public MemUsageWidget(MemoryUsage usage, UserPrefs prefs)
   {
      prefs_ = prefs;
      suspended_ = false;

      host_ = new HTMLPanel("");
      Style style = host_.getElement().getStyle();
      style.setProperty("display", "flex");
      style.setProperty("flexDirection", "row");

      pieCrust_ = new HTMLPanel("");
      pieCrust_.setHeight("13px");
      pieCrust_.setWidth("13px");
      style = pieCrust_.getElement().getStyle();
      style.setMarginTop(3, Style.Unit.PX);
      style.setMarginRight(3, Style.Unit.PX);
      host_.add(pieCrust_);
      ElementIds.assignElementId(pieCrust_, ElementIds.MEMORY_PIE_MINI);

      ToolbarPopupMenu memoryMenu = new ToolbarPopupMenu();
      memoryMenu.addItem(RStudioGinjector.INSTANCE.getCommands().freeUnusedMemory().createMenuItem(false));
      memoryMenu.addItem(RStudioGinjector.INSTANCE.getCommands().showMemoryUsageReport().createMenuItem(false));
      memoryMenu.addSeparator();
      memoryMenu.addItem(new UserPrefMenuItem<Boolean>(
         prefs_.showMemoryUsage(),
         true,
         constants_.showCurrentMemoryUsage(),
         prefs_
      ));

      menu_ = new ToolbarMenuButton(
         constants_.memoryCapitalized(),
         ToolbarButton.NoTitle,
         (ImageResource) null,
         memoryMenu);
      ElementIds.assignElementId(menu_, ElementIds.MEMORY_DROPDOWN);
      menu_.getElement().getStyle().setMarginTop(-3, Style.Unit.PX);
      host_.add(menu_);

      setMemoryUsage(usage);

      prefs.showMemoryUsage().addValueChangeHandler(evt ->
      {
         // Clear memory usage display immediately when turned off
         if (!evt.getValue())
         {
            setMemoryUsage(null);
         }
      });

      initWidget(host_);
   }

   /**
    * Sets the memory usage to be displayed in the widget.
    *
    * @param usage The memory usage to display
    */
   public void setMemoryUsage(MemoryUsage usage)
   {
      usage_ = usage;

      if (usage == null)
      {
         pieCrust_.getElement().removeAllChildren();
         pieCrust_.setVisible(false);
         menu_.setText(constants_.memoryCapitalized());
      }
      else
      {
         // If we were previously showing suspended memory data, then switch to live session view
         if (suspended_)
         {
            setSuspended(false);
         }

         menu_.setTitle(constants_.kiBUsedByRSession(
                 StringUtil.prettyFormatNumber(usage.getProcess().getKb()),
                 usage.getProcess().getProviderName()));
         menu_.setText(formatBigMemory(usage.getProcess().getKb()));

         MemoryUsagePieChart pie = new MemoryUsagePieChart(usage);
         loadPieDisplay(pie);
      }
   }

   /**
    * Load a pie chart into the memory usage widget
    *
    * @param pie The pie chart to load
    */
   private void loadPieDisplay(Widget pie)
   {
      // For browser SVG painting reasons, it is necessary to create a wholly
      // new SVG element and then replay it as HTML into the DOM to get it to
      // draw correctly.
      pieCrust_.setVisible(true);
      pieCrust_.getElement().removeAllChildren();
      pieCrust_.add(pie);
      pieCrust_.getElement().setInnerHTML(pieCrust_.getElement().getInnerHTML());
   }

   /**
    * Formats a large memory statistic for memory display
    *
    * @param kb The amount of memory
    * @return A string describing the amount of memory
    */
   private String formatBigMemory(int kb)
   {
      long mib = kb / 1024;
      if (mib >= 1024)
      {
         // Memory usage is > 1GiB, format as XX.YY GiB
         NumberFormat decimalFormat = NumberFormat.getFormat(".##");
         return decimalFormat.format((double)mib / (double)1024) + " GiB";
      }

      // Memory usage is in MiB
      return mib + " MiB";
   }

   /**
    * Sets whether to show the memory usage as suspended
    *
    * @param suspended
    */
   public void setSuspended(boolean suspended)
   {
      // Ignore if we're already in the desired state
      if (suspended == suspended_)
      {
         return;
      }
      suspended_ = suspended;

      // If the widget isn't showing memory usage information
      if (usage_ == null)
      {
         return;
      }

      if (suspended)
      {
         MiniPieWidget pie = new MiniPieWidget(
            constants_.memoryInUseNone(),
            constants_.emptyPieChartNoMemoryUsage(),
            MEMORY_PIE_UNUSED_COLOR);
         loadPieDisplay(pie);

         menu_.setEnabled(false);
      }
      else
      {
         menu_.setEnabled(true);
      }
   }

   private final UserPrefs prefs_;
   private final HTMLPanel pieCrust_;
   private final ToolbarMenuButton menu_;
   private final HTMLPanel host_;

   private MemoryUsage usage_;
   private boolean suspended_;

   public static final String MEMORY_PIE_UNUSED_COLOR = "#e4e4e4";
   private static final ViewEnvironmentConstants constants_ = GWT.create(ViewEnvironmentConstants.class);
}