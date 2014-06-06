/* PackratRestoreDialog.java
 *
 * Copyright (C) 2014 by RStudio, Inc.
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

package org.rstudio.studio.client.packrat.ui;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.packrat.model.PackratRestoreActions;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

public class PackratRestoreDialog extends ModalDialogBase
{
   public PackratRestoreDialog(JsArray<PackratRestoreActions> prRestore, final EventBus eventBus)
   {
      setText("Restore packages...");
      ThemedButton CancelButton = new ThemedButton("Cancel", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            closeDialog();
         }
      });
      
      ThemedButton RestoreButton = new ThemedButton("Restore", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event) {
            eventBus.fireEvent(new SendToConsoleEvent("packrat::restore(prompt = FALSE)", true, false));
            closeDialog();
         }
      });
      
      addOkButton(RestoreButton);
      addCancelButton(CancelButton);
      contents_ = new PackratRestoreDialogContents(prRestore);
      setWidth("500px");
   }

   @Override
   protected Widget createMainWidget()
   {
      return contents_;
   }
   
   private PackratRestoreDialogContents contents_;
}
