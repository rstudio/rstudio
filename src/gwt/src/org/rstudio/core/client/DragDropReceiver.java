/*
 * DragDropManager.java
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
package org.rstudio.core.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;

public abstract class DragDropReceiver
{
   public abstract void onDragOver(NativeEvent event);
   public abstract void onDragLeave(NativeEvent event);
   public abstract void onDrop(NativeEvent event);
   
   public DragDropReceiver(Widget host)
   {
      addDragDropHandlers(this, host.getElement());
   }
   
   private static final native void addDragDropHandlers(DragDropReceiver self,
                                                        Element el)
   /*-{
      
      el.addEventListener("dragover", function(event) {
         event.preventDefault();
         self.@org.rstudio.core.client.DragDropReceiver::onDragOver(*)(event);
      });
      
      el.addEventListener("dragleave", function(event) {
         event.preventDefault();
         self.@org.rstudio.core.client.DragDropReceiver::onDragLeave(*)(event);
      });
      
      el.addEventListener("drop", function(event) {
         event.preventDefault();
         console.log(event.dataTransfer);
         self.@org.rstudio.core.client.DragDropReceiver::onDrop(*)(event);
      });
   
   }-*/;
}
