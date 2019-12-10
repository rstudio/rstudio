/*
 * ShowDOMElementIDs.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
   }
   
   @Handler
   public void onShowDomElementIds()
   {
      showDomElementIds_ = !showDomElementIds_;
      if (showDomElementIds_)
      {
         startMonitoring();
         panel_.setPopupPosition(100, 100);
         panel_.show();
      }
      else
      {
         stopMonitoring();
         panel_.hide();
      }
   }
   
   private void startMonitoring()
   {
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

         builder.appendHtmlConstant("<strong>Element IDs</strong>");
         builder.appendHtmlConstant("<br>");
         
         for (Element el : els)
         {
            String id = el.getId();
            if (id.isEmpty())
               continue;

            builder.appendEscaped(id);
            builder.appendHtmlConstant("<br>");
         }
         
         builder.appendHtmlConstant("</div>");
         html_.setHTML(builder.toSafeHtml());
      });
   }
   
   private void stopMonitoring()
   {
      handler_.removeHandler();
      handler_ = null;
   }
   
   private boolean showDomElementIds_;
   private HandlerRegistration handler_;
   
   private final MiniPopupPanel panel_;
   private final HTML html_;
   
}
