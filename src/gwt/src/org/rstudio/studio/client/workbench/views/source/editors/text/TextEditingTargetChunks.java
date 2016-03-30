/*
 * TextEditingTargetChunks.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextUi;

public class TextEditingTargetChunks
{
   public TextEditingTargetChunks(TextEditingTarget target)
   {
      target_ = target;
      toolbars_ = new ArrayList<ChunkContextUi>();
      target.getDocDisplay().addScopeTreeReadyHandler(
            new ScopeTreeReadyEvent.Handler()
      {
         @Override
         public void onScopeTreeReady(ScopeTreeReadyEvent event)
         {
            initializeWidgets();
         }
      });
   }

   // Public methods ----------------------------------------------------------
   
   public void setChunkState(int preambleRow, int state)
   {
      for (ChunkContextUi toolbar: toolbars_)
      {
         if (toolbar.getPreambleRow() == preambleRow)
         {
            toolbar.setState(state);
         }
      }
   }
   
   // Private methods ---------------------------------------------------------
   
   private void initializeWidgets()
   {
      ScopeList scopes = new ScopeList(target_.getDocDisplay());
      for (Scope scope: scopes)
      {
         if (!scope.isChunk())
            continue;

         insertChunkToolbar(scope);
      }
   }
   
   private void onWidgetRemoved(LineWidget widget)
   {
      // remove the widget from our internal list
      for (ChunkContextUi toolbar: toolbars_)
      {
         if (toolbar.getLineWidget() == widget)
         {
            toolbars_.remove(toolbar);
            break;
         }
      }
   }
   
   private void insertChunkToolbar(Scope chunk)
   {
      // see if we've already drawn a toolbar for this chunk
      boolean hasToolbar = false;
      for (ChunkContextUi toolbar: toolbars_)
      {
         if (toolbar.getPreambleRow() == chunk.getPreamble().getRow())
         {
            hasToolbar = true; 
            break;
         }
      }
      if (hasToolbar)
         return;
         
      ChunkContextUi ui = new ChunkContextUi(target_, chunk, 
            new CommandWithArg<LineWidget>()
      {
         @Override
         public void execute(LineWidget widget)
         {
            onWidgetRemoved(widget);
         }
      });
      toolbars_.add(ui);
   }
   
   private final TextEditingTarget target_;
   private final ArrayList<ChunkContextUi> toolbars_;
}
