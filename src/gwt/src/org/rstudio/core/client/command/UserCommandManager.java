package org.rstudio.core.client.command;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.remote.ExecuteUserCommandEvent;
import org.rstudio.studio.client.server.remote.RegisterUserCommandEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import java.util.HashMap;
import java.util.Map;

public class UserCommandManager
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
   
   public UserCommandManager(ShortcutManager manager)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      manager_ = manager;
      commandMap_ = new HashMap<KeyboardShortcut, Command>();
      
      events_.addHandler(
            RegisterUserCommandEvent.TYPE,
            new RegisterUserCommandEvent.Handler()
            {
               @Override
               public void onRegisterUserCommand(RegisterUserCommandEvent event)
               {
                  UserCommandManager.this.onRegisterUserCommand(event);
               }
            });
   }
   
   @Inject
   public void initialize(EventBus events)
   {
      events_ = events;
   }
   
   public boolean dispatch(KeyboardShortcut shortcut)
   {
      if (commandMap_.containsKey(shortcut))
      {
         Command command = commandMap_.get(shortcut);
         command.execute();
         return true;
      }
      
      return false;
   }
   
   private KeySequence parseShortcutString(String shortcutString)
   {
      KeySequence sequence = new KeySequence();
      String[] splat = shortcutString.split("\\s+");
      for (int i = 0; i < splat.length; i++)
      {
         String sc = splat[i];
         
         int modifiers = KeyboardShortcut.NONE;
         if (sc.indexOf("ctrl") != -1)
            modifiers |= KeyboardShortcut.CTRL;
         if (sc.indexOf("alt") != -1)
            modifiers |= KeyboardShortcut.ALT;
         if (sc.indexOf("shift") != -1)
            modifiers |= KeyboardShortcut.SHIFT;
         if (sc.indexOf("meta") != -1 || sc.indexOf("cmd") != -1)
            modifiers |= KeyboardShortcut.META;
         
         int keyCode = 0;
         if (sc.endsWith("-"))
         {
            keyCode = '-';
         }
         else
         {
            String[] keySplit = sc.split("[-]");
            String keyName = keySplit[keySplit.length - 1];
            
            keyCode = KeyboardHelper.keyCodeFromKeyName(keyName);
            Debug.logToRConsole("Key name: '" + keyName + "'");
            Debug.logToRConsole("Key code: '" + keyCode + "'");
         }
            
         sequence.add(keyCode, modifiers);
      }
      
      Debug.logToRConsole("Parsed shortcut string: '" + shortcutString + "' -> '" + sequence.toString() + "'");
      return sequence;
   }
   
   private void onRegisterUserCommand(RegisterUserCommandEvent event)
   {
      final String name = event.getData().getName();
      JsArrayString shortcutStrings = event.getData().getShortcuts();
      
      for (int i = 0; i < shortcutStrings.length(); i++)
      {
         String shortcutString = shortcutStrings.get(i);
         KeySequence sequence = parseShortcutString(shortcutString);
         assert sequence != null : "Failed to parse string '" + shortcutString + "'";
         
         KeyboardShortcut shortcut = new KeyboardShortcut(sequence);
         Command command = new Command() {
            @Override
            public void execute()
            {
               events_.fireEvent(new ExecuteUserCommandEvent(name));
            }
         };
         
         Debug.logToRConsole("Registered shortcut '" + shortcutString + "'");
         commandMap_.put(shortcut, command);
      }
   }
   
   private final ShortcutManager manager_;
   private final Map<KeyboardShortcut, Command> commandMap_;
   
   // Injected ----
   private EventBus events_;
}
