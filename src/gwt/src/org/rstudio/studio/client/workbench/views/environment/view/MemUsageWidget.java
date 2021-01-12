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
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import org.rstudio.core.client.widget.MiniPieWidget;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.core.client.widget.UserPrefMenuItem;
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

      pie_ = new MiniPieWidget("#00000", "#ffffff", 0);
      pie_.setHeight("18px");
      pie_.setWidth("18px");
      host_.add(pie_);

      ToolbarPopupMenu memoryMenu = new ToolbarPopupMenu();
      memoryMenu.addItem(new UserPrefMenuItem<Boolean>(
         prefs_.showMemoryUsage(),
         true,
         "Show current memory usage",
         prefs_
      ));

      menu_ = new ToolbarMenuButton(
         "Mem",
         ToolbarButton.NoTitle,
         (ImageResource) null,
         memoryMenu);
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
         pie_.setVisible(false);
         menu_.setText("Mem");
      }
      else
      {
         long percent = Math.round(((usage.getUsed().getKb() * 1.0) / (usage.getTotal().getKb() * 1.0)) * 100);
         menu_.setText("Mem: " + percent + "%");
         menu_.setTitle("Used by process: " + (usage.getProcess().getKb() / 1024) + " MiB\n" +
            "Total used: " + (usage.getUsed().getKb() / 1024) + " MiB\n" +
            "Total memory: " + (usage.getTotal().getKb() / 1024) + " MiB");

         pie_.setPercent((int)percent);
         pie_.setVisible(true);
      }
   }

   private final UserPrefs prefs_;
   private final MiniPieWidget pie_;
   private final ToolbarMenuButton menu_;
   private final HTMLPanel host_;
}
