/*
 * PlainTextEditorImpl.java
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

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.dom.client.Element;

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
   public Element setupTextContainer(Element element)
   {
      element.setPropertyBoolean("contentEditable", true);
      return element;
   }

   public void relayFocusEvents(HasHandlers handlers)
   {
   }

   public void poll()
   {}
}
