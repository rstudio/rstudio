/*
 * TerminalDiagnostics.java
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
package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.StringUtil;

public class TerminalDiagnostics
{
   public void log(String msg)
   {
      if (diagnostic_ == null)
         diagnostic_ = new StringBuilder();

      diagnostic_.append(StringUtil.getTimestamp());
      diagnostic_.append(": ");
      diagnostic_.append(msg);
      diagnostic_.append("\n");
   }

   public String getLog()
   {
      if (diagnostic_ == null || diagnostic_.length() == 0)
         return("<none>\n");
      else
         return diagnostic_.toString();
   }

   public void resetLog()
   {
      diagnostic_ = null;
   }

   private StringBuilder diagnostic_;
}
