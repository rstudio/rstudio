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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkRowExecState;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;

/**
 * Represents the execution state for a row of a chunk in visual mode.
 */
public class VisualModeChunkRowState extends ChunkRowExecState
{
   public VisualModeChunkRowState(int state, AceEditor editor, int row)
   {
      super(state);
      
      // Convert to zero-based row
      row = row - 1;
      
      ele_ = Document.get().createDivElement();

      // Get all the line groups in the editor; note that this only works if the
      // word wrapping is turned on (otherwise Ace doesn't create ace_line_group
      // elements), but we can rely on word wrapping since we enforce it in the
      // visual editor.
      Element[] lines = DomUtils.getElementsByClassName(
            editor.asWidget().getElement(), "ace_line_group");
      
      if (row > lines.length)
      {
         // Very unlikely, but ensure we aren't trying to walk off the end of
         // the array
         Debug.logWarning("Can't draw execution state on line " + row + " of " + lines.length);
      }
      else
      {
         // Copy the position attributes from the line group
         Style style = ele_.getStyle();
         Style lineStyle = lines[row].getStyle();
         style.setProperty("top", lineStyle.getTop());
         style.setProperty("height", lineStyle.getHeight());
         style.setProperty("position", "absolute");
      }
      
      addClazz(state);
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
   
   @Override
   public void detach()
   {
      super.detach();
      ele_.removeFromParent();
   }
   
   public Element getElement()
   {
      return ele_;
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
