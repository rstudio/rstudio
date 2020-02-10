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

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrResult;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditAttrDialog extends ModalDialog<PanmirrorAttrResult>
{

   public interface Binder extends UiBinder<Widget, 
                                            PanmirrorEditAttrDialog>
   {
   }
   
   public PanmirrorEditAttrDialog(
               String caption,
               boolean removeEnabled,
               PanmirrorAttrProps attr,
               OperationWithInput<PanmirrorAttrResult> operation)
   {
      super(caption, Roles.getDialogRole(), operation);
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      
      
      if (removeEnabled)
      {
         ThemedButton removeAttributesButton = new ThemedButton("Remove Attributes");
         removeAttributesButton.addClickHandler((event) -> {
            PanmirrorAttrResult input = collectInput();
            input.action = "remove";
            validateAndGo(input, new Command()
            {
               @Override
               public void execute()
               {
                  closeDialog();
                  if (operation != null)
                     operation.execute(input);
                  onSuccess();
               }
            });
         });
      }
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected PanmirrorAttrResult collectInput()
   {
      PanmirrorAttrProps attr = new PanmirrorAttrProps();
      attr.id = "foo";
      attr.classes = new String[]{"boo", "too"};
      PanmirrorAttrResult result = new PanmirrorAttrResult();
      result.action = "edit";
      result.attr = attr;
      return result;
   }


   @Override
   protected boolean validate(PanmirrorAttrResult input)
   {
      return true;
   }
 
   
   private Widget mainWidget_; 
}
