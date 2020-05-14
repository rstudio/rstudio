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
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeySequence;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.aria.client.SelectedValue;
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
      selected_ = false;
      
      Roles.getOptionRole().set(getElement());
      Roles.getOptionRole().setAriaSelectedState(getElement(), SelectedValue.FALSE);
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
      String id = getId();
      if (id != null)
      {
         // Assign a unique element ID (for accessibility tree). There's no need
         // to do this if there's no ID as we'll ultimately discard widgets
         // which don't have an addressable ID.
         ElementIds.assignElementId(getElement(), ElementIds.COMMAND_ENTRY_PREFIX + 
               ElementIds.idSafeString(id));
      }

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
         context_.getElement().setInnerHTML(context);
         context_.setVisible(true);
      }
   }
   
   /*
    * Set whether or not the command should appear selected.
    */
   public void setSelected(boolean selected)
   {
      // No-op if we're not changing state
      if (selected_ == selected)
         return;
      
      // Add the CSS class indicating that this entry is selected
      if (selected)
         addStyleName(styles_.selected());
      else
         removeStyleName(styles_.selected());

      // Update ARIA state to indicate that we're selected. (The host is
      // responsible for updating other ARIA state such as active descendant.)
      Roles.getOptionRole().setAriaSelectedState(getElement(), 
            selected ? SelectedValue.TRUE : SelectedValue.FALSE);
      
      selected_ = selected;
   }
   
   /**
    * Highlights the given keywords on the command entry.
    */
   public void setSearchHighlight(String[] keywords)
   {
      if (keywords.length == 0)
      {
         name_.setText(getLabel());
      }
      else
      {
         SafeHtmlBuilder sb = new SafeHtmlBuilder();
         SafeHtmlUtil.highlightSearchMatch(sb, getLabel(), keywords, 
               styles_.searchMatch());
         name_.getElement().setInnerSafeHtml(sb.toSafeHtml());
      }
   }
   
   abstract public String getLabel();
   abstract public void invoke();
   abstract public String getId();
   abstract public String getContext();
   
   private final List<KeySequence> keys_;
   private boolean selected_;

   @UiField public Label context_;
   @UiField public Label name_;
   @UiField public Label shortcut_;
   @UiField public Styles styles_;
}
