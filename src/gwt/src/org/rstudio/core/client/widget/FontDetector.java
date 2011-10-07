/*
 * FontDetector.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.core.client.widget;

import org.rstudio.core.client.Debug;

public class FontDetector
{
   public static boolean isFontSupported(String fontName)
   {
      try
      {
         return isFontSupportedNative(fontName);
      }
      catch(Exception ex)
      {
         Debug.log(ex.toString());
         return false;
      }
   }
   
   private static native boolean isFontSupportedNative(String fontName) /*-{
      
      // alias doc and body
      var doc = $wnd.document;
      var body = doc.getElementsByTagName("BODY")[0];
      
      // default font is used as a reference size
      var defaultFontName = "Arial";
      if (fontName == defaultFontName)
         return true;
     
      // temporary content element which we write into 
      var content = doc.createElement("div");
      content.id = "content";
      content.setAttribute("style", 
                              "height: 200px; " + 
                              "visibility: hidden; " +
                              "overflow: scroll");
      body.appendChild(content);

      // load canvas element and get 2d drawing context
      var canvas = doc.createElement("canvas");
      canvas.width = 512;
      canvas.height = 64;
      canvas.setAttribute("style", "background: #ffe;  left: 400px");
      content.appendChild(canvas);
      content.appendChild(doc.createElement("br"));
      var ctx = canvas.getContext("2d");
      ctx.fillStyle = "#000000";
      
      // function to generate a rendered content hash for a given font
      function getHash(font) {
         ctx.font = "57px " + font + ", " + defaultFontName;
         ctx.clearRect(0, 0, canvas.width, canvas.height);
         ctx.fillText("TheQuickBrownFox", 2, 50);
         return canvas.toDataURL();
      } 
     
      // generate hashes
      var defaultFontHash = getHash(defaultFontName);
      var fontHash = getHash(fontName);
      
      // remove temporary element
      body.removeChild(content);
    
      // return result
      return fontHash != defaultFontHash;
   }-*/;
}
