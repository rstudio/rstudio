/*
 * PanimrrorEditAttrDialog.java
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


package org.rstudio.studio.client.panmirror.dialogs;


import com.google.gwt.aria.client.Roles;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrEditResult;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditAttrDialog extends ModalDialog<PanmirrorAttrEditResult>
{ 
   public PanmirrorEditAttrDialog(
               String caption,
               String removeButtonCaption,
               String idHint,
               PanmirrorAttrProps attr,
               OperationWithInput<PanmirrorAttrEditResult> operation)
   {
      super(caption, Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
       
      editAttr_.setAttr(attr, idHint);
      
      if (removeButtonCaption != null)
      {
         ThemedButton removeAttributesButton = new ThemedButton(removeButtonCaption);
         removeAttributesButton.addClickHandler((event) -> {
            PanmirrorAttrEditResult result = collectInput();
            result.action = "remove";
            validateAndGo(result, new Command()
            {
               @Override
               public void execute()
               {
                  closeDialog();
                  if (operation != null)
                     operation.execute(result);
                  onSuccess();
               }
            });
         });
         addLeftButton(removeAttributesButton, ElementIds.VISUAL_MD_ATTR_REMOVE_BUTTON);
      }
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected void focusFirstControl()
   {
      editAttr_.setFocus();
   }
   
   @Override
   protected PanmirrorAttrEditResult collectInput()
   {
      PanmirrorAttrEditResult result = new PanmirrorAttrEditResult();
      result.attr = editAttr_.getAttr();
      result.action = "edit";
      return result;
   }


   @Override
   protected boolean validate(PanmirrorAttrEditResult input)
   {
      return true;
   }

   
   interface Binder extends UiBinder<Widget, PanmirrorEditAttrDialog> {}
   
   private Widget mainWidget_; 
   
   @UiField PanmirrorEditAttrWidget editAttr_;
  
}
