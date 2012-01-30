/*
 * WeaveRnwSelectWidget.java
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
package org.rstudio.studio.client.common.prefs;

import org.rstudio.core.client.widget.SelectWidget;

import com.google.gwt.dom.client.Style.Unit;

public class WeaveRnwSelectWidget extends SelectWidget
{
   public WeaveRnwSelectWidget()
   {
      this("Weave Rnw files with:");
   }
   public WeaveRnwSelectWidget(String label)
   {
      super(label, new String[] { "Sweave", "knitr" }); 
      getElement().getStyle().setMarginLeft(2, Unit.PX);
   }        
}
