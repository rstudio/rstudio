/*
 * PlainTextEditorImplFirefox.java
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
package org.rstudio.studio.client.workbench.views.console.shell.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.dom.client.Element;
import org.rstudio.core.client.dom.DomUtils;

public class PlainTextEditorImplFirefox extends PlainTextEditorImpl
{
   /**
    * @see org.rstudio.studio.client.workbench.views.console.shell.impl.PlainTextEditorImpl#setupTextContainer(Element)
    */
   @Override
   public Element setupTextContainer(Element element)
   {
        
      Element zwspSpan = Document.get().createSpanElement();
      zwspSpan.setInnerText("\u200B");
      
      Element textContainer = Document.get().createDivElement();
      textContainer.getStyle().setDisplay(Display.INLINE);
      
      element.appendChild(zwspSpan);
      element.appendChild(textContainer);
      
      textContainer.setPropertyBoolean("contentEditable", true);

      textContainer_ = textContainer;
      return textContainer_;
   }

   @Override
   public void relayFocusEvents(HasHandlers handlers)
   {
      addDomListener(textContainer_, "focus", handlers);
      addDomListener(textContainer_, "blur", handlers);
   }

   private native static JavaScriptObject addDomListener(
         com.google.gwt.dom.client.Element element,
         String eventName,
         HasHandlers hasHandlers) /*-{
      var listener = $entry(function(e) {
         @com.google.gwt.event.dom.client.DomEvent::fireNativeEvent(Lcom/google/gwt/dom/client/NativeEvent;Lcom/google/gwt/event/shared/HasHandlers;Lcom/google/gwt/dom/client/Element;)(e, hasHandlers, element);
      });
      element.addEventListener(eventName, listener, false);
   }-*/;

   @Override
   public void poll()
   {
      // This doesn't work all that well
      /*
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            manageZwsp();
         }
      });
      */
   }

   private void manageZwsp()
   {
      String val = textContainer_.getInnerText();
      if (val.length() == 0)
      {
         textContainer_.appendChild(Document.get().createTextNode("\u200B"));
      }
      else if (val.length() > 1 && val.indexOf('\u200B') >= 0)
      {
         stripZwsp(textContainer_);
      }
   }

   private void stripZwsp(Node node)
   {
      if (node.getNodeType() == Node.TEXT_NODE)
      {
         while (true)
         {
            String text = node.getNodeValue();
            int index = text.indexOf('\u200B');
            if (index >= 0)
               DomUtils.deleteTextData((Text) node, index, 1);
            else
               break;
         }
      }
      else if (node.getNodeType() == Node.ELEMENT_NODE)
      {
         NodeList<Node> nodes = node.getChildNodes();
         for (int i = 0; i < nodes.getLength(); i++)
            stripZwsp(nodes.getItem(i));
      }
   }

   private Element textContainer_;
}
