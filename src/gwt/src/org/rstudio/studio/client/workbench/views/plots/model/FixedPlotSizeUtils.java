/*
 * FixedPlotSizeUtils.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.model;

import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor.FixedPlotSize;
import org.rstudio.studio.client.workbench.views.plots.PlotsConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;

public class FixedPlotSizeUtils
{
   public static final String UNITS_INCHES = "in";
   public static final String UNITS_CENTIMETERS = "cm";
   public static final String UNITS_PIXELS = "px";

   // Bounds on either dimension, in pixels at 96 DPI (1 to 30 inches); the
   // session clamps to the same range (SessionPlots.cpp)
   public static final int MIN_PIXELS = 96;
   public static final int MAX_PIXELS = 2880;

   public static native FixedPlotSize create(boolean enabled,
                                             double width,
                                             double height,
                                             String units) /*-{
      return {
         enabled: enabled,
         width: width,
         height: height,
         units: units
      };
   }-*/;

   public static boolean isEnabled(FixedPlotSize size)
   {
      return size != null && size.getEnabled();
   }

   // Pixels per unit, at the 96 DPI the RStudio graphics device draws at.
   public static double pixelsPerUnit(String units)
   {
      if (UNITS_CENTIMETERS.equals(units))
         return 96.0 / 2.54;
      else if (UNITS_PIXELS.equals(units))
         return 1.0;
      else
         return 96.0;
   }

   public static int toPixels(double value, String units)
   {
      return (int) Math.round(value * pixelsPerUnit(units));
   }

   public static String formatValue(double value, String units)
   {
      String pattern = UNITS_PIXELS.equals(units) ? "0" : "0.##";
      return NumberFormat.getFormat(pattern).format(value);
   }

   // The label for the Plots pane's size menu, e.g. "7 x 5 in".
   public static String label(FixedPlotSize size)
   {
      if (!isEnabled(size))
         return constants_.fitToPaneLabel();

      String units = size.getUnits();
      return formatValue(size.getWidth(), units) + " x " +
             formatValue(size.getHeight(), units) + " " + units;
   }

   public static void syncCommands(Commands commands, FixedPlotSize size)
   {
      boolean fixed = isEnabled(size);
      commands.fitPlotToPane().setChecked(!fixed);
      commands.useFixedPlotSize().setChecked(fixed);
   }

   private static final PlotsConstants constants_ = GWT.create(PlotsConstants.class);
}
