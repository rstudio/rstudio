/*
 * SavePlotAsPdfOptions.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.core.client.JavaScriptObject;

public class SavePlotAsPdfOptions extends JavaScriptObject
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
      options.width = width ;
      options.height = height ;
      options.portrait = portrait;
      options.cairo_pdf = cairoPdf;
      options.viewAfterSave = viewAfterSave;
      return options ;
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
   
   public final native double getWidth() /*-{
      return this.width;
   }-*/;
   
   public final native double getHeight() /*-{
      return this.height;
   }-*/;
   
   public final native boolean getPortrait() /*-{
      return this.portrait;
   }-*/;
   
   public final native boolean getCairoPdf() /*-{
      return this.cairo_pdf;
   }-*/;

   
   public final native boolean getViewAfterSave() /*-{
      return this.viewAfterSave;
   }-*/;
   
}
