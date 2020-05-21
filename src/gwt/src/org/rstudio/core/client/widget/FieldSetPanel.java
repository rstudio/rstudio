/*
 * FieldSetPanel.java
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import org.rstudio.core.client.a11y.A11y;

/**
 * Base class for panels containing one widget, wrapped in a Fieldset element.
 */
public class FieldSetPanel extends SimplePanel implements HasOneWidget
{
   public FieldSetPanel(String legend, boolean visuallyHideLegend)
   {
      super(DOM.createFieldSet());

      getElement().appendChild(legendElement_ = DOM.createLegend());
      legendElement_.setInnerText(legend);

      if (visuallyHideLegend)
      {
         A11y.setVisuallyHidden(legendElement_);
      }
   }
   
   public FieldSetPanel(String legend)
   {
      this(legend, false);
   }

   public FieldSetPanel()
   {
      this("", false);
   }


   /**
    * @param externalLabel existing visual label for the radio buttons; text of that label
    *                      will be applied to a hidden legend element for accessibility, and
    *                      the label itself will be marked aria-hidden
    */
   public FieldSetPanel(Label externalLabel)
   {
      this(externalLabel.getText(), true);
      A11y.setARIAHidden(externalLabel);
   }
   
   public void setLegend(String legend)
   {
      legendElement_.setInnerText(legend);
   }

   public void setLegendHidden(boolean hidden)
   {
      if (hidden)
         A11y.setVisuallyHidden(legendElement_);
      else
         A11y.unsetVisuallyHidden(legendElement_);
   }

   private Element legendElement_;
}
