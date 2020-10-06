/*
 * ChunkExecUnit.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

public class ChunkExecUnit
{
   public ChunkExecUnit(Scope scope, int execMode)
   {
      this(scope, null, execMode, NotebookQueueUnit.EXEC_SCOPE_CHUNK);
   }
   
   public ChunkExecUnit(Scope scope, Range range, int execMode, int execScope)
   {
      scope_ = scope;
      range_ = range;
      execMode_ = execMode;
      execScope_ = execScope;
   }
   
   public Scope getScope()
   {
      return scope_;
   }
   
   public Range getRange()
   {
      return range_;
   }

   public int getExecScope()
   {
      return execScope_;
   }
   
   public int getExecMode()
   {
      return execMode_;
   }
   
   private final Scope scope_;
   private final Range range_;
   private final int execScope_;
   private final int execMode_;
}
