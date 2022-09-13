/*
 * ShinyDocumentWarningDialog.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.rmarkdown.RMarkdownConstants;

public class ShinyDocumentWarningDialog extends ModalDialogBase
{
   public ShinyDocumentWarningDialog(
         final OperationWithInput<Integer> onSelected)
   {
      super(Roles.getAlertdialogRole());
      warning_ = new ShinyDocumentWarning();
      setWidth("400px");
      setText(constants_.warningDialogText());
      addOkButton(new ThemedButton(constants_.yesOnceButtonText(),
            returnResult(onSelected, RENDER_SHINY_ONCE)));
      addButton(new ThemedButton(constants_.yesAlwaysButtonText(),
            returnResult(onSelected, RENDER_SHINY_ALWAYS)),
            ElementIds.DIALOG_YES_BUTTON);
      addLeftButton(new ThemedButton(constants_.noButtonText(),
            returnResult(onSelected, RENDER_SHINY_NO)),
            ElementIds.DIALOG_NO_BUTTON);

      setARIADescribedBy(warning_.getMessageElement());
   }

   @Override
   protected void focusInitialControl()
   {
      focusOkButton();
   }

   @Override
   protected Widget createMainWidget()
   {
      return warning_;
   }
   
   private ClickHandler returnResult(
         final OperationWithInput<Integer> operation, 
         final int result)
   {
      return new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            closeDialog();
            operation.execute(result);
         }
      };
   }
   
   public final static int RENDER_SHINY_ONCE = 0;
   public final static int RENDER_SHINY_ALWAYS = 1;
   public final static int RENDER_SHINY_NO = 2;

   private ShinyDocumentWarning warning_;

   private static final RMarkdownConstants constants_ = GWT.create(RMarkdownConstants.class);
}
