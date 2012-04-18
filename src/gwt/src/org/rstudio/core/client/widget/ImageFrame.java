/*
 * ImageFrame.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.studio.client.application.Desktop;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Frame;

public class ImageFrame extends Frame
{
   public ImageFrame()
   {
      setUrl("javascript:false");
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
               // under Qt 4.8 on the Mac if we set the width and height of
               // the image in the iframe to 100% then the cpu gets pegged for 
               // ~3 seconds every time we replace the image url
               String sizing = "width=\"100%\" height=\"100%\"";
               if (Desktop.isDesktop() && BrowseCap.isMacintosh())
                  sizing = "";
               
               setupContent(getElement(), sizing);
               replaceLocation(getElement(), url_);
            }
         }
      }.schedule(100);
   }

   public void setMarginWidth(int width)
   {
      DOM.setElementAttribute(getElement(), 
                              "marginwidth", 
                              Integer.toString(width));
   }
   
   public void setMarginHeight(int height)
   {
      DOM.setElementAttribute(getElement(), 
                              "marginheight", 
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
         '<img id="img" ' + sizing + ' style="display: none" src="javascript:false">' +
         '</body></html>');
      doc.close();
   }-*/;

   private String url_ = "javascript:false";
}
