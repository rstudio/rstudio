/*
 * ShortcutViewer.java
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

package org.rstudio.core.client.command;


import org.rstudio.core.client.widget.ShortcutInfoPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShortcutViewer implements NativePreviewHandler
{
   public interface Binder extends CommandBinder<Commands, ShortcutViewer> {}

   @Inject
   public ShortcutViewer(
         Binder binder,
         Commands commands,
         GlobalDisplay globalDisplay)
   {
      binder.bind(commands, this);
   }
   
   @Handler
   public void onViewShortcuts()
   {
      // prevent reentry
      if (shortcutInfo_ != null)
      {
         return;
      }
      shortcutInfo_ = new ShortcutInfoPanel();
      RootLayoutPanel.get().add(shortcutInfo_);
      preview_ = Event.addNativePreviewHandler(this);
   }
   
   @Override
   public void onPreviewNativeEvent(NativePreviewEvent event)
   {
      if (event.isCanceled())
         return;

      if (event.getTypeInt() == Event.ONKEYDOWN || 
          event.getTypeInt() == Event.ONMOUSEDOWN)
      {
         if (shortcutInfo_ != null)
            RootLayoutPanel.get().remove(shortcutInfo_);
         shortcutInfo_ = null;
         if (preview_ != null)
            preview_.removeHandler();
         preview_ = null;
      }
   }

   private ShortcutInfoPanel shortcutInfo_ = null;
   private HandlerRegistration preview_;
}