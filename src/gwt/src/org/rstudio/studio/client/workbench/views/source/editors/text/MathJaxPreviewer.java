/*
 * MathJaxPreviewer.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.common.mathjax.MathJax;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;

import com.google.gwt.core.client.JsArray;

public class MathJaxPreviewer
{
   public MathJaxPreviewer(TextEditingTarget target)
   {
      target_ = target;
      display_= target.getDocDisplay();
   }
   
   public void renderAllLatex()
   {
      target_.renderLatex();
   }
   
   public void removeAllLatex()
   {
      JsArray<LineWidget> widgets = display_.getLineWidgets();
      for (int i = 0; i < widgets.length(); i++)
      {
         LineWidget widget = widgets.get(i);
         if (widget.getType() == MathJax.LINE_WIDGET_TYPE)
            display_.removeLineWidget(widget);
      }
   }
   
   private final DocDisplay display_;
   private final TextEditingTarget target_;
}
