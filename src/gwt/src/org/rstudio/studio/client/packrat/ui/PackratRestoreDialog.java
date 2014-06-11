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
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.packrat.model.PackratPackageAction;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Widget;

public class PackratRestoreDialog extends ModalDialog<Void>
{
   public PackratRestoreDialog(
         JsArray<PackratPackageAction> prRestore,
         final OperationWithInput<Void> operation)
   {
      super("Packrat Restore", operation);
      setOkButtonCaption("Restore");
      contents_ = new PackratRestoreDialogContents(prRestore);
      setWidth("500px");
   }

   @Override
   protected Widget createMainWidget()
   {
      return contents_;
   }
   
   private PackratRestoreDialogContents contents_;

   @Override
   protected Void collectInput()
   {
      return null; // no input to collect
   }

   @Override
   protected boolean validate(Void input)
   {
      return true;
   }
}
