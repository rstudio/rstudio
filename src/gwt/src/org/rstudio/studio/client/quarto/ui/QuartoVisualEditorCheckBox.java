/*
 * QuartoVisualEditorCheckBox.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


package org.rstudio.studio.client.quarto.ui;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.widget.HelpButton;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HorizontalPanel;

import elemental2.dom.URL;
import org.rstudio.studio.client.quarto.QuartoConstants;

public class QuartoVisualEditorCheckBox extends Composite implements HasValue<Boolean>
{
   public QuartoVisualEditorCheckBox()
   {
      chkVisualEditor_ = new CheckBox(constants_.chkVisualEditorLabel());
      
      HorizontalPanel editorPanel = HelpButton.checkBoxWithHelp(chkVisualEditor_,
            new HelpButton(new URL("https://quarto.org/docs/visual-editor/"), constants_.aboutHelpButtonTitle()));
      
      initWidget(editorPanel);
   }

   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler)
   {
      return chkVisualEditor_.addValueChangeHandler(handler);
   }

   @Override
   public Boolean getValue()
   {
      return chkVisualEditor_.getValue();
   }

   @Override
   public void setValue(Boolean value)
   {
      chkVisualEditor_.setValue(value);
      
   }

   @Override
   public void setValue(Boolean value, boolean fireEvents)
   {
      chkVisualEditor_.setValue(value, fireEvents);
   }
   
   private CheckBox chkVisualEditor_;

   private static final QuartoConstants constants_ = GWT.create(QuartoConstants.class);
}
