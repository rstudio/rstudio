/*
 * GlobalKeyboardListener.java
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
package org.rstudio.core.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.server.remote.ExecuteUserCommandEvent;
import org.rstudio.studio.client.server.remote.RegisterUserCommandEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CustomKeyboardShortcutDispatcher
{
   public static class UserCommand
   {
      public UserCommand(String name, Command command)
      {
         name_ = name;
         command_ = command;
      }
      
      public String getName() { return name_; }
      public void execute() { command_.execute(); }
      
      private final String name_;
      private final Command command_;
   }
   
   public static class UserCommandResult extends JavaScriptObject
   {
      protected UserCommandResult() {}
      
      public native final String getAction() /*-{ return this.action; }-*/;
      public native final String getText() /*-{ return this.text; }-*/;
      private native final JsArrayInteger getRangeVector() /*-{ return this.range; }-*/;
      public final Range getRange()
      {
         JsArrayInteger rangeVector = getRangeVector();
         return Range.create(
               rangeVector.get(0),
               rangeVector.get(1),
               rangeVector.get(2),
               rangeVector.get(3));
      }
   }
   
   public void addCommandBinding(String shortcut, UserCommand command)
   {
      commandMap_.put(shortcut, command);
      commandShortcuts_.add(shortcut);
   }
   
   @Inject
   public CustomKeyboardShortcutDispatcher(EventBus events,
                                           CodeToolsServerOperations server)
   {
      events_ = events;
      server_ = server;
      
      initializeEventBusListeners();
      
      commandMap_ = new LinkedHashMap<String, UserCommand>();
      commandShortcuts_ = new ArrayList<String>();
      
      current_ = "";
      buffer_ = "";
      bufferTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            buffer_ = "";
         }
      };
      
      handler_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            if (preview.getTypeInt() == Event.ONKEYDOWN)
            {
               // Clear the buffer every 3 seconds.
               bufferTimer_.schedule(3000);
               
               NativeEvent event = preview.getNativeEvent();
               int keyCode = event.getKeyCode();
               String keyString = keyEventToString(event);
               
               // If the key-pressed was just a modifier key, don't push it to the
               // buffer -- just record it in 'current' so that the status bar can
               // see it. A key combination is not 'pushed' to the buffer until a
               // non-modifier key is pressed.
               if (KeyboardHelper.isModifierKey(keyCode))
               {
                  current_ = keyString;
               }
               
               // Otherwise, 'push' the old 'current' to the key buffer,
               // and clear 'current'.
               else
               {
                  current_ = "";
                  if (buffer_.isEmpty())
                     buffer_ = keyString;
                  else
                     buffer_ += " " + keyString;
               }
               
               // If there's a match in our key map, execute it.
               if (commandMap_.containsKey(buffer_))
               {
                  UserCommand command = commandMap_.get(buffer_);
                  command.execute();
                  
                  // Clear the buffer and bail.
                  buffer_ = "";
                  return;
               }
               
               // If there are no prefix matches of the current keybuffer,
               // clear the buffer.
               int prefixCount = 0;
               for (int i = 0; i < commandShortcuts_.size(); i++)
                  if (isPrefixMatch(commandShortcuts_.get(i), buffer_))
                     prefixCount += 1;
               
               if (prefixCount == 0)
                  buffer_ = "";
            }
         }
      });
   }
   
   private void initializeEventBusListeners()
   {
      events_.addHandler(
            RegisterUserCommandEvent.TYPE,
            new RegisterUserCommandEvent.Handler()
            {
               @Override
               public void onRegisterUserCommand(RegisterUserCommandEvent event)
               {
                  registerUserCommand(event);
               }
            });
   }
   
   private UserCommand createUserCommand(final RegisterUserCommandEvent.Data data)
   {
      Command command = new Command()
      {
         @Override
         public void execute()
         {
            events_.fireEvent(new ExecuteUserCommandEvent(data.getName()));
         }
      };
      return new UserCommand(data.getName(), command);
   }
   
   private void registerUserCommand(RegisterUserCommandEvent event)
   {
      RegisterUserCommandEvent.Data data = event.getData();
      UserCommand command = createUserCommand(data);
      
      JsArrayString shortcuts = data.getShortcuts();
      for (int i = 0; i < shortcuts.length(); i++)
         addCommandBinding(shortcuts.get(i), command);
   }
   
   private boolean isPrefixMatch(String shortcut, String buffer)
   {
      // TODO: Splitting may be expensive when many bindings are available.
      // Should this be optimized to avoid string splits? (On the other hand,
      // splitting such tiny strings should still be very fast)
      String[] shortcutSplat = shortcut.split(" ");
      String[] bufferSplat = buffer.split(" ");
      
      // NOTE: We use '<=' to enforce 'strict' prefix matches; ie, exact
      // matches are not permitted (should have already been handled)
      if (shortcutSplat.length <= bufferSplat.length)
         return false;
      
      for (int i = 0; i < bufferSplat.length; i++)
         if (!shortcutSplat[i].equals(bufferSplat[i]))
            return false;
      
      return true;
   }
   
   private final HashMap<String, AppCommand> createCommandMap(Commands commands)
   {
      HashMap<String, AppCommand> kbdCommandMap = new HashMap<String, AppCommand>();
      
      // Get the set of all available commands, and then map them from
      // keyboard shortcut to command to execute.
      HashMap<String, AppCommand> commandMap = commands.getCommands();
      for (Map.Entry<String, AppCommand> entry : commandMap.entrySet())
      {
         String shortcut = entry.getValue().getShortcutRaw();
         if (shortcut == null)
            continue;
         kbdCommandMap.put(shortcut, entry.getValue());
      }
      
      return kbdCommandMap;
   }
   
   private void showKeyBuffer()
   {
      
   }
   
   public static String keyEventToString(NativeEvent event)
   {
      StringBuilder builder = new StringBuilder();
      
      if (event.getCtrlKey())
         builder.append("ctrl-");
      
      if (event.getAltKey())
         builder.append("alt-");
      
      if (event.getMetaKey())
         builder.append("cmd-");
      
      if (event.getShiftKey())
         builder.append("shift-");
      
      int keyCode = event.getKeyCode();
      
      // Don't duplicate modifier keys.
      if (KeyboardHelper.isModifierKey(keyCode))
         return builder.toString();
      
      builder.append(keyCodeToString(keyCode));
      return builder.toString();
   }
   
   public static final String keyCodeToString(int keyCode)
   {
      return keyCodeToString(keyCode, KEY_CODE_MAP);
   }
   
   private static final native String keyCodeToString(int keyCode, JavaScriptObject keyCodeMap)
   /*-{
      return keyCodeMap[keyCode] || String.fromCharCode(keyCode);
   }-*/;
   
   private static boolean debuggingEnabled()
   {
      return true;
   }
   
   private static final native JavaScriptObject makeKeyCodeMap()
   /*-{
      
      // Map array indices as key codes to corresponding name.
      var map = new Array(256);
      
      map[8] = "backspace";
      map[9] = "tab";
      map[12] = "numlock";
      map[13] = "enter";
      map[16] = "shift";
      map[17] = "ctrl";
      map[18] = "alt";
      map[19] = "pause";
      map[20] = "capslock";
      map[27] = "escape";
      map[33] = "pageup";
      map[34] = "pagedown";
      map[35] = "end";
      map[36] = "home";
      map[37] = "left";
      map[38] = "up";
      map[39] = "right";
      map[40] = "down";
      map[45] = "insert";
      map[46] = "delete";
      
      // Add in numbers 0-9
      for (var i = 48; i <= 57; i++)
         map[i] = "" + (i - 48);
         
      // Add in letters
      for (var i = 65; i <= 90; i++)
         map[i] = String.fromCharCode(i).toLowerCase();
         
      map[91] = "left.window";
      map[92] = "right.window";
      
      for (var i = 96; i <= 105; i++)
         map[i] = "numpad" + (i - 96);
      
      map[106] = "*";
      map[107] = "+";
      map[109] = "-";
      
     // NOTE: This is actually 'decimal point' which is
     // distinct as a keycode from '.', but probably easier
     // to just treat them the same.
      map[110] = "." 
      
      map[111] = "/";
      for (var i = 112; i <= 123; i++)
         map[i] = "f" + (i - 111);
      
      map[144] = "num.lock";
      map[145] = "scroll.lock";
      map[186] = ";";
      map[187] = "=";
      map[188] = ",";
      map[189] = "-";
      map[190] = ".";
      map[191] = "/";
      map[192] = "`";
      map[219] = "[";
      map[220] = "\\";
      map[221] = "]";
      map[222] = "'";
      
      return map;
   
   }-*/;
   
   // TODO: This should really be considered mutable, or we should
   // return some interface to the object.
   public Map<String, UserCommand> getCommandMap()
   {
      return commandMap_;
   }
   
   // NOTE: The 'commandMap_' below and the 'commandShortcuts_' array
   // must be synchronized.
   private final LinkedHashMap<String, UserCommand> commandMap_;
   private final ArrayList<String> commandShortcuts_;
   
   private final HandlerRegistration handler_;
   private String current_;
   private String buffer_;
   private final Timer bufferTimer_;
   
   // Injected members ----
   private final CodeToolsServerOperations server_;
   private final EventBus events_;
   
   // Static Initialization ----
   private static final JavaScriptObject KEY_CODE_MAP;
   private static final String[] MODIFIERS;
   static {
      KEY_CODE_MAP = makeKeyCodeMap();
      MODIFIERS = new String[] { "ctrl-", "alt-", "cmd-", "shift-" };
   }

}
