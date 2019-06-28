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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.StringUtil;

/**
 * A label associated with a form control. Use in UiBinder-created labels where 
 * the association must be set manually after UI is generated.
 */
public class FormLabel extends Label
{
   public static String NoForId = null;

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
      super(text, NoForId);
   }

  /**
   * Creates a label to associate with a form control via <code>setFor</code>
   * @param text the new label's text
   * @param w labeled widget; if the widget's element does not already have an id attribute,
   *          one will be generated and assigned to it
   * @param wordWrap <code>false</code> to disable word wrapping
   */
   public FormLabel(String text, Widget w, boolean wordWrap)
   {
      super(text, NoForId, wordWrap);
      setFor(w);
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
    * Create a label to associate with an existing widget.
    * @param text label text
    * @param w labeled widget; if the widget's element does not already have an id attribute,
    *          one will be generated and assigned to it
    */
   public FormLabel(String text, Widget w)
   {
      super(text, NoForId);
      setFor(w);
   }

   /**
    * Create a label to associate with an existing element
    * @param text label text
    * @param el labeled element; if the element does not already have an id attribute,
    *          one will be generated and assigned to it
    */
   public FormLabel(String text, Element el)
   {
      super(text, NoForId);
      setFor(el);
   }

   /**
    * Associate this label with the given widget. If the widget's element does not
    * have an id attribute, a unique one will be generated and assigned to it.
    * @param widget
    */
   public void setFor(Widget widget)
   {
      if (widget == null)
         return;
      setFor(widget.getElement());
   }

   /**
    * Associate this label with the given element. If the element does not
    * have an id attribute, a unique one will be generated and assigned to it.
    * @param el
    */   public void setFor(Element el)
   {
      if (el == null)
         return;

      String controlId = el.getId();
      if (StringUtil.isNullOrEmpty(controlId))
      {
         controlId = DOM.createUniqueId();
         el.setId(controlId);
      }
      setFor(controlId);
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
