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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Widget;

public class EditingTargetToolbar extends Toolbar
{
   public EditingTargetToolbar(Commands commands, boolean includePopout, SourceColumn column)
   {
      this(commands, includePopout, column, "");
   }

   public EditingTargetToolbar(Commands commands, boolean includePopout,
                               SourceColumn column, String id)
   {
      super("Code Editor Tab");

      // Buttons are unique to a source column so require SourceAppCommands
      SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();

      id_ = id;
      addLeftWidget(commands.sourceNavigateBack().createToolbarButton());
      Widget forwardButton = commands.sourceNavigateForward().createToolbarButton();
      forwardButton.getElement().getStyle().setMarginLeft(-6, Unit.PX);
      addLeftWidget(forwardButton);
      addLeftSeparator();
      if (includePopout)
      {
         if (SourceWindowManager.isMainSourceWindow())
         {
            addLeftWidget(
               mgr.getSourceCommand(commands.popoutDoc(), column).createToolbarButton());
         }
         else
         {
            addLeftWidget(
               mgr.getSourceCommand(commands.returnDocToMain(), column).createToolbarButton());
         }
         addLeftSeparator();
      }
   }

   // wrapper methods to add the editing target's id to the class id

   @Override
   public <TWidget extends Widget> TWidget insertWidget(TWidget widget, TWidget beforeWidget)
   {
      widget = super.insertWidget(widget, beforeWidget);
      return addClassId(widget);
   }

   @Override
   public <TWidget extends Widget> TWidget addLeftWidget(TWidget widget)
   {
      widget = super.addLeftWidget(widget);
      return addClassId(widget);
   }

   @Override
   public <TWidget extends Widget> TWidget addRightWidget(TWidget widget)
   {
      widget = super.addRightWidget(widget);
      return addClassId(widget);
   }

   public <TWidget extends Widget> TWidget addClassId(TWidget widget)
   {
      if (!StringUtil.isNullOrEmpty(id_) && widget instanceof ToolbarButton)
         ((ToolbarButton) widget).setClassId(id_);
      return widget;
   }

   private final String id_;
}
