/*
 * ChunkRowAceExecState.java
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

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;

import com.google.gwt.user.client.Command;

public class ChunkRowAceExecState extends ChunkRowExecState
{
   public ChunkRowAceExecState(final AceEditorNative editor, int row, int state,
         Command onRemoved)
   {
      super(state);

      anchor_ = Anchor.createAnchor(editor.getSession().getDocument(), row, 0);
      editor_ = editor;
      row_ = row;
      onRemoved_ = onRemoved;
      anchor_.addOnChangeHandler(new Command()
      {
         @Override
         public void execute()
         {
            // no work if anchor hasn't changed rows
            if (getRow() == anchor_.getRow())
               return;
            
            // if we're just cleaning up this line, finish the cleanup 
            // immediately rather than trying to shift 
            if (state_ == LINE_RESTING || state_ == LINE_ERROR)
            {
               detach();
               return;
            }

            // remove all cumulative state from the old line and reapply to
            // the new line
            removeClazz();
            row_ = anchor_.getRow();
            for (int i = LINE_QUEUED; i <= state_; i++)
            {
               addClazz(i);
            }
         }
      });

      addClazz(state_);
   }

   public int getRow()
   {
      return row_;
   }

   public void setRow(int row)
   {
      row_ = row;
   }
   
   @Override
   public void detach()
   {
      super.detach();
      anchor_.detach();
      if (onRemoved_ != null)
         onRemoved_.execute();
   }

   protected void addClazz(int state)
   {
      editor_.getRenderer().addGutterDecoration(getRow() - 1, getClazz(state));
   }
   
   protected void removeClazz()
   {
      for (int i = LINE_QUEUED; i <= state_; i++)
      {
         editor_.getRenderer().removeGutterDecoration(
            getRow() - 1, getClazz(i));
      }
   }

   private String getClazz(int state)
   {
      switch (state)
      {
      case LINE_QUEUED:
         return LINE_QUEUED_CLASS;
      case LINE_EXECUTED:
         return LINE_EXECUTED_CLASS;
      case LINE_RESTING:
         return LINE_RESTING_CLASS;
      case LINE_ERROR:
         return LINE_ERROR_CLASS;
      }
      return "";
   }
   
   private final AceEditorNative editor_;
   private final Anchor anchor_;
   private final Command onRemoved_;

   public final static String LINE_QUEUED_CLASS   = "ace_chunk-queued-line";
   public final static String LINE_EXECUTED_CLASS = "ace_chunk-executed-line";
   public final static String LINE_RESTING_CLASS  = "ace_chunk-resting-line";
   public final static String LINE_ERROR_CLASS    = "ace_chunk-error-line";
   
   private int row_;
}
