/*
 * MarkdownToolbar.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import org.rstudio.core.client.widget.LatchingToolbarButton;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.event.dom.client.ClickHandler;

public class MarkdownToolbar extends SecondaryToolbar
{
   public MarkdownToolbar(Commands commands, ClickHandler visualModeClickHandler)
   {
      super(true, "Markdown editing tools");
      
      visualMode_ = new LatchingToolbarButton(
         "Visual Editor", 
         commands.toggleRmdVisualMode().getTooltip(),
         false,
         null,
         visualModeClickHandler
      );
      visualMode_.addStyleName("rstudio-themes-inverts");
      addLeftWidget(visualMode_);
      visualMode_.setLatched(true);
      

      sourceMode_ = new LatchingToolbarButton(
         "Source Editor", 
         commands.toggleRmdVisualMode().getTooltip(),
         false,
         null,
         visualModeClickHandler
      );
      sourceMode_.addStyleName("rstudio-themes-inverts");
      addLeftWidget(sourceMode_);
            
   }
   
   public void setVisualMode(boolean visualMode)
   {
      visualMode_.setLatched(visualMode);
      sourceMode_.setLatched(!visualMode);
   }
   
   private LatchingToolbarButton visualMode_;
   private LatchingToolbarButton sourceMode_;
}
