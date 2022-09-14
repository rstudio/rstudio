/*
 * PanmirrorEditMathDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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


package org.rstudio.studio.client.panmirror.dialogs;


import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;


public class PanmirrorEditMathDialog extends ModalDialog<String>
{
   public PanmirrorEditMathDialog(String id, OperationWithInput<String> operation)
   {
      super(constants_.editEquationCaption(), Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      id_.setText(id.isEmpty() ? id : "#" + id);
      DomUtils.disableSpellcheck(id_);;
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Override
   public void focusInitialControl()
   {
      id_.setFocus(true);
      id_.selectAll();
   }

   @Override
   protected String collectInput()
   {  
      return id_.getText().trim().replaceFirst("^#", "");
   }
   
   @Override
   protected boolean validate(String input)
   {
      if (!input.isEmpty() && !input.startsWith("eq-"))
      {
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
            constants_.invalidIDCaption(),
            constants_.invalidIDMessage(),
            id_);
         return false;
      } else {
         return true;
      }
   }

   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);

   interface Binder extends UiBinder<Widget, PanmirrorEditMathDialog> {}
   
   private Widget mainWidget_; 
   
   @UiField TextBox id_;

}
