/*
 * FormLabel.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;

/**
 * A label associated with a form control. Use in UiBinder-created labels where 
 * the association must be set manually after UI is generated.
 */
public class FormLabel extends Label
{
   public FormLabel()
   {
      this("");
   }

   /**
    * Create a label to associate with a form control via <code>setFor</code>
    * @param text label text
    */
   public FormLabel(String text)
   {
      super(text, "placeholder");
   }

  /**
   * Creates a label to associate with a form control via <code>setFor</code>
   * @param text the new label's text
   * @param wordWrap <code>false</code> to disable word wrapping
   */
   public FormLabel(String text, boolean wordWrap)
   {
      super(text, "placeholder", wordWrap);
   }

   /**
    * Create a label to associate with an existing form control
    * @param text label text
    * @param forId the form controls id
    */
   public FormLabel(String text, String forId)
   {
      super(text, forId);
   }

   /**
    * Associate this label with the given widget. An id will be assigned to the widget
    * element as part of this.
    * @param widget
    */
   public void setFor(Widget widget)
   {
      assert !StringUtil.isNullOrEmpty(getText()) : "FormLabel has not text, not supported";
      if (StringUtil.isNullOrEmpty(getText()))
         return;

      String controlId = ElementIds.idFromLabel(getText());
      widget.getElement().setId(controlId);
      getElement().setAttribute("for", controlId);
   }

   /**
    * Associate this label with the given element id.
    * element as part of this.
    * @param controlId target element of this label
    */
   public void setFor(String controlId)
   {
      getElement().setAttribute("for", controlId);
   }
}
