/*
 * CheckBoxHiddenLabel.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.CheckBox;

/**
 * A CheckBox with a label only visible to screen readers. Not recommended, avoid using.
 */
public class CheckBoxHiddenLabel extends CheckBox
{
   public CheckBoxHiddenLabel(String visuallyHiddenLabel)
   {
      super();
      
      // CheckBox consists of a span with input (checkbox) element followed
      // by a label element (empty in this case).
      Element label = DOM.getChild(getElement(), 1);
      if (label != null)
      {
         Roles.getCheckboxRole().setAriaLabelProperty(label, visuallyHiddenLabel);
      }
   }
}
