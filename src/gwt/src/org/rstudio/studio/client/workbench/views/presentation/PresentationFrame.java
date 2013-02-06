/*
 * PresentationFrame.java
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

package org.rstudio.studio.client.workbench.views.presentation;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.ReloadableFrame;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationCommand;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.HasText;

public class PresentationFrame extends ReloadableFrame
{
   public PresentationFrame(boolean autoFocus, 
                            boolean allowFullScreen,
                            final HasText titleWidget)
   {
      super(autoFocus);
      
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
            Document doc = getWindow().getDocument();
            String title = StringUtil.notNull(
                                getWindow().getDocument().getTitle());
            if (titleWidget != null)
               titleWidget.setText(title);
            
            // fixup links
            NodeList<Element> links = doc.getElementsByTagName("a");
            for (int i=0; i<links.getLength(); i++)
            {
               Element link = links.getItem(i);
               String href = StringUtil.notNull(link.getAttribute("href"));
               if (href.startsWith("#"))
               {
                  // internal link, leave it alone
               }
               else if (href.contains("://"))
               {
                  // external link, show in new window
                  link.setAttribute("target", "_blank");
               }
               else if (href.startsWith("help-doc:") ||
                        href.startsWith("help-topic:"))
               {
                  // help command, change the link to dispatch it
                  int colonLoc = href.indexOf(':');
                  if (href.length() > (colonLoc+2))
                  {
                     String command = href.substring(0, colonLoc).trim();
                     String params = href.substring(colonLoc + 1).trim();
                     PresentationCommand cmd = PresentationCommand.create(
                                                             command, params);
                     String cmdObj = new JSONObject(cmd).toString();
                     String onClick = 
                            "window.parent.dispatchPresentationCommand(" +
                            cmdObj + "); return false;";
                     link.setAttribute("onclick", onClick);
                  }
               }
                  
            }
         }
      }); 
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
   

}
