/*
 * CppCompletionContext.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.core.client.CommandWith2Args;

public interface CppCompletionContext
{
   boolean isCompletionEnabled();
   void withUpdatedDoc(CommandWith2Args<String, String> onUpdated);
   void cppCompletionOperation(CppCompletionOperation operation);
   String getDocPath();
}
