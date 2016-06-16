/*
 * ChunkRowExecState.java
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

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

public class ChunkRowExecState
{
   public ChunkRowExecState(final AceEditorNative editor, int row, int state,
         Command onRemoved)
   {
      anchor_ = Anchor.createAnchor(editor.getSession().getDocument(), row, 0);
      editor_ = editor;
      row_ = row;
      state_ = state;
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
   
   // Public methods ----------------------------------------------------------

   public int getRow()
   {
      return row_;
   }

   public void setRow(int row)
   {
      row_ = row;
   }

   public void detach()
   {
      resetTimer();
      removeClazz();
      anchor_.detach();
      if (onRemoved_ != null)
         onRemoved_.execute();
   }
   
   public int getState()
   {
      return state_;
   }
   
   public static String getClazz(int state)
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
   
   public void setState(int state)
   {
      // ignore if already at this state
      if (state_ == state)
         return;
      
      // if the moving to the error state, clean other states
      if (state == LINE_ERROR)
         removeClazz();
      
      // if this is the error state, there's no transition to the resting state
      if (state_ == LINE_ERROR && state == LINE_RESTING)
         return;
      
      state_ = state;
      if (state_ == LINE_RESTING)
      {
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               addClazz(state_);
               scheduleDismiss();
            }
         };
         timer_.schedule(LINGER_MS);
      }
      else
      {
         addClazz(state_);
      }
   }
   
   // Private methods ---------------------------------------------------------
   
   private void addClazz(int state)
   {
      editor_.getRenderer().addGutterDecoration(getRow() - 1, getClazz(state));
   }
   
   private void removeClazz()
   {
      for (int i = LINE_QUEUED; i <= state_; i++)
      {
         editor_.getRenderer().removeGutterDecoration(
            getRow() - 1, getClazz(i));
      }
   }
   
   private void scheduleDismiss()
   {
      resetTimer();
      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            detach();
         }
      };
      timer_.schedule(FADE_MS);
   }
   
   private void resetTimer()
   {
      if (timer_ != null && timer_.isRunning())
      {
         timer_.cancel();
         timer_ = null;
      }
   }

   private int row_;
   private int state_;

   private final Anchor anchor_;
   private final AceEditorNative editor_;
   private final Command onRemoved_;
   
   private Timer timer_;
   
   private final static int LINGER_MS = 250;
   private final static int FADE_MS   = 400;

   public final static String LINE_QUEUED_CLASS   = "ace_chunk-queued-line";
   public final static String LINE_EXECUTED_CLASS = "ace_chunk-executed-line";
   public final static String LINE_RESTING_CLASS  = "ace_chunk-resting-line";
   public final static String LINE_ERROR_CLASS    = "ace_chunk-error-line";
   
   public final static int LINE_QUEUED   = 0;
   public final static int LINE_EXECUTED = 1;
   public final static int LINE_RESTING  = 2;
   public final static int LINE_ERROR    = 3;
   public final static int LINE_NONE     = 4;
}
