package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;

public class AceCommandManager extends JavaScriptObject
{
   protected AceCommandManager() {}
   
   public static final AceCommandManager create()
   {
      return _createImpl(BrowseCap.isWindows());
   }
   
   public static final native AceCommandManager _createImpl(boolean isWindows)
   /*-{
      var CommandManager = $wnd.require("ace/commands/command_manager").CommandManager;
      var commands = $wnd.require("ace/commands/default_commands").commands;
      var platform = isWindows ? "win" : "mac";
      return new CommandManager(platform, commands);
   }-*/;
   
   public static native final JsArray<AceCommand> getDefaultCommands()
   /*-{
      return $wnd.require("ace/commands/default_commands").commands;
   }-*/;
   
   public final JsArray<AceCommand> getRelevantCommands()
   {
      JsObject allCommands = getCommands();
      JsArray<AceCommand> filtered = JavaScriptObject.createArray().cast();
      JsArrayString keys = allCommands.keys();
      for (String key : JsUtil.asIterable(keys))
      {
         AceCommand command = allCommands.getObject(key);
         String name = command.getInternalName();
         if (!EXCLUDED_COMMANDS_MAP.hasKey(name))
         {
            filtered.push(command);
         }
      }
      
      return sorted(filtered);
   }
   
   private static final native JsArray<AceCommand> sorted(JsArray<AceCommand> commands) /*-{
      var clone = commands.slice();
      clone.sort(function(o1, o2) {
         var n1 = o1.name;
         var n2 = o2.name;
         
         if (n1 == null)
            return 1;
         else if (n2 == null)
            return -1;
         
         return n1 < n2 ? -1 : 1;
      });
      return clone;
   }-*/;
   
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
   
   public final native boolean exec(String command, AceEditorNative editor) /*-{
      return this.exec(command, editor);
   }-*/;
   
   public final native boolean exec(String command, AceEditorNative editor, String arg) /*-{
      return this.exec(command, editor, arg);
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
   
   public final void rebindCommand(String id, List<KeySequence> keys)
   {
      JsArrayString shortcuts = JavaScriptObject.createArray().cast();
      for (KeySequence ks : keys)
         shortcuts.push(toAceStyleShortcutString(ks));
      rebindCommand(id, shortcuts);
   }
   
   private final native void rebindCommand(String id, JsArrayString keys)
   /*-{
      var command = this.byName[id];

      // The command can be null if it's an excluded command, or
      // the user has manually edited their keybinding file and
      // selected the ID of a non-existent command.
      if (command == null)
         return;

      var newCommand = @org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommandManager::cloneCommand(Lcom/google/gwt/core/client/JavaScriptObject;)(command);

      newCommand.bindKey = keys.join("|");
      newCommand.isCustom = true;

      this.addCommand(newCommand);
      
      // Refresh the 'chainKeys' fields. This is necessary
      // to ensure that Ace can dispatch to commands requiring
      // multiple key combinations.
      for (var i = 0; i < keys.length; i++) {
         var keySequence = keys[i];
         var splat = keySequence.split(" ");
         if (splat.length <= 1)
            continue;
         
         var field = splat[0];
         this.commandKeyBinding[field] = "chainKeys";
         for (var j = 1; j < splat.length - 1; j++) {
            field += " " + splat[i];
            this.commandKeyBinding[field] = "chainKeys";
         }
      }
      
   }-*/;
   
   public final native void addCommand(AceCommand command) /*-{
      this.addCommand(command);
   }-*/;

   // Clone a command object. Ace's default command objects are shared by
   // every editor instance, so install modified clones rather than mutating
   // the originals -- both so other editors are unaffected and so the
   // defaults remain available for reset.
   private static final native JavaScriptObject cloneCommand(JavaScriptObject command)
   /*-{
      var clone = {};
      for (var key in command) {
         if (command.hasOwnProperty(key)) {
            clone[key] = command[key];
         }
      }
      return clone;
   }-*/;

   /**
    * Make the line start / line end navigation commands act on document lines
    * rather than soft-wrapped screen rows.
    *
    * By default, Ace treats each visual row of a soft-wrapped line as its own
    * line, so Home / End (and, on macOS, Ctrl+A / Ctrl+E) stop at the wrap
    * boundary. That's the right behavior for a text editor, but not for a
    * console, where the whole wrapped command should behave as a single line
    * the way it does under readline.
    *
    * Command tables are per-editor, so this only affects the editor whose
    * command manager this is.
    *
    * https://github.com/rstudio/rstudio/issues/18447
    */
   public final native void useDocumentLineNavigation()
   /*-{
      var self = this;

      var override = function(name, exec) {
         var command = self.byName[name];

         // The names below are Ace built-ins, so a missing one means an Ace
         // update renamed or removed it. Warn rather than silently skipping
         // part of the override: a partial application (say, navigation
         // updated but selection not) would leave the two disagreeing.
         if (command == null) {
            @org.rstudio.core.client.Debug::logWarning(Ljava/lang/String;)("useDocumentLineNavigation: Ace command '" + name + "' not found");
            return;
         }

         var clone = @org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommandManager::cloneCommand(Lcom/google/gwt/core/client/JavaScriptObject;)(command);
         clone.exec = exec;
         self.addCommand(clone);
      };

      var lineStart = function(editor) {
         return { row: editor.getCursorPosition().row, column: 0 };
      };

      var lineEnd = function(editor) {
         var row = editor.getCursorPosition().row;
         return { row: row, column: editor.getSession().getLine(row).length };
      };

      var navigateToLineStart = function(editor) {
         var position = lineStart(editor);
         editor.navigateTo(position.row, position.column);
      };

      var navigateToLineEnd = function(editor) {
         var position = lineEnd(editor);
         editor.navigateTo(position.row, position.column);
      };

      var selectToLineStart = function(editor) {
         var position = lineStart(editor);
         editor.getSelection().selectTo(position.row, position.column);
      };

      var selectToLineEnd = function(editor) {
         var position = lineEnd(editor);
         editor.getSelection().selectTo(position.row, position.column);
      };

      override("gotolinestart", navigateToLineStart);
      override("gotolineend", navigateToLineEnd);

      // Ace has two equivalent pairs of selection commands here: 'selectto*'
      // (Cmd+Shift+Left / Ctrl+Shift+A and friends) and 'select*' (Shift+Home /
      // Shift+End). Which one a given key reaches varies by platform, so both
      // have to move in step with the navigation commands above.
      override("selecttolinestart", selectToLineStart);
      override("selecttolineend", selectToLineEnd);
      override("selectlinestart", selectToLineStart);
      override("selectlineend", selectToLineEnd);
   }-*/;
   
   private static final JsObject EXCLUDED_COMMANDS_MAP =
         makeExcludedCommandsMap();
   
   private static final native JsObject makeExcludedCommandsMap()
   /*-{
      
      var excludedCommands = [
         "showSettingsMenu", "goToNextError", "goToPreviousError",
         "togglerecording", "replaymacro", "passKeysToBrowser",
         "copy", "cut", "cut_or_delete", "paste", "replace",
         "insertstring", "inserttext", "gotoline", "jumptomatching",
         "backspace", "delete", "togglecomment", "toggleBlockComment"
      ];
      
      var map = {};
      for (var i = 0; i < excludedCommands.length; i++) {
         map[excludedCommands[i]] = true;
      }
      
      return map;
      
   }-*/;
   
}
