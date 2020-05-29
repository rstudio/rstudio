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
package org.rstudio.studio.client.application.ui;

import org.rstudio.studio.client.workbench.prefs.model.Prefs.IntValue;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class UserPrefIntegerPaletteEntry extends UserPrefPaletteEntry
{
   public UserPrefIntegerPaletteEntry(IntValue val)
   {
      super(val);
      val_ = val;
      
      panel_ = new SimplePanel();

      label_ = new Label();
      label_.setText(val.getGlobalValue().toString());
      panel_.add(label_);
      
      text_ = new TextBox();
      text_.setValue(val_.getGlobalValue().toString());
      text_.getElement().addClassName("rstudio-fixed-width-font");

      Style style = text_.getElement().getStyle();
      style.setFontSize(8, Unit.PT);
      style.setTextAlign(TextAlign.RIGHT);
      style.setWidth(50, Unit.PX);

      initialize();
   }

   @Override
   public Widget getInvoker()
   {
      return panel_;
   }
   
   private final SimplePanel panel_;
   private final TextBox text_;
   private final Label label_;
   private final IntValue val_;
}
