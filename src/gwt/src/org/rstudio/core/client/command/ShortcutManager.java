/*
 * ShortcutManager.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.events.NativeKeyDownHandler;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

public class ShortcutManager implements NativePreviewHandler,
                                        NativeKeyDownHandler
{
   public interface Handle
   {
      void close();
   }

   public static final ShortcutManager INSTANCE = new ShortcutManager();

   private ShortcutManager()
   {
      Event.addNativePreviewHandler(this);
   }

   public boolean isEnabled()
   {
      return disableCount_ == 0;
   }

   public Handle disable()
   {
      disableCount_++;
      return new Handle()
      {
         private boolean closed_ = false;

         @Override
         public void close()
         {
            if (!closed_)
               disableCount_--;
            closed_ = true;
         }
      };
   }

   public void register(int modifiers, 
                        int keyCode, 
                        AppCommand command, 
                        String groupName, 
                        String title,
                        String disableModes)
   {
      if (!BrowseCap.hasMetaKey() && (modifiers & KeyboardShortcut.META) != 0)
         return;
      
      KeyboardShortcut shortcut = 
            new KeyboardShortcut(modifiers, keyCode, groupName, title, disableModes);
      if (command == null)
      {
         // If the shortcut is unbound, check to see whether there's another
         // unbound shortcut with the same title; replace it if there is.
         boolean existingShortcut = false;
         for (int i = 0; i < unboundShortcuts_.size(); i++) {
            if (unboundShortcuts_.get(i).getTitle().equals(title)) {
               unboundShortcuts_.set(i, shortcut);
               existingShortcut = true;
               break;
            }
         }
         if (!existingShortcut)
            unboundShortcuts_.add(shortcut);
      }
      else
      {
         ArrayList<AppCommand> commands;
         if (commands_.containsKey(shortcut)) 
         {
            // already have a command for this shortcut; add this one
            commands = commands_.get(shortcut);
         }
         else 
         {
            // no commands yet, make a new list
            commands = new ArrayList<AppCommand>();
            commands_.put(shortcut, commands);
         }
         commands.add(command);

         command.setShortcut(shortcut);
      }
      
      if (shortcut.isModalShortcut())
      {
         modalShortcuts_.add(shortcut);
      }
   }

   public void onKeyDown(NativeKeyDownEvent evt)
   {
      if (evt.isCanceled())
         return;

      if (handleKeyDown(evt.getEvent()))
         evt.cancel();
   }

   public void onPreviewNativeEvent(NativePreviewEvent event)
   {
      if (event.isCanceled())
         return;

      if (event.getTypeInt() == Event.ONKEYDOWN)
      {
         if (handleKeyDown(event.getNativeEvent()))
            event.cancel();
      }
   }
   
   public void setEditorMode(int editorMode)
   {
      editorMode_ = editorMode;
   }
   
   public List<ShortcutInfo> getActiveShortcutInfo()
   {
      List<ShortcutInfo> info = new ArrayList<ShortcutInfo>();
      
      HashMap<Command, ShortcutInfo> infoMap = 
            new HashMap<Command, ShortcutInfo>();
      ArrayList<KeyboardShortcut> shortcuts = 
            new ArrayList<KeyboardShortcut>();
      
      // Create a ShortcutInfo for each unbound shortcut
      for (KeyboardShortcut shortcut: unboundShortcuts_)
      {
         info.add(new ShortcutInfo(shortcut, null));
      }

      // Sort the shortcuts as they were presented in Commands.cmd.xml
      for (KeyboardShortcut ks: commands_.keySet())
      {
         shortcuts.add(ks);
      }
      Collections.sort(shortcuts, new Comparator<KeyboardShortcut>()
      {
         @Override
         public int compare(KeyboardShortcut o1, KeyboardShortcut o2)
         {
            return o1.getOrder() - o2.getOrder();
         }
      });

      // Create a ShortcutInfo for each command (a command may have multiple
      // shortcut bindings)
      for (KeyboardShortcut shortcut: shortcuts)
      {
         // skip shortcuts inaccessible due to editor mode
         if (shortcut.isModeDisabled(editorMode_))
            continue;
         
         AppCommand command = commands_.get(shortcut).get(0);
         if (infoMap.containsKey(command))
         {
            infoMap.get(command).addShortcut(shortcut);
         }
         else
         {
            ShortcutInfo shortcutInfo = new ShortcutInfo(shortcut, command);
            info.add(shortcutInfo);
            infoMap.put(command, shortcutInfo);
         }
      }
      // Sort the commands back into the order in which they were created 
      // (reading them out of the keyset mangles the original order)
      Collections.sort(info, new Comparator<ShortcutInfo>()
      {
         @Override
         public int compare(ShortcutInfo o1, ShortcutInfo o2)
         {
            return o1.getOrder() - o2.getOrder();
         }
      });
      return info;
   }

   private boolean handleKeyDown(NativeEvent e)
   {
      int modifiers = KeyboardShortcut.getModifierValue(e);

      KeyboardShortcut shortcut = new KeyboardShortcut(modifiers,
                                                       e.getKeyCode());

      // check for disabled modal shortcuts if we're modal
      if (editorMode_ > 0)
      {
         for (KeyboardShortcut modalShortcut: modalShortcuts_)
         {
            if (modalShortcut.equals(shortcut) &&
                modalShortcut.isModeDisabled(editorMode_))
            {
               return false;
            }
         }
      }

      if (!commands_.containsKey(shortcut) || commands_.get(shortcut) == null) 
      {
         return false;
      }
      
      
      AppCommand command = null;
      for (int i = 0; i < commands_.get(shortcut).size(); i++) 
      {
         command = commands_.get(shortcut).get(i);
         if (command != null)
         {
            boolean enabled = isEnabled() && command.isEnabled();
            
            // some commands want their keyboard shortcut to pass through 
            // to the browser when they are disabled (e.g. Cmd+W)
            if (!enabled && !command.preventShortcutWhenDisabled())
               return false;
            
            e.preventDefault();

            // if this command is enabled, execute it and stop looking  
            if (enabled) 
            {
               command.executeFromShortcut();
               break;
            }
         }
      }

      return command != null;
   }

   private int disableCount_ = 0;
   private int editorMode_ = KeyboardShortcut.MODE_NONE;
   private final HashMap<KeyboardShortcut, ArrayList<AppCommand> > commands_
                                  = new HashMap<KeyboardShortcut, ArrayList<AppCommand> >();
   private ArrayList<KeyboardShortcut> unboundShortcuts_
                                  = new ArrayList<KeyboardShortcut>();
   private ArrayList<KeyboardShortcut> modalShortcuts_ 
                                  = new ArrayList<KeyboardShortcut>();
}
