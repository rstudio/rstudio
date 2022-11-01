/*
 * MarkdownToolbar.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;

import org.rstudio.core.client.widget.LatchingToolbarButton;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

public class MarkdownToolbar extends SecondaryToolbar implements RequiresResize
{
   public MarkdownToolbar(Commands commands, ClickHandler visualModeClickHandler)
   {
      super(true, constants_.markdownEditingTools());
      addStyleName(RES.styles().markdownToolbar());
      
      sourceMode_ = new LatchingToolbarButton(
            constants_.source(),
            commands.toggleRmdVisualMode().getTooltip(),
            false,
            null,
            visualModeClickHandler
         );
         sourceMode_.addStyleName("rstudio-themes-inverts");
         sourceMode_.addStyleName(RES.styles().editorModeButton());
         addLeftWidget(sourceMode_);
      
      visualMode_ = new LatchingToolbarButton(
         constants_.visual(),
         commands.toggleRmdVisualMode().getTooltip(),
         false,
         null,
         visualModeClickHandler
      );
      visualMode_.addStyleName("rstudio-themes-inverts");
      visualMode_.addStyleName(RES.styles().editorModeButton());
      addLeftWidget(visualMode_);
      visualMode_.setLatched(true);
            
   }
   
   public void setVisualMode(boolean visualMode)
   {
      visualMode_.setLatched(visualMode);
      sourceMode_.setLatched(!visualMode);
      visualModeTools_.forEach(tool -> tool.setVisible(visualMode));
   }

   
   public void addVisualModeTools(Widget tools)
   {
      visualModeTools_.add(tools);
      addLeftWidget(tools);
   }
   
   
   private LatchingToolbarButton visualMode_;
   private LatchingToolbarButton sourceMode_;
   private ArrayList<Widget> visualModeTools_ = new ArrayList<Widget>();
   
   interface Styles extends CssResource
   {
      String markdownToolbar();
      String editorModeButton();
   }
   
   interface Resources extends ClientBundle
   {
      @Source("MarkdownToolbar.css")
      Styles styles();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   static { RES.styles().ensureInjected(); }
   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);
}
