/*
 * ImageFrame.java
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Frame;

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
               replaceLocation(getElement(), url_);
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
      url_ = url;
      if (isAttached())
         replaceLocation(getElement(), url);
   }

   private native final boolean replaceLocation(Element el, String url) /*-{
      if (!el.contentWindow.document)
         return false;
      var img = el.contentWindow.document.getElementById('img');
      if (!img)
         return false;
      if (url && url != 'javascript:false') {
         img.style.display = 'inline';
         img.src = url;
      }
      else {
         img.style.display = 'none';
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
}
