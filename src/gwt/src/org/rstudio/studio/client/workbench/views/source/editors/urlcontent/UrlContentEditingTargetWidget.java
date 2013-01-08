/*
 * UrlContentEditingTargetWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.urlcontent;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;

public class UrlContentEditingTargetWidget extends Composite
   implements UrlContentEditingTarget.Display
{
   public UrlContentEditingTargetWidget(Commands commands, String url)
   {
      commands_ = commands;

      frame_ = new Frame(url);
      frame_.setSize("100%", "100%");

      PanelWithToolbars panel = new PanelWithToolbars(createToolbar(),
                                                    frame_);

      initWidget(panel);

   }

   private Toolbar createToolbar()
   {
      Toolbar toolbar = new EditingTargetToolbar(commands_);
      toolbar.addLeftWidget(commands_.popoutDoc().createToolbarButton());  
      return toolbar;
   }

   public void print()
   {
      IFrameElementEx frameEl = (IFrameElementEx) frame_.getElement().cast();
      frameEl.getContentWindow().print();
   }

   public Widget asWidget()
   {
      return this;
   }

   private final Commands commands_;
   private Frame frame_;
}
