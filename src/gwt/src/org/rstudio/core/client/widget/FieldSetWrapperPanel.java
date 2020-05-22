/*
 * FieldSetWrapperPanel.java
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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A fieldset element containing a Panel of type T
 */
public class FieldSetWrapperPanel<T extends Panel> extends FieldSetPanel
{
   public FieldSetWrapperPanel(T panel, String legend, boolean visuallyHideLegend)
   {
      super(legend, visuallyHideLegend);
      panel_ = panel;
      super.add(panel_);
   }

   /**
    * @param externalLabel existing visual label for the radio buttons; text of that label
    *                      will be applied to a hidden legend element for accessibility, and
    *                      the label itself will be marked aria-hidden
    */
   public FieldSetWrapperPanel(T panel, Label externalLabel)
   {
      super(externalLabel);
      panel_ = panel;
      super.add(panel_);
   }

   public void add(Widget w)
   {
      panel_.add(w);
   }

   public T getPanel()
   {
      return panel_;
   }

   private T panel_;
}
