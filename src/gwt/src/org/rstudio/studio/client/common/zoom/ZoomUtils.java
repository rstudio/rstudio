/*
 * ZoomUtils.java
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

package org.rstudio.studio.client.common.zoom;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.NativeScreen;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;

import com.google.gwt.user.client.Window;


public class ZoomUtils
{  
   public static Size getZoomWindowSize(Size sourceFrameSize, Size defaultSize)
   {
      int width, height;
      if (defaultSize != null)
      {
         // trim based on available screen size
         NativeScreen screen = NativeScreen.get();
         width = Math.min(screen.getAvailWidth(), defaultSize.width);
         height = Math.min(screen.getAvailHeight(), defaultSize.height); 
      }
      else
      {
         final int PADDING = 20;
   
         // calculate ideal height and width. try to be as large as possible
         // within the bounds of the current client size
         Size bounds = new Size(Window.getClientWidth() - PADDING,
                                Window.getClientHeight() - PADDING);
   
         float widthRatio = bounds.width / ((float)sourceFrameSize.width);
         float heightRatio = bounds.height / ((float)sourceFrameSize.height);
         float ratio = Math.min(widthRatio, heightRatio);
   
         // constrain initial width to between 300 and 1,200 pixels
         width = Math.max(300, (int) (ratio * sourceFrameSize.width));
         width = Math.min(1200, width);
         
         // constrain initial height to between 300 and 900 pixels
         height = Math.max(300, (int) (ratio * sourceFrameSize.height));
         height = Math.min(900, height);
      }
      
      return new Size(width, height);
   }
   
   public static Size getZoomedSize(Size size, Size minSize, Size maxSize)                              
   {
      float widthRatio = maxSize.width / ((float)size.width);
      float heightRatio = maxSize.height / ((float)size.height);
      float ratio = Math.min(widthRatio, heightRatio);

      // constrain width
      int width = Math.max(minSize.width, (int) (ratio * size.width));
      width = Math.min(maxSize.width, width);
      
      // constrain height
      int height = Math.max(minSize.height, (int) (ratio * size.height));
      height = Math.min(maxSize.height, height);
      
      return new Size(width, height);
   }
   
   public static void openZoomWindow(String name, String url, Size size, 
                                     final OperationWithInput<WindowEx> onOpened)
   {
      NewWindowOptions options = new NewWindowOptions();
      options.setName(name);
      options.setFocus(true);
      options.setCallback(onOpened);
      RStudioGinjector.INSTANCE.getGlobalDisplay().openMinimalWindow(
                                       url,
                                       false,
                                       size.width,
                                       size.height,
                                       options);
   }
   
   
}
