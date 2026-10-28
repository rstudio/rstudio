/*
 * CommandShortcutReference.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.core.client.widget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.ShortcutInfo;
import org.rstudio.core.client.command.ShortcutManager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;

/**
 * The shortcuts declared in Commands.cmd.xml, grouped as declared there. Reflects
 * the platform, the editor keybindings, and customized shortcuts for commands
 * that have a default shortcut.
 */
public class CommandShortcutReference extends Composite
{
   public CommandShortcutReference()
   {
      HTML html = new HTML(buildHtml(ShortcutManager.INSTANCE.getActiveShortcutInfo()));
      html.addStyleName(ShortcutReferenceDialog.RES.styles().groups());
      initWidget(html);
   }

   // Groups appear in order of first use, which is declaration order for
   // ShortcutManager's list
   static SafeHtml buildHtml(List<ShortcutInfo> shortcuts)
   {
      Map<String, List<ShortcutInfo>> groups = new LinkedHashMap<>();
      for (ShortcutInfo info : shortcuts)
      {
         if (StringUtil.isNullOrEmpty(info.getDescription()) ||
             info.getDisplayedKeySequences().isEmpty() ||
             info.getGroupName().equals(HIDDEN_GROUP))
         {
            continue;
         }
         groups.computeIfAbsent(info.getGroupName(), key -> new ArrayList<>()).add(info);
      }

      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      for (Map.Entry<String, List<ShortcutInfo>> group : groups.entrySet())
      {
         sb.appendHtmlConstant("<h2>");
         sb.appendEscaped(groupLabel(group.getKey()));
         sb.appendHtmlConstant("</h2><table>");
         for (ShortcutInfo info : group.getValue())
         {
            sb.appendHtmlConstant("<tr><th scope=\"row\">");
            appendShortcuts(sb, info);
            sb.appendHtmlConstant("</th><td>");
            sb.appendEscaped(info.getDescription());
            sb.appendHtmlConstant("</td></tr>");
         }
         sb.appendHtmlConstant("</table>");

         if (group.getKey().equals("Panes"))
         {
            sb.appendHtmlConstant("<p>");
            sb.appendEscaped(constants_.addShiftPTag());
            sb.appendHtmlConstant("</p>");
         }
      }
      return sb.toSafeHtml();
   }

   private static void appendShortcuts(SafeHtmlBuilder sb, ShortcutInfo info)
   {
      boolean first = true;
      for (KeySequence keys : info.getDisplayedKeySequences())
      {
         if (!first)
            sb.appendHtmlConstant(", ");
         first = false;

         String pretty = keys.toString(true);
         String plain = keys.toString(false);
         sb.appendHtmlConstant("<kbd>");
         if (pretty.equals(plain))
         {
            sb.appendEscaped(plain);
         }
         else
         {
            // The pretty form uses symbols on macOS (e.g. for Command) and
            // abbreviations such as PgUp that screen readers may not announce
            // well; give them names instead. The symbols are HTML entities, but
            // custom key names come from the user's bindings file, so escape the rest.
            String spoken = BrowseCap.hasMetaKey() ? spokenMacForm(keys) : plain;
            sb.appendHtmlConstant("<span aria-hidden=\"true\">");
            sb.append(SafeHtmlUtils.fromTrustedString(
                  SafeHtmlUtils.htmlEscapeAllowEntities(pretty)));
            sb.appendHtmlConstant("</span>");
            sb.append(SafeHtmlUtil.visuallyHiddenText(spoken));
         }
         sb.appendHtmlConstant("</kbd>");
      }
   }

   // Names keys as KeySymbolLegend does, in macOS modifier order. The plain
   // form can't be used: it says Alt, Enter, and Backspace, and menu
   // accelerators depend on it.
   static String spokenMacForm(KeySequence keys)
   {
      List<String> combinations = new ArrayList<>();
      for (KeyCombination combination : keys.getData())
      {
         List<String> parts = new ArrayList<>();
         int modifiers = combination.getModifier();
         if ((modifiers & KeyboardShortcut.CTRL) != 0)
            parts.add(constants_.keySymbolControl());
         if ((modifiers & KeyboardShortcut.ALT) != 0)
            parts.add(constants_.keySymbolOption());
         if ((modifiers & KeyboardShortcut.SHIFT) != 0)
            parts.add(constants_.keySymbolShift());
         if ((modifiers & KeyboardShortcut.META) != 0)
            parts.add(constants_.keySymbolCommand());
         parts.add(macKeyName(combination));
         combinations.add(StringUtil.joinStrings(parts, "+"));
      }
      return StringUtil.joinStrings(combinations, " ");
   }

   private static String macKeyName(KeyCombination combination)
   {
      switch (combination.getKeyCode())
      {
         case KeyCodes.KEY_ENTER:     return constants_.keySymbolReturn();
         case KeyCodes.KEY_TAB:       return constants_.keySymbolTab();
         case KeyCodes.KEY_BACKSPACE: return constants_.keySymbolDelete();
         case KeyCodes.KEY_DELETE:    return constants_.keySymbolForwardDelete();
         default:                     return combination.getKeyName(false);
      }
   }

   private static String groupLabel(String group)
   {
      switch (group)
      {
         case "Source Editor":      return constants_.sourceEditorGroupName();
         case "Editing":            return constants_.editingGroupName();
         case "Completion":         return constants_.completionGroupName();
         case "Source Navigation":  return constants_.sourceNavigationGroupName();
         case "View":               return constants_.viewGroupName();
         case "Panes":              return constants_.panesGroupName();
         case "Tabs":               return constants_.tabsGroupName();
         case "Files":              return constants_.filesGroupName();
         case "Build":              return constants_.buildGroupName();
         case "Execute":            return constants_.executeGroupName();
         case "Debug":              return constants_.debugGroupName();
         case "Source Control":     return constants_.sourceControlGroupName();
         case "Other":              return constants_.otherGroupName();
         case "Console":            return constants_.consoleGroupName();
         case "Terminal":           return constants_.terminalGroupName();
         case "Main Menu (Server)": return constants_.mainMenuGroupName();
         case "Accessibility":      return constants_.accessibilityGroupName();
         default:
            // shown untranslated; add a case and a CoreClientConstants entry
            Debug.logWarning("Keyboard shortcut group '" + group +
                  "' from Commands.cmd.xml has no translated label");
            return group;
      }
   }

   private static final String HIDDEN_GROUP = "Not Displayed";
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
