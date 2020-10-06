/*
 * ShellOutputWriter.java
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
package org.rstudio.studio.client.common.shell;

import org.rstudio.studio.client.common.debugging.model.UnhandledError;

public interface ShellOutputWriter 
{
   void consoleWriteError(String string);
   void consoleWriteExtendedError(
         String string, UnhandledError traceInfo, 
         boolean expand, String command);
   void consoleWriteOutput(String output);
   void consoleWritePrompt(String prompt);
}
