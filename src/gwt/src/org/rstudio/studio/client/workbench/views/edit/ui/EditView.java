/*
 * EditView.java
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
package org.rstudio.studio.client.workbench.views.edit.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.Command;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.workbench.views.edit.Edit.Display;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

public class EditView implements Display
{
   public void show(final String text,
                    final boolean isRCode,
                    final boolean lineWrapping,
                    final ProgressOperationWithInput<String> operation)
   {
      AceEditor.load(new Command()
      {
         public void execute()
         {
            new EditDialog(text, Roles.getDialogRole(), isRCode, lineWrapping, operation).showModal();
         }
      });
   }
}
