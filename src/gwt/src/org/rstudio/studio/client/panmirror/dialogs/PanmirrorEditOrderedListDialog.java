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

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorListCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorOrderedListProps;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
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
   
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      
     
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
      result.tight = true;
      result.number = 1;
      result.number_delim = "DefaultDelim";
      result.number_style = "DefaultStyle";
      return result;
   }
   
   @Override
   protected boolean validate(PanmirrorOrderedListProps result)
   {
      return true;
   }
   
   
   interface Binder extends UiBinder<Widget, PanmirrorEditOrderedListDialog> {}
   
   
   private Widget mainWidget_;

 
   
}
