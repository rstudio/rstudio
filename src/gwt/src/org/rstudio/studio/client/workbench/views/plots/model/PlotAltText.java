/*
 * PlotAltText.java
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

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.plots.PlotsConstants;

public class PlotAltText
{
   /**
    * The plot's position in the history, followed by the alt text its author
    * supplied, if any.
    */
   public static String forState(PlotsState plotsState)
   {
      int position = plotsState.getPlotIndex() + 1;
      int count = plotsState.getPlotCount();
      String authorAltText = plotsState.getAltText();
      if (StringUtil.isNullOrEmpty(authorAltText))
         return constants_.plotAltText(position, count);
      return constants_.plotAltTextWithDescription(position, count, authorAltText);
   }

   private static final PlotsConstants constants_ = GWT.create(PlotsConstants.class);
}
