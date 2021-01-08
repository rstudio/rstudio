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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import org.rstudio.core.client.widget.MiniPieWidget;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsage;

public class MemUsageWidget extends Composite
{
   public MemUsageWidget(MemoryUsage usage)
   {
      host_ = new HTMLPanel("");
      Style style = host_.getElement().getStyle();
      style.setProperty("display", "flex");
      style.setProperty("flexDirection", "row");
      style.setMarginTop(3, Style.Unit.PX);

      pie_ = new MiniPieWidget("#00000", "#ffffff", 0);
      pie_.setHeight("18px");
      pie_.setWidth("18px");
      percent_ = new Label("");
      percent_.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

      setMemoryUsage(usage);

      initWidget(host_);
   }

   public void setMemoryUsage(MemoryUsage usage)
   {
      if (usage == null && usage_ != null)
      {
         host_.remove(pie_);
         host_.remove(percent_);
      }
      else
      {
         long percent = Math.round(((usage.getUsed().getKb() * 1.0) / (usage.getTotal().getKb() * 1.0)) * 100);
         percent_.setText("Mem: " + percent + "%");
         percent_.setTitle("Used by process: " + (usage.getProcess().getKb() / 1024) + " MiB\n" +
            "Total used: " + (usage.getUsed().getKb() / 1024) + " MiB\n" +
            "Total memory: " + (usage.getTotal().getKb() / 1024) + " MiB");
         percent_.setVisible(true);

         pie_.setPercent((int)percent);
         pie_.setVisible(true);

         if (usage_ == null)
         {
            host_.add(pie_);
            host_.add(percent_);
         }
      }

      usage_ = usage;
   }

   private final MiniPieWidget pie_;
   private final Label percent_;
   private final HTMLPanel host_;
   private MemoryUsage usage_;
}
