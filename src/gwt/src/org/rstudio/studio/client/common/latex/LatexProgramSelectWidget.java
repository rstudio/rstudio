/*
 * LatexProgramSelectWidget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.latex;

import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;

public class LatexProgramSelectWidget extends SelectWidget
{
   public LatexProgramSelectWidget()
   {
      super("Default program for LaTeX typesetting:", 
            latexProgramRegistry_.getTypeNames());
         
      HelpButton helpButton = new HelpButton("latex_program");
      Style style = helpButton.getElement().getStyle();
      style.setMarginTop(3, Unit.PX);
      style.setMarginLeft(4, Unit.PX);
      addWidget(helpButton);
   }
   
   
   public static final LatexProgramRegistry latexProgramRegistry_ = 
         RStudioGinjector.INSTANCE.getLatexProgramRegistry();
}
