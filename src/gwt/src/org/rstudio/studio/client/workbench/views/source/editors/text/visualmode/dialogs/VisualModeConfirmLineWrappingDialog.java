/*
 * VisualModeLineWrappingDialog.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.dialogs;


// explain the deal for this particular context

// offer for this file

// offer for this project (if approprate)

// offer to do nothing

// learn more

import com.google.gwt.aria.client.Roles;


import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualModeConfirm;

import com.google.gwt.user.client.ui.Widget;

public class VisualModeConfirmLineWrappingDialog extends ModalDialog<VisualModeConfirm.LineWrappingAction>
{   
   public VisualModeConfirmLineWrappingDialog(OperationWithInput<VisualModeConfirm.LineWrappingAction> onConfirm,
                                  Operation onCancel)
   {
      super("Line Wrapping", 
            Roles.getDialogRole(), 
            onConfirm, 
            onCancel);
      
    
   }
   
  
   @Override
   protected Widget createMainWidget()
   {
      return null;
   }
   
  
   
   @Override
   protected VisualModeConfirm.LineWrappingAction collectInput()
   {
      return VisualModeConfirm.LineWrappingAction.SetFileLineWrapping;
   }


   @Override
   protected boolean validate(VisualModeConfirm.LineWrappingAction result)
   {
      return true;  
   }
   
  
   
   private Widget mainWidget_; 
   

   
}
