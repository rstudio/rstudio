/*
 * ImageFrame.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Frame;

import org.rstudio.core.client.Size;

public class ImageFrame extends Frame
{
   public ImageFrame(String title)
   {
      setUrl("javascript:false");
      setTitle(title);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      new Timer() {
         @Override
         public void run()
         {
            // No way to tell when iframe is actually ready to be
            // manipulated (sometimes contentWindow is null). Need
            // to probe and retry.
            if (!isReadyForContent(getElement()))
            {
               this.schedule(200);
            }
            else
            {
               String sizing = "width=\"100%\" height=\"100%\"";
               setupContent(getElement(), sizing);
               replaceLocation(getElement(), url_, fixedWidth(), fixedHeight());
            }
         }
      }.schedule(100);
   }

   public void setMarginWidth(int width)
   {
      getElement().setAttribute("marginwidth", 
                                Integer.toString(width));
   }
   
   public void setMarginHeight(int height)
   {
      getElement().setAttribute("marginheight", 
                                Integer.toString(height));
   }
   
   public void setImageUrl(String url)
   {
      setImageUrl(url, null);
   }

   /**
    * Shows the image at the given size, in CSS pixels, scaling it down to fit
    * the frame and centering it; a null size stretches it to fill the frame.
    */
   public void setImageUrl(String url, Size fixedSize)
   {
      url_ = url;
      fixedSize_ = fixedSize;
      if (isAttached())
         replaceLocation(getElement(), url, fixedWidth(), fixedHeight());
   }

   private int fixedWidth()
   {
      return fixedSize_ == null ? 0 : fixedSize_.width;
   }

   private int fixedHeight()
   {
      return fixedSize_ == null ? 0 : fixedSize_.height;
   }

   private native final boolean replaceLocation(Element el,
                                                String url,
                                                int fixedWidth,
                                                int fixedHeight) /*-{
      // contentWindow itself is null while the iframe is detached or being
      // re-parented (e.g. a pane-layout quadrant swap moves the Plots pane),
      // and dereferencing it raises an uncaught TypeError that surfaces as an
      // error dialog. Report not-ready instead; the caller's retry timer
      // re-attempts once the frame has a window again.
      if (!el.contentWindow || !el.contentWindow.document)
         return false;
      var img = el.contentWindow.document.getElementById('img');
      if (!img)
         return false;
      var style = img.style;
      if (fixedWidth > 0 && fixedHeight > 0) {
         // the viewport units are the frame's; the outline shows where the
         // plot ends when its background matches the pane's
         var w = fixedWidth, h = fixedHeight;
         style.position = 'absolute';
         style.inset = '0';
         style.margin = 'auto';
         style.width = 'min(' + w + 'px, 100vw, calc(100vh * ' + w + ' / ' + h + '))';
         style.height = 'min(' + h + 'px, 100vh, calc(100vw * ' + h + ' / ' + w + '))';
         style.boxShadow = '0 0 0 1px rgba(128, 128, 128, 0.5)';
      }
      else {
         style.position = style.inset = style.margin = '';
         style.width = style.height = style.boxShadow = '';
      }
      if (url && url != 'javascript:false') {
         style.display = 'inline';
         img.src = url;
      }
      else {
         style.display = 'none';
      }
      return true;
   }-*/;

   private native boolean isReadyForContent(Element el) /*-{
      return el != null
            && el.contentWindow != null
            && el.contentWindow.document != null; 
   }-*/;

   private native void setupContent(Element el, String sizing) /*-{
      var doc = el.contentWindow.document;

      // setupContent can get called multiple times, as progress causes the
      // widget to be loaded/unloaded. This condition checks if we're already
      // set up.
      if (doc.getElementById('img'))
         return;

      doc.open();
      doc.write(
         '<html><head></head>' +
         '<body style="margin: 0; padding: 0; overflow: hidden; border: none">' +
         '<img id="img" ' + sizing + ' style="display: none" src="data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs%3D">' +
         '</body></html>');
      doc.close();
   }-*/;

   private String url_ = "javascript:false";
   private Size fixedSize_ = null;
}
