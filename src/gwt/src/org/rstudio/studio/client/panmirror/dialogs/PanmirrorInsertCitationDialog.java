/*
 * PanmirrorInsertCitationDialog.java
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

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithCue;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertCitationResult;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorInsertCitationDialog extends ModalDialog<PanmirrorInsertCitationResult>
{
   public PanmirrorInsertCitationDialog(OperationWithInput<PanmirrorInsertCitationResult> operation)
   {
      super("Insert Citation", Roles.getDialogRole(), operation, () -> {
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
   protected PanmirrorInsertCitationResult collectInput()
   {
      PanmirrorInsertCitationResult result = new PanmirrorInsertCitationResult();
      result.id = citationId_.getText().trim();
      result.locator = locator_.getText().trim();
      return result;
   }
   
   @Override
   protected boolean validate(PanmirrorInsertCitationResult result)
   {
      return true;
   }
   
   interface Binder extends UiBinder<Widget, PanmirrorInsertCitationDialog> {}
   
   private Widget mainWidget_;

   @UiField TextBox citationId_;
   @UiField TextBoxWithCue locator_;
   
}
