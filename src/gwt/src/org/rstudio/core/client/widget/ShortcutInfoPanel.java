package org.rstudio.core.client.widget;

import java.util.List;

import org.rstudio.core.client.command.ShortcutInfo;
import org.rstudio.core.client.command.ShortcutManager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
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
         sb.appendEscaped(info.getGroupName() + " - " + info.getDescription());
         sb.appendHtmlConstant("</br>");
         if ((i + 1) % Math.floor(shortcuts.size() / 3) == 0)
            sb.appendHtmlConstant("</td><td>");
      }
      sb.appendHtmlConstant("</td></tr></table>");
      HTMLPanel panel = new HTMLPanel(sb.toSafeHtml());
      shortcutPanel.add(panel);
   }
   
   public void addCloseHandler(final Operation operation)
   {
      focusPanel.addKeyPressHandler(new KeyPressHandler()
      {
         @Override
         public void onKeyPress(KeyPressEvent event)
         {
            operation.execute();
         }
      });
      
      focusPanel.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            operation.execute();
         }
      });
      
      focusPanel.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            operation.execute();
         }
      });
   }

   @UiField HTMLPanel shortcutPanel;
   @UiField FocusPanel focusPanel;
}
