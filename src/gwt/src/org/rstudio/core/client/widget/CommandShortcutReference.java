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

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.ShortcutInfo;
import org.rstudio.core.client.command.ShortcutManager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;

/**
 * The shortcuts currently in effect, grouped as in Commands.cmd.xml. Reflects
 * the platform, the editor keybindings, and any customized shortcuts.
 */
public class CommandShortcutReference extends Composite
{
   public CommandShortcutReference()
   {
      HTML html = new HTML(buildHtml());
      html.addStyleName(ShortcutReferenceDialog.RES.styles().groups());
      initWidget(html);
   }

   private static SafeHtml buildHtml()
   {
      // Shortcut info arrives in declaration order, so groups keep that order too
      Map<String, List<ShortcutInfo>> groups = new LinkedHashMap<>();
      for (ShortcutInfo info : ShortcutManager.INSTANCE.getActiveShortcutInfo())
      {
         if (StringUtil.isNullOrEmpty(info.getDescription()) ||
             info.getShortcuts(false).isEmpty() ||
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
      List<String> pretty = info.getShortcuts(true);
      List<String> plain = info.getShortcuts(false);
      for (int i = 0; i < plain.size(); i++)
      {
         if (i > 0)
            sb.appendHtmlConstant(", ");

         sb.appendHtmlConstant("<kbd>");
         if (pretty.get(i).equals(plain.get(i)))
         {
            sb.appendEscaped(plain.get(i));
         }
         else
         {
            // On macOS the pretty form uses symbols (e.g. for Command) that
            // screen readers may not announce; give them the spelled-out form
            sb.appendHtmlConstant("<span aria-hidden=\"true\">" + pretty.get(i) + "</span>");
            sb.append(SafeHtmlUtil.visuallyHiddenText(plain.get(i)));
         }
         sb.appendHtmlConstant("</kbd>");
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
         default:                   return group;
      }
   }

   private static final String HIDDEN_GROUP = "Not Displayed";
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
