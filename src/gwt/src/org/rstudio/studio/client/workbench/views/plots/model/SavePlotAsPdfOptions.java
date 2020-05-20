/*
 * SavePlotAsPdfOptions.java
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
package org.rstudio.studio.client.workbench.views.plots.model;

import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor;

public class SavePlotAsPdfOptions extends UserStateAccessor.SavePlotAsPdfOptions
{
   protected SavePlotAsPdfOptions()
   {   
   }
   
   public static final SavePlotAsPdfOptions createDefault()
   {
      return SavePlotAsPdfOptions.create(8.5, 11);
   }
   
   public static final SavePlotAsPdfOptions create(double width, double height)
   {
      return create(width, height, true, false, false);
   }
   
   public static final native SavePlotAsPdfOptions create(
                                                  double width, 
                                                  double height,
                                                  boolean portrait,
                                                  boolean cairoPdf,
                                                  boolean viewAfterSave) /*-{
      var options = new Object();
      options.width = width;
      options.height = height;
      options.portrait = portrait;
      options.cairo_pdf = cairoPdf;
      options.viewAfterSave = viewAfterSave;
      return options;
   }-*/;
   
   public static final SavePlotAsPdfOptions adaptToSize(
                                            SavePlotAsPdfOptions options,
                                            double width,
                                            double height)
   {
      return SavePlotAsPdfOptions.create(width, 
                                         height, 
                                         options.getPortrait(),
                                         options.getCairoPdf(),
                                         options.getViewAfterSave());
   }
   
   public static native boolean areEqual(SavePlotAsPdfOptions a, SavePlotAsPdfOptions b) /*-{
      if (a === null ^ b === null)
         return false;
      if (a === null)
         return true;
      return a.width === b.width &&
             a.height === b.height &&
             a.portrait === b.portrait &&
             a.cairo_pdf === b.cairo_pdf &&
             a.viewAfterSave === b.viewAfterSave;    
   }-*/;
}
