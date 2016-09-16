/*
 * AceEditorIdleCommands.java
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

import org.rstudio.studio.client.common.mathjax.MathJaxUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetIdleMonitor.IdleCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetIdleMonitor.IdleState;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.inject.Singleton;

@Singleton
public class AceEditorIdleCommands
{
   public AceEditorIdleCommands()
   {
      PREVIEW_LINK = previewLink();
      PREVIEW_LATEX = previewLatex();
   }
   
   // Latex Preview ----
   
   private IdleCommand previewLatex()
   {
      return new IdleCommand()
      {
         @Override
         public void execute(DocDisplay display, DocUpdateSentinel sentinel, 
               IdleState state)
         {
            onPreviewLatex(display, state);
         }
      };
   }
   
   private void onPreviewLatex(DocDisplay display, IdleState state)
   {
      Position position = resolvePosition(display, state);
      Range range = MathJaxUtil.getLatexRange(display, position);
      if (range == null)
         return;
      
      display.renderLatex(range);
   }
   
   // Link Preview ----
   
   private IdleCommand previewLink()
   {
      return new IdleCommand()
      {
         @Override
         public void execute(DocDisplay display, DocUpdateSentinel sentinel, 
               IdleState idleState)
         {
            ImagePreviewer.onPreviewLink(display, sentinel, 
                  resolvePosition(display, idleState));
         }
      };
   }
   
   private static Position resolvePosition(DocDisplay display, IdleState state)
   {
      int type = state.getType();
      if (type == IdleState.STATE_CURSOR_IDLE)
         return display.getCursorPosition();
      else if (type == IdleState.STATE_MOUSE_IDLE)
         return display.screenCoordinatesToDocumentPosition(
               state.getMouseX(), state.getMouseY());
      
      assert false : "Unhandled idle state type '" + type + "'";
      return Position.create(0, 0);
   }
   
   public final IdleCommand PREVIEW_LINK;
   public final IdleCommand PREVIEW_LATEX;
}