/*
 * PresentationFrame.java
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

package org.rstudio.studio.client.workbench.views.presentation;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.AnchorableFrame;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.HasText;

public class PresentationFrame extends AnchorableFrame
{
   public PresentationFrame(boolean autoFocus)
   {
      this(autoFocus, false);
   }
   
   public PresentationFrame(boolean autoFocus, 
                            boolean allowFullScreen)
   {
      this(autoFocus, allowFullScreen, null);
   }
                           
   public PresentationFrame(boolean autoFocus, 
                            boolean allowFullScreen,
                            final HasText titleWidget)
   {
      super("Presentation Frame", autoFocus);
      
      // allow full-screen view of iframe
      if (allowFullScreen)
      {
         Element el = getElement();
         el.setAttribute("webkitallowfullscreen", "");
         el.setAttribute("mozallowfullscreen", "");
         el.setAttribute("allowfullscreen", "");
      }
      
      addLoadHandler(new LoadHandler() {

         @Override
         public void onLoad(LoadEvent event)
         {
            // set title
            title_ = StringUtil.notNull(
                                getWindow().getDocument().getTitle());
            
            if (titleWidget != null)
               titleWidget.setText(title_);
         }
      }); 
   }
   
   public String getFrameTitle()
   {
      return title_;
   }
   
   public void clear()
   {
      getWindow().replaceLocationHref("about:blank");
   }
   
   public void home()
   {
      Reveal.fromWindow(getWindow()).home();
   }
   
   public void slide(int index)
   {
      Reveal.fromWindow(getWindow()).slide(index);
   }
   
   public void next()
   {
      Reveal.fromWindow(getWindow()).next();
   }
   
   public void prev()
   {
      Reveal.fromWindow(getWindow()).prev();
   }
   
   private static class Reveal extends JavaScriptObject
   {
      protected Reveal()
      {
      }
      
      public static final native Reveal fromWindow(WindowEx window) /*-{
         return window.Reveal;
      }-*/;
      
      public final native void home() /*-{
         this.slide(0);
      }-*/;
      
      public final native void slide(int index) /*-{
         this.slide(index);
      }-*/;
      
      public final native void next() /*-{
         this.next();
      }-*/;
      
      public final native void prev() /*-{
         this.prev();
      }-*/;
   }
   
   
   private String title_ = "";

}
