/*
 * InlineToolbarSeparator.java
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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.theme.res.ThemeResources;

public class InlineToolbarSeparator extends Composite
{
   public InlineToolbarSeparator()
   {
      ZeroHeightPanel panel = new ZeroHeightPanel(2, 20, 5);
      panel.addChild(new Image(ThemeResources.INSTANCE.toolbarSeparator()));
      initWidget(panel);
      getElement().getStyle().setMarginLeft(4, Style.Unit.PX);
   }
}
