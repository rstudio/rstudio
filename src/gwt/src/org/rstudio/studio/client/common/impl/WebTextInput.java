/*
 * WebTextInput.java
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
import org.rstudio.core.client.widget.TextEntryModalDialog;
import org.rstudio.studio.client.common.TextInput;

public class WebTextInput implements TextInput
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
      new TextEntryModalDialog(title,
                               label,
                               initialValue,
                               usePasswordMask,
                               numbersOnly,
                               selectionStart,
                               selectionLength,
                               okButtonCaption,
                               300,
                               okOperation,
                               cancelOperation).showModal();
   }

}
