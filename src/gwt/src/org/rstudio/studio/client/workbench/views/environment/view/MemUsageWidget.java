/*
 * MemUsageWidget.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import org.rstudio.core.client.widget.MiniPieWidget;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.core.client.widget.UserPrefMenuItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsage;

public class MemUsageWidget extends Composite
{
   public MemUsageWidget(MemoryUsage usage, UserPrefs prefs)
   {
      prefs_ = prefs;

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

      ToolbarPopupMenu memoryMenu = new ToolbarPopupMenu();
      memoryMenu.addItem(RStudioGinjector.INSTANCE.getCommands().freeUnusedMemory().createMenuItem(false));
      memoryMenu.addItem(RStudioGinjector.INSTANCE.getCommands().showMemoryUsageReport().createMenuItem(false));
      memoryMenu.addSeparator();
      memoryMenu.addItem(new UserPrefMenuItem<Boolean>(
         prefs_.showMemoryUsage(),
         true,
         "Show Current Memory Usage",
         prefs_
      ));

      menu_ = new ToolbarMenuButton(
         "Memory",
         ToolbarButton.NoTitle,
         (ImageResource) null,
         memoryMenu);
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

   public void setMemoryUsage(MemoryUsage usage)
   {
      if (usage == null)
      {
         pieCrust_.getElement().removeAllChildren();
         menu_.setText("Memory");
      }
      else
      {
         menu_.setTitle("Memory used by R session");
         menu_.setText(formatBigMemory(usage.getProcess().getKb()));

         // For browser SVG painting reasons, it is necessary to create a wholly
         // new SVG element and then replay it as HTML into the DOM to get it to 
         // draw correctly.
         MiniPieWidget pie = new MiniPieWidget(
            "Memory in use: " + usage.getPercentUsed() + "% of " +
               formatBigMemory(usage.getTotal().getKb()) + 
               " (source: " + usage.getTotal().getProviderName() + ")",
            "Pie chart depicting the percentage of total memory in use", 
            usage.getColorCode(),
            MEMORY_PIE_UNUSED_COLOR,
            usage.getPercentUsed());
         pieCrust_.getElement().removeAllChildren();
         pieCrust_.add(pie);
         pieCrust_.getElement().setInnerHTML(pieCrust_.getElement().getInnerHTML());
      }
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

   private final UserPrefs prefs_;
   private final HTMLPanel pieCrust_;
   private final ToolbarMenuButton menu_;
   private final HTMLPanel host_;

   public static final String MEMORY_PIE_UNUSED_COLOR = "#e4e4e4";
}
