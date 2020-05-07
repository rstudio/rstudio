/*
 * CommandPaletteEntry.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.application.ui;

import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeySequence;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public abstract class CommandPaletteEntry extends Composite
{

   private static CommandPaletteEntryUiBinder uiBinder = GWT
         .create(CommandPaletteEntryUiBinder.class);

   interface CommandPaletteEntryUiBinder extends UiBinder<Widget, CommandPaletteEntry>
   {
   }

   public interface Styles extends CssResource
   {
      String entry();
      String keyboard();
      String searchMatch();
      String selected();
   }

   public CommandPaletteEntry(List<KeySequence> keys)
   {
      initWidget(uiBinder.createAndBindUi(this));
      keys_ = keys;
   }

   private void appendKey(SafeHtmlBuilder b, String key)
   {
      b.appendHtmlConstant("<span class=\"" + 
             styles_.keyboard() + "\">");
      b.appendHtmlConstant(key);
      b.appendHtmlConstant("</span>");
   }

   public void initialize()
   {
      name_.setText(getLabel());
      SafeHtmlBuilder b = new SafeHtmlBuilder();
      for (KeySequence k: keys_)
      {
         List<KeyCombination> combos = k.getData();
         for (int i = 0; i < combos.size(); i++)
         {
            KeyCombination combo = combos.get(i);
            if (combo.isCtrlPressed())
               appendKey(b, "Ctrl");
            if (combo.isAltPressed())
               appendKey(b, "Alt");
            if (combo.isShiftPressed())
               appendKey(b, "Shift");
            if (combo.isMetaPressed())
               appendKey(b, BrowseCap.hasMetaKey() ? "&#8984;" : "Cmd");
            appendKey(b, combo.key());
            
            // Is this a multi-key sequence?
            if (i < (combos.size() - 1))
            {
               b.appendEscaped(",");
            }
         }
         break;
      }
      shortcut_.getElement().setInnerSafeHtml(b.toSafeHtml());
      
      String context = getContext();
      if (StringUtil.isNullOrEmpty(context))
      {
         context_.setVisible(false);
      }
      else
      {
         context_.setText(context);
         context_.setVisible(true);
      }
   }
   
   public void setSelected(boolean selected)
   {
      if (selected)
         addStyleName(styles_.selected());
      else
         removeStyleName(styles_.selected());
   }
   
   public void setSearchHighlight(String text)
   {
      String label = getLabel();
      int idx = label.toLowerCase().indexOf(text.toLowerCase());
      if (idx >= 0)
      {
         SafeHtmlBuilder b = new SafeHtmlBuilder();
         b.appendEscaped(label.substring(0, idx));
         b.appendHtmlConstant("<span class=\"" + styles_.searchMatch() + "\">");
         b.appendEscaped(label.substring(idx, idx + text.length()));
         b.appendHtmlConstant("</span>");
         b.appendEscaped(label.substring(idx + text.length(), label.length()));
         name_.getElement().setInnerSafeHtml(b.toSafeHtml());
      }
      else
      {
         name_.setText(label);
      }
   }
   
   abstract public String getLabel();
   abstract public void invoke();
   abstract public String getId();
   abstract public String getContext();
   
   private final List<KeySequence> keys_;

   @UiField public Label context_;
   @UiField public Label name_;
   @UiField public Label shortcut_;
   @UiField public Styles styles_;
}
