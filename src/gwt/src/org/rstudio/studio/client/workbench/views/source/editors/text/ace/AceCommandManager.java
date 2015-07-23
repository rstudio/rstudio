package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut.KeyCombination;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.js.JsObject;

public class AceCommandManager extends JavaScriptObject
{
   protected AceCommandManager() {}
   
   public static native final JsArray<AceCommand> getDefaultCommands()
   /*-{
      return $wnd.require("ace/commands/default_commands").commands;
   }-*/;
   
   public static final JsArray<AceCommand> getRelevantCommands()
   {
      JsArray<AceCommand> allCommands = getDefaultCommands();
      JsArray<AceCommand> filtered = JavaScriptObject.createArray().cast();
      for (int i = 0; i < allCommands.length(); i++)
      {
         AceCommand command = allCommands.get(i);
         String name = command.getInternalName();
         if (!EXCLUDED_COMMANDS_MAP.hasKey(name))
         {
            filtered.push(command);
         }
      }
      
      return filtered;
      
   }
   
   public final native boolean hasCommand(String id) /*-{
      return this.byName[id] != null;
   }-*/;
   
   public final boolean hasBinding(KeySequence keys)
   {
      String transformed = toAceStyleShortcutString(keys);
      return hasBinding(transformed);
   }
   
   public final boolean hasPrefix(KeySequence keys)
   {
      String transformed = toAceStyleShortcutString(keys);
      return hasPrefix(transformed);
   }
   
   public final native JsObject getCommandKeyBindings() /*-{
      return this.commandKeyBinding;
   }-*/;
   
   public final native JsObject getCommands() /*-{
      return this.commands;
   }-*/;
   
   private static final String toAceStyleShortcutString(KeyCombination keys)
   {
      StringBuilder builder = new StringBuilder();
      
      if (keys.isCtrlPressed()) builder.append("ctrl-");
      if (keys.isMetaPressed()) builder.append("cmd-");
      if (keys.isAltPressed()) builder.append("alt-");
      if (keys.isShiftPressed()) builder.append("shift-");
      
      String keyName =
            KeyboardHelper.keyNameFromKeyCode(keys.getKeyCode());
      builder.append(keyName.toLowerCase());
      return builder.toString();
   }
   
   private static final String toAceStyleShortcutString(KeySequence sequence)
   {
      if (sequence.size() == 0)
         return "";
      
      StringBuilder builder = new StringBuilder();
      builder.append(toAceStyleShortcutString(sequence.get(0)));
      for (int i = 1; i < sequence.size(); i++)
      {
         builder.append(" ");
         builder.append(toAceStyleShortcutString(sequence.get(i)));
      }
      
      return builder.toString();
      
   }
   
   private final native boolean hasBinding(String shortcut) /*-{
      var binding = this.commandKeyBinding[shortcut];
      return binding != null && binding !== "chainKeys";
   }-*/;
   
   private final native boolean hasPrefix(String shortcut) /*-{
      var binding = this.commandKeyBinding[shortcut];
      return binding != null && binding === "chainKeys";
   }-*/;
   
   public final void rebindCommand(String id, KeySequence keys)
   {
      rebindCommand(id, toAceStyleShortcutString(keys));
   }
   
   private final native void rebindCommand(String id, String keys)
   /*-{
      var command = this.byName[id];
      
      if (command == null) {
         throw new Error("No command with id '" + id + "'");
      }
      
      command.bindKey = keys;
      this.addCommand(command);
   }-*/;
   
   private static final JsObject EXCLUDED_COMMANDS_MAP =
         makeExcludedCommandsMap();
   
   private static final native JsObject makeExcludedCommandsMap()
   /*-{
      
      var bad = [
         "showSettingsMenu", "goToNextError", "goToPreviousError",
         "togglerecording", "replaymacro", "passKeysToBrowser",
         "copy", "cut", "cut_or_delete", "paste", "replace",
         "insertstring", "inserttext"
      ];
      
      var map = {};
      for (var i = 0; i < bad.length; i++) {
         map[bad[i]] = true;
      }
      
      return map;
      
   }-*/;
   
}
