package org.rstudio.core.client.widget;

import java.util.List;

import org.rstudio.core.client.command.ShortcutInfo;
import org.rstudio.core.client.command.ShortcutManager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
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
      List<ShortcutInfo> shortcuts = ShortcutManager.INSTANCE.getActiveShortcutInfo();
      sb.appendHtmlConstant("<table><tr><td>");
      for (int i = 0; i < shortcuts.size(); i++)
      {
         ShortcutInfo info = shortcuts.get(i);
         if (info.getDescription() == null ||
             info.getShortcut() == null)
         {
            continue;
         }
         sb.appendHtmlConstant("<strong>");
         sb.appendHtmlConstant(info.getShortcut());
         sb.appendHtmlConstant("</strong>: ");
         sb.appendEscaped(info.getDescription());
         sb.appendHtmlConstant("</br>");
         if (i == Math.floor(shortcuts.size() / 2))
            sb.appendHtmlConstant("</td><td>");
      }
      sb.appendHtmlConstant("</td></tr></table>");
      HTMLPanel panel = new HTMLPanel(sb.toSafeHtml());
      shortcutPanel.add(panel);
   }

   @UiField 
   HTMLPanel shortcutPanel;
}
