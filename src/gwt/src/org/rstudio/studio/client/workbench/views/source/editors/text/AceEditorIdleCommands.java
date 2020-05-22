/*
 * AceEditorIdleCommands.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.common.mathjax.MathJaxUtil;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetIdleMonitor.IdleCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetIdleMonitor.IdleState;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AceEditorIdleCommands
{
   public AceEditorIdleCommands()
   {
      PREVIEW_LINK = previewLink();
      PREVIEW_LATEX = previewLatex();
   }
   
   @Inject
   private void initialize(UserPrefs uiPrefs)
   {
      prefs_ = uiPrefs;
   }
   
   // Latex Preview ----
   
   private IdleCommand previewLatex()
   {
      return new IdleCommand()
      {
         @Override
         public void execute(TextEditingTarget target, 
               DocUpdateSentinel sentinel, IdleState state)
         {
            onPreviewLatex(target, sentinel, state);
         }
      };
   }
   
   private void onPreviewLatex(TextEditingTarget target, 
         DocUpdateSentinel sentinel, IdleState state)
   {
      Position position = resolvePosition(target.getDocDisplay(), state);
      Range range = MathJaxUtil.getLatexRange(target.getDocDisplay(), position);
      if (range == null)
         return;
      
      String pref = prefs_.latexPreviewOnCursorIdle().getValue();
      
      // preview if preview is always enabled, or specifically enabled for this
      // document
      if (sentinel.getBoolProperty(
            TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED,
            pref != UserPrefs.LATEX_PREVIEW_ON_CURSOR_IDLE_NEVER))
      {
         target.renderLatex(range, true);
      }
   }
   
   // Link Preview ----
   
   private IdleCommand previewLink()
   {
      return new IdleCommand()
      {
         @Override
         public void execute(TextEditingTarget target, 
               DocUpdateSentinel sentinel, IdleState idleState)
         {
            ImagePreviewer.onPreviewLink(target.getDocDisplay(), sentinel, 
                  prefs_, resolvePosition(target.getDocDisplay(), idleState));
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

   private UserPrefs prefs_;
   
   public final IdleCommand PREVIEW_LINK;
   public final IdleCommand PREVIEW_LATEX;
}
