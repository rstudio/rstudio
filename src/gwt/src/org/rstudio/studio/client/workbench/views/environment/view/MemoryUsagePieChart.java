/*
 * MemoryUsagePieChart.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import org.rstudio.core.client.widget.MiniPieWidget;
import org.rstudio.studio.client.workbench.views.environment.ViewEnvironmentConstants;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsage;

/**
 * Wrapper around MiniPieChart for system memory usage statistics.
 */
public class MemoryUsagePieChart extends Composite
{
   public MemoryUsagePieChart(MemoryUsage usage)
   {
      pie_ = new MiniPieWidget(
         constants_.memoryInUse(usage.getPercentUsed(), usage.getTotal().getProviderName()),
         constants_.pieChartDepictingMemoryInUse(),
         MemUsageWidget.MEMORY_PIE_UNUSED_COLOR);

      // Add a segment with the overall usage
      pie_.addSegment(usage.getPercentUsed(), getSystemColorCode(usage.getPercentUsed()));

      // Add a smaller segment showing how much is used by the process (session)
      pie_.addSegment(usage.getProcessPercentUsed(), getProcessColorCode(usage.getPercentUsed()));

      initWidget(pie_);
   }

   /**
    * Gets a color code representing the system memory usage (green = low, red = high, etc.)
    *
    * @return The color code, in a CSS-compatible format.
    */
   public static String getSystemColorCode(int percent)
   {
      // These values are chosen to align with those used in rstudio.cloud.
      String color = "#5f9a91";   // under 70%, green
      if (percent > 90) {
         color = "#e55037";       // 90% and above, red
      } else if (percent > 80) {
         color = "#e58537";       // 80-90%, orange
      } else if (percent > 70) {
         color = "#fcbf49";       // 70-80%, yellow
      }

      return color;
   }

   /**
    * Gets a color code representing the process memory usage (green = low, red = high, etc.)
    *
    * @return The color code, in a CSS-compatible format.
    */
   public static String getProcessColorCode(int percent)
   {
      // These values are chosen to align with those used in rstudio.cloud.
      String color = "#4c7b74";   // under 70%, dark green
      if (percent > 90) {
         color = "#b52f17";       // 90% and above, dark red
      } else if (percent > 80) {
         color = "#d06c1b";       // 80-90%, dark orange
      } else if (percent > 70) {
         color = "#fba904";       // 70-80%, dark yellow
      }

      return color;
   }

   private final MiniPieWidget pie_;
   private static final ViewEnvironmentConstants constants_ = GWT.create(ViewEnvironmentConstants.class);
}
