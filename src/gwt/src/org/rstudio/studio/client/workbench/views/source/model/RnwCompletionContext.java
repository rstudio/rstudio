/*
 * RnwCompletionContext.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface RnwCompletionContext
{
   void getChunkOptions(
                     ServerRequestCallback<RnwChunkOptions> requestCallback);

   RnwWeave getActiveRnwWeave();

   /**
    * Determines whether the given line and cursor position are a valid place
    * to attempt an Rnw options completion. If not, -1 is returned. If so,
    * the number returned is the index into <code>line</code> where the options
    * start.
    */
   int getRnwOptionsStart(String line, int cursorPos);
}
