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
import org.rstudio.core.client.widget.FormTextArea;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrEditInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorUITools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditAttrDialog extends ModalDialog<PanmirrorAttrResult>
{ 
   public PanmirrorEditAttrDialog(
               String caption,
               boolean removeEnabled,
               PanmirrorAttrProps attr,
               OperationWithInput<PanmirrorAttrResult> operation)
   {
      super(caption, Roles.getDialogRole(), operation);
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      uiTools_ = new PanmirrorUITools();
      
      PanmirrorAttrEditInput input = uiTools_.attrPropsToInput(attr);
      id_.getElement().setId(ElementIds.VISUAL_MD_ATTR_ID);
      id_.setText(input.id);
      classes_.getElement().setId(ElementIds.VISUAL_MD_ATTR_CLASSES);
      classes_.setText(input.classes);
      attributes_.getElement().setId(ElementIds.VISUAL_MD_ATTR_KEYVALUE);
      attributes_.setText(input.keyvalue);
     
      if (removeEnabled)
      {
         ThemedButton removeAttributesButton = new ThemedButton("Remove Attributes");
         removeAttributesButton.addClickHandler((event) -> {
            PanmirrorAttrResult result = collectInput();
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
   protected PanmirrorAttrResult collectInput()
   {
      PanmirrorAttrEditInput input = new PanmirrorAttrEditInput();
      input.id = id_.getValue().trim();
      input.classes = classes_.getValue().trim();
      input.keyvalue = attributes_.getValue().trim();
      PanmirrorAttrResult result = new PanmirrorAttrResult();
      result.attr = uiTools_.attrInputToProps(input);
      result.action = "edit";
      return result;
   }


   @Override
   protected boolean validate(PanmirrorAttrResult input)
   {
      return true;
   }
 
   private final PanmirrorUITools uiTools_;
   
   interface Binder extends UiBinder<Widget, PanmirrorEditAttrDialog> {}
   
   private Widget mainWidget_; 
   
   @UiField TextBox id_;
   @UiField TextBox classes_;
   @UiField FormTextArea attributes_;
  
}
