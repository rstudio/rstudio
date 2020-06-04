/*
 * UserPrefEnumPaletteEntry.java
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
package org.rstudio.studio.client.palette.ui;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.studio.client.palette.UserPrefPaletteItem;
import org.rstudio.studio.client.palette.model.CommandPaletteItem.InvocationSource;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.IntValue;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class UserPrefIntegerPaletteEntry extends UserPrefPaletteEntry
{
   public UserPrefIntegerPaletteEntry(IntValue val, UserPrefPaletteItem item)
   {
      super(val, item);
      val_ = val;
      
      panel_ = new SimplePanel();

      label_ = new Label();
      label_.setText(val.getGlobalValue().toString());
      label_.addStyleName("rstudio-fixed-width-font");
      label_.getElement().getStyle().setFontSize(9, Unit.PT);
      label_.getElement().getStyle().setMarginRight(8, Unit.PX);
      panel_.add(label_);
      
      text_ = new NumericTextBox();
      text_.getElement().addClassName("rstudio-fixed-width-font");
      
      text_.addBlurHandler((evt) ->
      {
         // End editing mode when the textbox loses focus
         endEdit();
      });
      
      text_.addKeyDownHandler((evt) ->
      {
         switch (evt.getNativeKeyCode())
         {
         case KeyCodes.KEY_ENTER:
            commit();
            break;
         case KeyCodes.KEY_ESCAPE:
            endEdit();
            break;
         }
      });

      Style style = text_.getElement().getStyle();
      style.setFontSize(8, Unit.PT);
      style.setWidth(50, Unit.PX);
      style.setMarginRight(8, Unit.PX);
      style.setMarginBottom(0, Unit.PX);
      editing_ = false;
      
      initialize();
      
      Roles.getTextboxRole().setAriaLabelledbyProperty(text_.getElement(), 
            Id.of(name_.getElement()));
   }

   @Override
   public Widget getInvoker()
   {
      return panel_;
   }
   
   public void invoke(InvocationSource source)
   {
      if (editing_)
      {
         commit();
      }
      else
      {
         beginEdit();
      }
   }
   
   private void commit()
   {
      // If already editing the value, invoking performs a commit.
      String val = text_.getValue();
      if (!val.isEmpty())
      {
         int newVal = StringUtil.parseInt(val, -1);
         if (newVal >= 0)
         {
            val_.setGlobalValue(newVal);
            label_.setText(val);
         }
      }
      
      endEdit();
   }
   
   private void beginEdit()
   {
      panel_.remove(label_);
      text_.setValue(val_.getGlobalValue().toString());
      panel_.add(text_);
      text_.setFocus(true);
      
      editing_ = true;
   }
   
   private void endEdit()
   {
      panel_.remove(text_);
      panel_.add(label_);

      editing_ = false;
   }
   
   private final SimplePanel panel_;
   private final TextBox text_;
   private final Label label_;
   private final IntValue val_;
   private boolean editing_;
}
