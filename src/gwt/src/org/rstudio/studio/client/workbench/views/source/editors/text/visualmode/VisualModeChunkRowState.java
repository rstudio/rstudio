/*
 * VisualModeChunkRowState.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkRowExecState;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;

/**
 * Represents the execution state for a row of a chunk in visual mode.
 */
public class VisualModeChunkRowState extends ChunkRowExecState
{
   public VisualModeChunkRowState(int state)
   {
      super(state);
      ele_ = Document.get().createDivElement();
   }

   @Override
   protected void addClazz(int state)
   {
      ele_.addClassName(getClazz(state));
   }

   @Override
   protected void removeClazz()
   {
      ele_.setAttribute("class", "");
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
   
   public final static String LINE_QUEUED_CLASS   = "visual_chunk-queued-line";
   public final static String LINE_EXECUTED_CLASS = "visual_chunk-executed-line";
   public final static String LINE_RESTING_CLASS  = "visual_chunk-resting-line";
   public final static String LINE_ERROR_CLASS    = "visual_chunk-error-line";
   
   private DivElement ele_;
}
