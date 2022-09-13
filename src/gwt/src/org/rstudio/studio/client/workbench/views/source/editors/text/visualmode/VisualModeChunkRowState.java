/*
 * VisualModeChunkRowState.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkRowExecState;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

/**
 * Represents the execution state for a row of a chunk in visual mode.
 */
public class VisualModeChunkRowState extends ChunkRowExecState
{
   /**
    * Constructs a new row state.
    *
    * @param state The state of the row
    * @param editor The Ace editor instance where the row resides
    * @param row The index of the row in the editor
    */
   public VisualModeChunkRowState(int state, AceEditor editor, int row)
   {
      this(state, editor, row, null);
   }

   /**
    * Constructs a new row state.
    *
    * @param state The state of the row
    * @param editor The Ace editor instance where the row resides
    * @param row The index of the row in the editor
    * @param clazz The CSS class to apply to the row decoration
    */
   public VisualModeChunkRowState(int state, AceEditor editor, int row, String clazz)
   {
      super(state);
      
      attached_ = false;
      editor_ = editor;
      row_ = -1;
      registrations_ = new HandlerRegistrations();
      
      // Convert to zero-based row
      row = row - 1;

      // Create a backing anchor in the editor.
      anchor_ = editor.createAnchor(Position.create(row, 0));
      anchor_.addOnChangeHandler(() ->
      {
         // If the text changes enough to move the anchor, just get rid of the indicator.
         if (row_ != anchor_.getRow())
         {
            detach();
         }
      });

      // If the user edits the line containing this indicator, remove the indicator.
      registrations_.add(editor.addDocumentChangedHandler((evt) ->
      {
         if (row_ >= evt.getEvent().start.getRow() &&
             row_ <= evt.getEvent().end.getRow())
         {
            detach();
         }
      }));

      ele_ = Document.get().createDivElement();

      // This element is decorative from the perspective of a screen reader
      A11y.setARIAHidden(ele_);

      if (StringUtil.isNullOrEmpty(clazz))
      {
         addClazz(state);
      }
      else
      {
         addClazz(state, clazz);
      }
   }

   @Override
   protected void addClazz(int state)
   {
      addClazz(state, getClazz(state));
   }

   protected void addClazz(int state, String clazz)
   {
      ele_.addClassName(clazz);
      
      // When moving elements to a resting state, clean them up entirely shortly
      // thereafter (this gives the fadeout animation time to run)
      if (state == LINE_RESTING && !restingTimer_.isRunning())
      {
         restingTimer_.schedule(500);
      }
      else if (restingTimer_.isRunning())
      {
         // If running the resting timer, cancel it since we are moving out of
         // the resting state
         restingTimer_.cancel();
      }
   }

   @Override
   protected void removeClazz()
   {
      ele_.setAttribute("class", "");
   }

   @Override
   protected void setTitle(String title)
   {
      ele_.setTitle(title);
   }

   public String getTitle()
   {
      return ele_.getTitle();
   }

   @Override
   protected void appendToTitle(String text)
   {
      String currentTitle = ele_.getTitle();
      ele_.setTitle(currentTitle + "\n" + text);
   }

   @Override
   public void detach()
   {
      // Detach from DOM
      if (attached_)
      {
         super.detach();
         ele_.removeFromParent();
         attached_ = false;
         if (restingTimer_.isRunning())
         {
            restingTimer_.cancel();
         }
      }

      // Detach from Ace instance
      if (anchor_ != null)
      {
         anchor_.detach();
      }

      // Remove all event handlers
      registrations_.removeHandler();
   }
   
   public void attach(Element parent)
   {
      parent.appendChild(ele_);
      attached_ = true;
      reposition();
   }
   
   public boolean attached()
   {
      return attached_;
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

   /**
    * Moves the row state decoration so it's next to the row of code with which it's associated.
    */
   private void reposition()
   {
      // If we're detached, no need to redraw anything.
      if (!attached_ || ele_ == null)
      {
         return;
      }

      // If we're still on the same row, no need to redraw anything
      int row = anchor_.getRow();
      if (row == row_)
      {
         return;
      }

      // We've moved; save the new row position
      row_ = row;

      // Get all the line groups in the editor; note that this only works if the
      // word wrapping is turned on (otherwise Ace doesn't create ace_line_group
      // elements), but we can rely on word wrapping since we enforce it in the
      // visual editor.
      Element[] lines = DomUtils.getElementsByClassName(
         editor_.asWidget().getElement(), "ace_line_group");

      if (row_ >= lines.length)
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
   }


   public final static String LINE_QUEUED_CLASS   = "visual_chunk-queued-line";
   public final static String LINE_EXECUTED_CLASS = "visual_chunk-executed-line";
   public final static String LINE_RESTING_CLASS  = "visual_chunk-resting-line";
   public final static String LINE_ERROR_CLASS    = "visual_chunk-error-line";

   private Timer restingTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         detach();
      }
   };
   
   private boolean attached_;
   private DivElement ele_;
   private int row_;

   private final Anchor anchor_;
   private final AceEditor editor_;
   private final HandlerRegistrations registrations_;
}
