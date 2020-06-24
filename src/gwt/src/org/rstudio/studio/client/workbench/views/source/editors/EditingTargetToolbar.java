/*
 * EditingTargetToolbar.java
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
package org.rstudio.studio.client.workbench.views.source.editors;

import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Widget;

public class EditingTargetToolbar extends Toolbar
{
   public EditingTargetToolbar(Commands commands, boolean includePopout, SourceColumn column)
   {
      super("Code Editor Tab");
      addLeftWidget(commands.sourceNavigateBack().createToolbarButton(column));
      Widget forwardButton = commands.sourceNavigateForward().createToolbarButton(column);
      forwardButton.getElement().getStyle().setMarginLeft(-6, Unit.PX);
      addLeftWidget(forwardButton);
      addLeftSeparator();
      if (includePopout)
      {
         if (SourceWindowManager.isMainSourceWindow())
         {
            addLeftWidget(commands.popoutDoc().createToolbarButton(column));
         }
         else
         {
            addLeftWidget(commands.returnDocToMain().createToolbarButton(column));
         }
         addLeftSeparator();
      }
   }
}
