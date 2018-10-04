/*
 * KeyboardMonitor.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client;

import org.rstudio.core.client.command.KeyboardShortcut;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class KeyboardMonitor
{
   @Inject
   public KeyboardMonitor()
   {
      Event.addNativePreviewHandler((NativePreviewEvent preview) -> {
         
         NativeEvent event = preview.getNativeEvent();
         
         if (event.getType().equals("keydown"))
            toggleState(event, true);
         else if (event.getType().equals("keyup"))
            toggleState(event, false);
         
      });
   }
   
   public int getModifierValue()
   {
      return
            (isCtrlKeyDown()  ? KeyboardShortcut.CTRL  : 0) +
            (isAltKeyDown()   ? KeyboardShortcut.ALT   : 0) +
            (isShiftKeyDown() ? KeyboardShortcut.SHIFT : 0) +
            (isMetaKeyDown()  ? KeyboardShortcut.META  : 0);
            
   }
   
   public boolean isCtrlKeyDown()
   {
      return ctrl_;
   }
   
   public boolean isAltKeyDown()
   {
      return alt_;
   }
   
   public boolean isShiftKeyDown()
   {
      return shift_;
   }
   
   public boolean isMetaKeyDown()
   {
      return leftMeta_ || meta_ || rightMeta_;
   }
   
   private void toggleState(NativeEvent event, boolean enable)
   {
      int keyCode = event.getKeyCode();
      switch (keyCode)
      {
      case CTRL:        ctrl_      = enable;  break;
      case ALT:         alt_       = enable;  break;
      case SHIFT:       shift_     = enable;  break;
      case LEFT_META:   leftMeta_  = enable;  break;
      case META:        meta_      = enable;  break;
      case RIGHT_META:  rightMeta_ = enable;  break;
      }
   }
   
   private boolean ctrl_;
   private boolean alt_;
   private boolean shift_;
   private boolean meta_;
   private boolean leftMeta_;
   private boolean rightMeta_;
   
   private static final int CTRL       = KeyCodes.KEY_CTRL;
   private static final int ALT        = KeyCodes.KEY_ALT;
   private static final int SHIFT      = KeyCodes.KEY_SHIFT;
   private static final int LEFT_META  = 91;
   private static final int META       = 92;
   private static final int RIGHT_META = 93;
}
