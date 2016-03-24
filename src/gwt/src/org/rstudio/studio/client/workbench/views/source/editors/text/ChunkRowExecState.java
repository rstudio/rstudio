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

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Document;

public class ChunkRowExecState
{
   public ChunkRowExecState(Document document, int row, int state)
   {
      anchor_ = Anchor.createAnchor(document, row, 0);
      row_ = row;
      state_ = state;
   }
   
   public int getRow()
   {
      return row_;
   }
   public void setRow(int row)
   {
      row_ = row;
   }

   public Anchor getAnchor()
   {
      return anchor_;
   }
   
   public int getState()
   {
      return state_;
   }
   
   public String getClazz()
   {
      switch (state_)
      {
      case LINE_QUEUED:
         return LINE_QUEUED_CLASS;
      case LINE_EXECUTED:
         return LINE_EXECUTED_CLASS;
      }
      return "";
   }

   private int row_;
   private int state_;
   private Anchor anchor_;

   public final static String LINE_QUEUED_CLASS = "ace_chunk-queued-line";
   public final static String LINE_EXECUTED_CLASS = "ace_chunk-executed-line";
   
   public final static int LINE_RESTING  = 0;
   public final static int LINE_QUEUED   = 1;
   public final static int LINE_EXECUTED = 2;
}
