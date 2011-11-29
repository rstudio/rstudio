/*
 * ProcessCallback.java
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
package org.rstudio.studio.client.workbench.views.vcs.common;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.SVNServerOperations.ProcessResult;

public class ProcessCallback extends SimpleRequestCallback<ProcessResult>
{
   public ProcessCallback(String title)
   {
      super(title);
      title_ = title;
   }

   @Override
   public void onResponseReceived(ProcessResult response)
   {
      if (!StringUtil.isNullOrEmpty(response.getOutput()))
      {
         new ConsoleProgressDialog(title_,
                                   response.getOutput(),
                                   response.getExitCode()).showModal();
      }
   }

   private final String title_;
}
