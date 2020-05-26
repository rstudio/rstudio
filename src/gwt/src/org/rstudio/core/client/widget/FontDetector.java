/*
 * FontDetector.java
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

package org.rstudio.core.client.widget;

import org.rstudio.core.client.Debug;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;

public class FontDetector
{
   public static boolean isFontSupported(String fontName)
   {
      SimplePanel panel = null;
      try
      {
         // default font name as a reference point
         final String defaultFontName = "Arial";
         if (defaultFontName.equals(fontName))
            return true;
         
         // make sure canvas is supported
         if (!Canvas.isSupported())
            return false;
               
         // add a temporary div to the dom
         panel = new SimplePanel();
         panel.setHeight("200px");
         panel.getElement().getStyle().setVisibility(Visibility.HIDDEN);
         panel.getElement().getStyle().setOverflow(Overflow.SCROLL);
         RootPanel.get().add(panel, -2000, -2000);
      
         // add a canvas element to the div and get the 2d drawing context
         final Canvas canvas = Canvas.createIfSupported();
         canvas.setWidth("512px");
         canvas.setHeight("64px");
         canvas.getElement().getStyle().setLeft(400, Unit.PX);
         canvas.getElement().getStyle().setBackgroundColor("#ffe");
         panel.add(canvas);
         final Context2d ctx = canvas.getContext2d();
         ctx.setFillStyle("#000000");
         
         // closure to generate a hash for a font
         class HashGenerator { 
            public String getHash(String fontName)
            {
               ctx.setFont("57px " + fontName + ", " + defaultFontName);
               int width = canvas.getOffsetWidth();
               int height = canvas.getOffsetHeight();
               ctx.clearRect(0, 0, width, height);
               ctx.fillText("TheQuickBrownFox", 2, 50);
               return canvas.toDataUrl();
            }}
         
         // get hashes and compare them
         HashGenerator hashGenerator = new HashGenerator();
         String defaultHash = hashGenerator.getHash(defaultFontName);
         String fontHash = hashGenerator.getHash(fontName);
         return !defaultHash.equals(fontHash);
      }
      catch(Exception ex)
      {
         Debug.log(ex.toString());
         return false;
      }
      finally
      {
         if (panel != null)
            RootPanel.get().remove(panel);
      }
   }
}
