/*
 * TextEditingTargetNotebook.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

public class TextEditingTargetNotebook
{
   public TextEditingTargetNotebook(DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;      
   }
   
   public void executeChunk(Scope chunk, String code)
   {
      
      int row = chunk.getEnd().getRow();
      ChunkOutputLineWidget widget = ChunkOutputLineWidget.create(row);
      widget.setFixedWidth(true);
      docDisplay_.addLineWidget(widget);
   }
   
   
   private final DocDisplay docDisplay_;
   @SuppressWarnings("unused")
   private final DocUpdateSentinel docUpdateSentinel_;
}
