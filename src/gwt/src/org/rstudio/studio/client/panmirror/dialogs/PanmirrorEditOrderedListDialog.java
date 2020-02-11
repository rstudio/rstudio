/*
 * PanmirrorEditOrderedListDialog.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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


package org.rstudio.studio.client.panmirror.dialogs;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorListCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorOrderedListProps;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditOrderedListDialog extends ModalDialog<PanmirrorOrderedListProps>
{
   public PanmirrorEditOrderedListDialog(PanmirrorOrderedListProps props,
                                         PanmirrorListCapabilities capabilities,
                                         OperationWithInput<PanmirrorOrderedListProps> operation)
   {
      super("Ordered List", Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
   
      capabilities_ = capabilities;
      
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      
      startingNumber_.setMin(1);
      
      startingNumber_.setVisible(capabilities.order);
      numberStyle_.setVisible(capabilities.fancy);
      numberDelimiter_.setVisible(capabilities.fancy);
      
      numberStyle_.setChoices(new String[] {
         "DefaultStyle",
         "Decimal",
         "LowerRoman",
         "UpperRoman",
         "LowerAlpha",
         "UpperAlpha",
         "Example",    
      });
      numberDelimiter_.setChoices(new String[] {
         "DefaultDelim",
         "Period",
         "OneParen",
         "TwoParens",   
      });
      
      startingNumber_.setValue(props.order + "");
      
      numberStyle_.setValue(props.number_style);
      
      numberDelimiter_.setValue(props.number_delim);
      tight_.setValue(props.tight);
      
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected PanmirrorOrderedListProps collectInput()
   {
      PanmirrorOrderedListProps result = new PanmirrorOrderedListProps();
      result.order = StringUtil.parseInt(startingNumber_.getValue(), 1);
      result.number_style = capabilities_.fancy ? numberStyle_.getValue() : "DefaultStyle";
      result.number_delim = capabilities_.fancy ? numberDelimiter_.getValue() : "DefaultDelim";
      result.tight = tight_.getValue();
      return result;
   }
   
   @Override
   protected boolean validate(PanmirrorOrderedListProps result)
   {
      return true;
   }
   
   
   interface Binder extends UiBinder<Widget, PanmirrorEditOrderedListDialog> {}
   
   private final PanmirrorListCapabilities capabilities_;
   
   private Widget mainWidget_;

   @UiField NumericTextBox startingNumber_;
   @UiField SelectWidget numberStyle_;
   @UiField SelectWidget numberDelimiter_;
   @UiField CheckBox tight_;
   
   
}
