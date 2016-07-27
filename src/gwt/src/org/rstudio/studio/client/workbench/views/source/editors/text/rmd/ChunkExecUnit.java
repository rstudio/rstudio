/*
 * ChunkExecUnit.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
   public ChunkExecUnit(Scope scope)
   {
      this(scope, null);
   }

   public ChunkExecUnit(Scope scope, Range range)
   {
      scope_ = scope;
      range_ = range;
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
      return range_ == null ? NotebookQueueUnit.EXEC_SCOPE_CHUNK :
         NotebookQueueUnit.EXEC_SCOPE_PARTIAL;
   }
   
   private final Scope scope_;
   private final Range range_;
}
