/*
 * UrlContentEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.urlcontent;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.widget.RStudioThemedFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;

public class UrlContentEditingTargetWidget extends Composite
   implements UrlContentEditingTarget.Display
{
   public UrlContentEditingTargetWidget(String title,
                                        Commands commands,
                                        String url,
                                        SourceColumn column)
   {
      commands_ = commands;

      frame_ = new RStudioThemedFrame(title, url, true, "allow-same-origin", null, null, false, true);
      frame_.setSize("100%", "100%");

      column_ = column;

      panel_ = new PanelWithToolbars(createToolbar(), frame_);
      Roles.getTabpanelRole().set(panel_.getElement());
      setAccessibleName(null);
      initWidget(panel_);
   }

   private Toolbar createToolbar()
   {
      Toolbar toolbar = new EditingTargetToolbar(commands_, true, column_);
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

   @Override
   public void setAccessibleName(String name)
   {
      if (StringUtil.isNullOrEmpty(name))
         name = "Untitled URL Browser";
      Roles.getTabpanelRole().setAriaLabelProperty(panel_.getElement(), name + " URL Browser");
   }

   private final Commands commands_;
   private RStudioThemedFrame frame_;
   private final PanelWithToolbars panel_;
   private SourceColumn column_;
}
