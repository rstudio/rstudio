/*
 * DesktopTextInput.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.impl;

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.TextInput;

public class DesktopTextInput implements TextInput
{
   public void promptForText(String title,
                             String label,
                             String initialValue,
                             boolean usePasswordMask,
                             boolean numbersOnly,
                             int selectionStart,
                             int selectionLength,
                             String okButtonCaption,
                             ProgressOperationWithInput<String> okOperation,
                             Operation cancelOperation)
   {
      String result = Desktop.getFrame().promptForText(title,
                                                       label,
                                                       initialValue,
                                                       usePasswordMask,
                                                       numbersOnly,
                                                       selectionStart,
                                                       selectionLength,
                                                       okButtonCaption);
      if (result == null)
      {
         if (cancelOperation != null)
            cancelOperation.execute();
      }
      else
      {
         okOperation.execute(result,
                             RStudioGinjector.INSTANCE
                                   .getGlobalDisplay()
                                   .getProgressIndicator("Error"));
      }
   }
}
