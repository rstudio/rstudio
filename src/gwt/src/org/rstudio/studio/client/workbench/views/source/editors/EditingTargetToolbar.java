/*
 * EditingTargetToolbar.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors;

import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Widget;

public class EditingTargetToolbar extends Toolbar
{
   public EditingTargetToolbar(Commands commands)
   {
      addLeftWidget(commands.sourceNavigateBack().createToolbarButton());
      Widget forwardButton = commands.sourceNavigateForward().createToolbarButton();
      forwardButton.getElement().getStyle().setMarginLeft(-6, Unit.PX);
      addLeftWidget(forwardButton);
      addLeftSeparator();
   }

}
