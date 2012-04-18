/*
 * PreWidget.java
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

public class PreWidget extends Widget implements HasKeyDownHandlers,
                                                 HasClickHandlers
{
   public PreWidget()
   {
      setElement(Document.get().createPreElement());
      getElement().setTabIndex(0);
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return addDomHandler(handler, KeyDownEvent.getType());
   }

   public void setText(String text)
   {
      getElement().setInnerText(text);
   }

   public void appendText(String text)
   {
      getElement().setInnerText(getElement().getInnerText() + text);
   }
}
