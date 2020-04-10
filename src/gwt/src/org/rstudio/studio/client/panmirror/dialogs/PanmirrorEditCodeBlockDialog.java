/*
 * PanmirrorEditCodeBlockDialog.java
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
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorCodeBlockProps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditCodeBlockDialog extends ModalDialog<PanmirrorCodeBlockProps>
{ 
   public PanmirrorEditCodeBlockDialog(
               PanmirrorCodeBlockProps codeBlock,
               String[] languages,
               OperationWithInput<PanmirrorCodeBlockProps> operation)
   {
      super("Code Block", Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
       
      editAttr_.setAttr(codeBlock);
      
      
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected PanmirrorCodeBlockProps collectInput()
   {
      PanmirrorCodeBlockProps result = new PanmirrorCodeBlockProps();
      PanmirrorAttrProps attr = editAttr_.getAttr();
      result.id = attr.id;
      result.classes = attr.classes;
      result.keyvalue = attr.keyvalue;
      result.lang = "";
      return result;
   }


   @Override
   protected boolean validate(PanmirrorCodeBlockProps input)
   {
      return true;
   }

   
   interface Binder extends UiBinder<Widget, PanmirrorEditCodeBlockDialog> {}
   
   private Widget mainWidget_; 
   
   @UiField PanmirrorEditAttrWidget editAttr_;
  
}
