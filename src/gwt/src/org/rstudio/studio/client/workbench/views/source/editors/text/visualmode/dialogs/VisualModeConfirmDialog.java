/*
 * VisualModeConfirmDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.dialogs;


import com.google.gwt.aria.client.Roles;


import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;

public class VisualModeConfirmDialog extends ModalDialog<Boolean>
{
   public VisualModeConfirmDialog(OperationWithInput<Boolean> onConfirm,
                                  Operation onCancel)
   {
      super(constants_.switchToVisualMode(),
            Roles.getDialogRole(),
            onConfirm,
            onCancel);

      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      mainWidget_.addStyleName(VisualModeDialogsResources.INSTANCE.styles().confirmDialog());

      setOkButtonCaption(constants_.useVisualMode());

      chkDontShowAgain_ = new CheckBox(constants_.dontShowMessageAgain());
      chkDontShowAgain_.setValue(true);
      addLeftWidget(chkDontShowAgain_);
   }


   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }



   @Override
   protected Boolean collectInput()
   {
      return chkDontShowAgain_.getValue();
   }


   @Override
   protected boolean validate(Boolean result)
   {
      return true;
   }


   interface Binder extends UiBinder<Widget, VisualModeConfirmDialog> {}

   private Widget mainWidget_;

   private CheckBox chkDontShowAgain_;
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}