/*
 * DesktopTextInput.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.common.impl;

import org.rstudio.core.client.MessageDisplay.PromptWithOptionResult;
import org.rstudio.core.client.StringUtil;
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
                                                       "",
                                                       false,
                                                       numbersOnly,
                                                       selectionStart,
                                                       selectionLength,
                                                       okButtonCaption);
      if (StringUtil.isNullOrEmpty(result))
      {
         if (cancelOperation != null)
            cancelOperation.execute();
      }
      else
      {
         String[] lines = result.split("\\n");
         okOperation.execute(lines[0],
                             RStudioGinjector.INSTANCE
                                   .getGlobalDisplay()
                                   .getProgressIndicator("Error"));
      }
   }

   @Override
   public void promptForTextWithOption(String title,
                                 String label,
                                 String initialValue,
                                 boolean usePasswordMask,
                                 String extraOptionPrompt,
                                 boolean extraOptionDefault,
                                 int selectionStart,
                                 int selectionLength,
                                 String okButtonCaption,
                                 ProgressOperationWithInput<PromptWithOptionResult> okOperation,
                                 Operation cancelOperation)
   {
      String result = Desktop.getFrame().promptForText(title,
                                                       label,
                                                       initialValue,
                                                       usePasswordMask,
                                                       extraOptionPrompt,
                                                       extraOptionDefault,
                                                       false,
                                                       selectionStart,
                                                       selectionLength,
                                                       okButtonCaption);
      if (StringUtil.isNullOrEmpty(result))
      {
         if (cancelOperation != null)
            cancelOperation.execute();
      }
      else
      {
         PromptWithOptionResult presult = new PromptWithOptionResult();
         String[] lines = result.split("\\n");
         presult.input = lines[0];
         presult.extraOption = "1".equals(lines[1]);
         okOperation.execute(presult,
                             RStudioGinjector.INSTANCE
                                   .getGlobalDisplay()
                                   .getProgressIndicator("Error"));
      }
   }
}
