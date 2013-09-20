package org.rstudio.core.client.widget;

import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.ShortcutInfo;
import org.rstudio.core.client.command.ShortcutManager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class ShortcutInfoPanel extends Composite
{
   private static ShortcutInfoPanelUiBinder uiBinder = GWT
         .create(ShortcutInfoPanelUiBinder.class);

   interface ShortcutInfoPanelUiBinder extends
         UiBinder<Widget, ShortcutInfoPanel>
   {
   }

   public ShortcutInfoPanel()
   {
      initWidget(uiBinder.createAndBindUi(this));
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      List<ShortcutInfo> shortcuts = 
            ShortcutManager.INSTANCE.getActiveShortcutInfo();
      String[][] groupNames = { 
            new String[] { "Tabs/Panes", "Files", "Console" },
            new String[] { "Source Navigation", "Source Editor" },
            new String[] { "Execute", "Debug", "Build" }, 
            new String[] { "Source Control", "Other" }
      };
      sb.appendHtmlConstant("<table><tr>");
      for (String[] colGroupNames: groupNames)
      {
         sb.appendHtmlConstant("<td>");
         for (String colGroupName: colGroupNames)
         {
            sb.appendHtmlConstant("<h2>");
            sb.appendEscaped(colGroupName);
            sb.appendHtmlConstant("</h2><table>");
            for (int i = 0; i < shortcuts.size(); i++)
            {
               ShortcutInfo info = shortcuts.get(i);
               if (info.getDescription() == null ||
                   info.getShortcuts().size() == 0 || 
                   !info.getGroupName().equals(colGroupName))
               {
                  continue;
               }
               sb.appendHtmlConstant("<tr><td><strong>");
               sb.appendHtmlConstant(
                     StringUtil.joinStrings(info.getShortcuts(), ", "));
               sb.appendHtmlConstant("</strong></td><td>");
               sb.appendEscaped(info.getDescription());
               sb.appendHtmlConstant("</td></tr>");
            }
            sb.appendHtmlConstant("</table>");
         }
         sb.appendHtmlConstant("</td>");
      }
      sb.appendHtmlConstant("</td></tr></table>");
      HTMLPanel panel = new HTMLPanel(sb.toSafeHtml());
      shortcutPanel.add(panel);
   }
   
   @UiField HTMLPanel shortcutPanel;
   @UiField FocusPanel focusPanel;
}
