/*
 * TextInput.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common;

import org.rstudio.core.client.MessageDisplay.PromptWithOptionResult;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

public interface TextInput
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
                             Operation cancelOperation);

   void promptForPassword(String title,
                          String label,
                          String initialValue,
                          // Null or "" means don't prompt for remembering pw
                          String rememberPasswordPrompt,
                          boolean rememberByDefault,
                          int selectionStart,
                          int selectionLength,
                          String okButtonCaption,
                          ProgressOperationWithInput<PromptWithOptionResult> okOperation,
                          Operation cancelOperation);
}
