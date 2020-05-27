/*
 * ButtonClickManager.java
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
package org.rstudio.core.client.widget.events;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

/**
 * Helper for extending UI elements that activate via click to also
 * activate via spacebar.
 */
public class ButtonClickManager extends HandlerManager implements HasHandlers
{
   public ButtonClickManager(Widget widget, ClickHandler clickHandler)
   {
      super(null);
      widget.addDomHandler(clickHandler, ClickEvent.getType());
      addHandler(ClickEvent.getType(), clickHandler);
      widget.addDomHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_SPACE)
            {
               event.stopPropagation();
               event.preventDefault();
               click();
            }
         }, KeyDownEvent.getType());
   }

   private void click()
   {
      NativeEvent clickEvent = Document.get().createClickEvent(
            1, 0, 0, 0, 0, false, false, false, false);
      DomEvent.fireNativeEvent(clickEvent, this);
   }
}
