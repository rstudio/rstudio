/*
 * ShortcutInfoPanel.java
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
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
package org.rstudio.core.client.widget;

import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.ShortcutInfo;
import org.rstudio.core.client.command.ShortcutManager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ShortcutInfoPanel extends Composite
{
   private static ShortcutInfoPanelUiBinder uiBinder = GWT
         .create(ShortcutInfoPanelUiBinder.class);

   interface ShortcutInfoPanelUiBinder extends
         UiBinder<Widget, ShortcutInfoPanel>
   {
   }

   public ShortcutInfoPanel(final Command onShowFullDocs)
   {
      initWidget(uiBinder.createAndBindUi(this));

      if (onShowFullDocs == null)
      {
         shortcutDocLink.setVisible(false);
      }
      else
      {
         shortcutDocLink.addClickHandler(new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               onShowFullDocs.execute();
            }
         });
      }
      
      headerLabel.setText(getHeaderText());
      shortcutPanel.add(getShortcutContent());
   }
   
   protected String getHeaderText()
   {
      return "Keyboard Shortcut Quick Reference";
   }
   
   protected Widget getShortcutContent()
   {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      List<ShortcutInfo> shortcuts = 
            ShortcutManager.INSTANCE.getActiveShortcutInfo();
      String[][] groupNames = { 
            new String[] { "Tabs", "Panes", "Files" },
            new String[] { "Source Navigation", "Execute" },
            new String[] { "Source Editor", "Debug" }, 
            new String[] { "Source Control", "Build", "Console", "Terminal", "Other" }
      };
      int pctWidth = 100 / groupNames.length;
      sb.appendHtmlConstant("<table width='100%'><tr>");
      for (String[] colGroupNames: groupNames)
      {
         sb.appendHtmlConstant("<td width='" + pctWidth + "%'>");
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
            if (colGroupName == "Panes")
            {
               sb.appendHtmlConstant("<p>Add Shift to zoom (maximize) pane.</p>");
            }
         }
         sb.appendHtmlConstant("</td>");
      }
      sb.appendHtmlConstant("</td></tr></table>");
      HTMLPanel panel = new HTMLPanel(sb.toSafeHtml());
      return panel;
   }
   
   public Element getRootElement()
   {
      return focusPanel.getElement();
   }
   
   @UiField HTMLPanel shortcutPanel;
   @UiField FocusPanel focusPanel;
   @UiField Anchor shortcutDocLink;
   @UiField Label headerLabel;
}
