/*
 * PanmirrorInsertTableDialog.java
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

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertTableResult;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorInsertTableDialog extends ModalDialog<PanmirrorInsertTableResult>
{
   public PanmirrorInsertTableDialog(OperationWithInput<PanmirrorInsertTableResult> operation)
   {
      super("Insert Table", Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
      
      
      mainWidget_ = uiBinder.createAndBindUi(this);
   }
   
   @Override
   protected PanmirrorInsertTableResult collectInput()
   {
      PanmirrorInsertTableResult result = new PanmirrorInsertTableResult();
      result.rows = 3;
      result.cols = 3;
      result.header = true;
      return result;
   }


   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   

   private static PanmirrorInsertTableDialogUiBinder uiBinder = GWT
         .create(PanmirrorInsertTableDialogUiBinder.class);

   interface PanmirrorInsertTableDialogUiBinder extends
         UiBinder<Widget, PanmirrorInsertTableDialog>
   {
   }
   
   
   private Widget mainWidget_;


  
}
