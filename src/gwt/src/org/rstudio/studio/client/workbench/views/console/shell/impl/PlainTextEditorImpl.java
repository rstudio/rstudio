/*
 * PlainTextEditorImpl.java
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
package org.rstudio.studio.client.workbench.views.console.shell.impl;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import org.rstudio.core.client.dom.ElementEx;

public class PlainTextEditorImpl
{
   /**
    * Webkit doesn't like empty editable SPANs--it is not possible to
    * drive the focus into one. However, empty editable DIVs are fine.
    * 
    * Firefox likes empty editable DIVs only slightly more--they put 
    * themselves about 0.5em too high and sometimes collapse their height
    * to just a few pixels. However, a DIV that contains two SPANs--one to
    * set the height with a zero-width space and the other being editable--
    * works great.
    * 
    * This method takes one or the other approach, and returns the actual
    * contentEditable element.
    */
   public ElementEx setupTextContainer(Element element)
   {
      DOM.setElementPropertyBoolean(element, "contentEditable", true) ;
      return (ElementEx) element;
   }

   public void relayFocusEvents(HasHandlers handlers)
   {
   }

   public void poll()
   {}
}
