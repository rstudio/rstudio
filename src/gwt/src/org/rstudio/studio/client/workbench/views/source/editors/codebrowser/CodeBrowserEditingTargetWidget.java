/*
 * CodeBrowserEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.codebrowser;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;

public class CodeBrowserEditingTargetWidget extends Composite
   implements CodeBrowserEditingTarget.Display
{
   public CodeBrowserEditingTargetWidget(Commands commands,
                                         TextEditingTarget.DocDisplay editor)
   {
      commands_ = commands;
      
      editor_ = editor;
      editor_.setFileType(FileTypeRegistry.R);
      
      panel_ = new PanelWithToolbar(createToolbar(),
                                    editor_.asWidget());
      
      initWidget(panel_);

   }

   private Toolbar createToolbar()
   {
      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.sourceNavigateBack().createToolbarButton());
      Widget forwardButton = commands_.sourceNavigateForward().createToolbarButton();
      forwardButton.getElement().getStyle().setMarginLeft(-6, Unit.PX);
      toolbar.addLeftWidget(forwardButton);
      toolbar.addLeftSeparator();
  
      return toolbar;
   }


   public Widget asWidget()
   {
      return this;
   }

   private final PanelWithToolbar panel_;
   private final Commands commands_;
   private final TextEditingTarget.DocDisplay editor_;

  
}
