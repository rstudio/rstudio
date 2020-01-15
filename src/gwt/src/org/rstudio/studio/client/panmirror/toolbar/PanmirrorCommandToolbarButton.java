
/*
 * PanmirrorCommandToolbarButton.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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



package org.rstudio.studio.client.panmirror.toolbar;

import org.rstudio.core.client.widget.ToolbarButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;



public class PanmirrorCommandToolbarButton extends ToolbarButton
{
   public PanmirrorCommandToolbarButton(PanmirrorCommandUI commandUI)
   {
      super("", commandUI.getMenuText(), commandUI.getImage(), new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            commandUI.execute();
         }
      });
      commandUI_ = commandUI;
      sync();
   }
   
   public void sync()
   {
      setEnabled(commandUI_.isEnabled());
      setVisible(commandUI_.isVisible());
   }
   
   private final PanmirrorCommandUI commandUI_;
}
