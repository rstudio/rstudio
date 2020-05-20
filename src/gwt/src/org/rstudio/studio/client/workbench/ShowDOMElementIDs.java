/*
 * ShowDOMElementIDs.java
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
package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DocumentEx;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShowDOMElementIDs
{
   interface Binder extends CommandBinder<Commands, ShowDOMElementIDs>
   {
   }
   
   @Inject
   public ShowDOMElementIDs(Binder binder,
                            Commands commands)
   {
      binder.bind(commands, this);
      
      panel_ = new MiniPopupPanel();
      html_ = new HTML();
      panel_.add(html_);
      
      panel_.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (event.isAttached())
            {
               startMonitoring();
            }
            else
            {
               stopMonitoring();
            }
         }
      });
   }
   
   @Handler
   public void onShowDomElements()
   {
      if (panel_.isShowing())
      {
         panel_.hide();
      }
      else
      {
         panel_.show();
      }
   }
   
   private void startMonitoring()
   {
      stopMonitoring();
      
      handler_ = Event.addNativePreviewHandler((NativePreviewEvent preview) ->
      {
         if (preview.getTypeInt() != Event.ONMOUSEMOVE)
            return;

         NativeEvent event = preview.getNativeEvent();
         int x = event.getClientX();
         int y = event.getClientY();
         Element[] els = DocumentEx.get().elementsFromPoint(x, y);

         SafeHtmlBuilder builder = new SafeHtmlBuilder();

         builder.appendHtmlConstant("<div>");
         for (Element el : els)
         {
            String id = el.getId();
            String[] classNames = el.getClassName().split(" ");
            
            boolean interesting = false;
            if (id.startsWith("rstudio_"))
               interesting = true;
            
            for (String className : classNames)
            {
               if (className.startsWith("rstudio_"))
               {
                  interesting = true;
                  break;
               }
            }
            
            if (!interesting)
               continue;
            
            builder.appendHtmlConstant("<span style='color: rgb(217, 95, 2)'>");
            builder.appendEscaped(el.getTagName().toLowerCase());
            builder.appendHtmlConstant("</span>");
            
            for (String className : classNames)
            {
               if (className.startsWith("rstudio_"))
               {
                  builder.appendHtmlConstant("<span style='color: rgb(117, 112, 179)'>");
                  builder.appendEscaped(".");
                  builder.appendEscaped(className);
                  builder.appendHtmlConstant("</span>");
               }
            }
            
            if (!id.isEmpty())
            {
               builder.appendHtmlConstant("<span style='color: rgb(28, 158, 119)'>");
               builder.appendEscaped("#");
               builder.appendEscaped(id);
               builder.appendHtmlConstant("</span>");
            }
            
            builder.appendHtmlConstant("<br>");
         }
         
         builder.appendHtmlConstant("</div>");
         html_.setHTML(builder.toSafeHtml());
      });
   }
   
   private void stopMonitoring()
   {
      if (handler_ != null)
      {
         handler_.removeHandler();
         handler_ = null;
      }
   }
   
   private HandlerRegistration handler_;
   
   private final MiniPopupPanel panel_;
   private final HTML html_;
   
}
