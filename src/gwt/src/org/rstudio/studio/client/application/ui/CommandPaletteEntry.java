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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class CommandPaletteEntry extends Composite
{

   private static CommandPaletteEntryUiBinder uiBinder = GWT
         .create(CommandPaletteEntryUiBinder.class);

   interface CommandPaletteEntryUiBinder extends UiBinder<Widget, CommandPaletteEntry>
   {
   }

   public interface Styles extends CssResource
   {
      String entry();
      String selected();
   }

   public CommandPaletteEntry(AppCommand command)
   {
      command_ = command;
      
      initWidget(uiBinder.createAndBindUi(this));
      String label = command.getLabel();
      if (StringUtil.isNullOrEmpty(label))
         label = command.getButtonLabel();
      if (StringUtil.isNullOrEmpty(label))
         label = command.getDesc();
      name_.setText(label);
      shortcut_.getElement().setInnerHTML(command.getShortcutPrettyHtml());
   }
   
   public String getText()
   {
      return name_.getText();
   }
   
   public void setSearchHighlight(String text)
   {
      
   }
   
   public void setSelected(boolean selected)
   {
      if (selected)
         addStyleName(styles_.selected());
      else
         removeStyleName(styles_.selected());
   }
   
   public void invoke()
   {
      command_.execute();
   }
   
   public String getId()
   {
      return command_.getId();
   }

   final AppCommand command_;
   @UiField public Label name_;
   @UiField public Label shortcut_;
   @UiField public Styles styles_;
}
