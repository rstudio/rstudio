/*
 * PinnedLineWidget.java
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

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FoldChangeEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

/*
 * A pinned line widget binds an Ace LineWidget to a GWT widget. It provides
 * two features needed by most usages of LineWidgets:
 * 
 * - The LineWidget is "pinned" to its original line; a pair of anchors detects
 *   when the widget has been shifted by editing, and restores it to its
 *   original position.
 * - A notification is emitted to the host when Ace removes the LineWidget.
 */
public class PinnedLineWidget
             implements FoldChangeEvent.Handler
{
   public interface Host
   {
      void onLineWidgetAdded(LineWidget widget);
      void onLineWidgetRemoved(LineWidget widget);
   }

   public PinnedLineWidget(String type, DocDisplay display, Widget widget, 
         int row, JavaScriptObject data, Host host)
   {
      display_ = display;
      widget_ = widget;
      host_ = host;
      
      lastWidgetRow_ = row;
      moving_ = false;
      shiftPending_ = false;
      folded_ = folded();
      
      lineWidget_ = LineWidget.create(type, row, widget_.getElement(), data);
      lineWidget_.setFixedWidth(true); 

      renderLineWidget();
      
      // the Ace line widget manage emits a 'changeFold' event when a line
      // widget is destroyed; this is our only signal that it's been
      // removed, so when it happens, we need check to see if the widget
      // has been removed
      registrations_ = new HandlerRegistrations(
         display_.addFoldChangeHandler(this));

      startAnchor_ = display_.createAnchor(Position.create(row, 0));

      createEndAnchor();
   }
   
   public boolean moving()
   {
      return moving_;
   }
   
   public int getRow()
   {
      if (shiftPending_)
         return lastWidgetRow_;
      return lineWidget_.getRow();
   }
   
   public void detach()
   {
      detachAnchors();
      registrations_.removeHandler();
      display_.removeLineWidget(lineWidget_);
   }
   
   public LineWidget getLineWidget()
   {
      return lineWidget_;
   }
   
   public void reloadWidget()
   {
      parent_.setInnerHTML("");
      widget_.getElement().removeFromParent();
      parent_.appendChild(widget_.getElement());
   }
   
   // Event handlers ----------------------------------------------------------

   @Override
   public void onFoldChange(FoldChangeEvent event)
   {
      // check to see whether we just became hidden by a fold collapse
      if (folded_ != folded())
      {
         folded_ = folded();
         if (folded_)
         {
            widget_.setVisible(false);
         }
         else
         {
            widget_.setVisible(true);
         }
         
         // if this fold change event was a collapse, we know it can't also
         // be a widget deletion
         return;
      }
      
      // the FoldChangeEvent is fired at the moment that Ace removes the line
      // widget, but before our anchors are updated. set a flag so we know to
      // check for an attached widget next time.
      checkForRemove_ = true;
   }

   private void renderLineWidget()
   {
      // add the widget to the document
      display_.addLineWidget(lineWidget_);
      parent_ = lineWidget_.getElement().getParentElement();
      
      // notify host if available
      if (host_ != null)
         host_.onLineWidgetAdded(lineWidget_);
   }

   // Private methods ---------------------------------------------------------

   private void detachAnchors()
   {
      startAnchor_.detach();
      endAnchor_.detach();
   }
   
   private void syncEndAnchor()
   {
      // if a fold change occurred since the last time the anchor moved, check
      // to see if this widget needs to be removed entirely 
      if (checkForRemove_)
      {
         checkForRemove_ = false;
         if (checkForRemoval())
            return;
      }

      int delta = endAnchor_.getRow() - startAnchor_.getRow();
      
      if (shiftPending_)
      {
         // ignore if the shift hasn't happened yet
         if (lineWidget_.getRow() == lastWidgetRow_)
            return;

         // if a shift was pending, detach the anchors and move the widget to
         // its original position (below)
         shiftPending_ = false;
         detachAnchors();
         startAnchor_ = display_.createAnchor(Position.create(
               lastWidgetRow_, 0));
         delta = 2;
      }
      else if (startAnchor_.getRow()    == lineWidget_.getRow() && 
               endAnchor_.getRow()      == lineWidget_.getRow() &&
               startAnchor_.getColumn() == endAnchor_.getColumn())
      {
         // if the entire row of content is swapped out, the line widget can get
         // moved inappropriately; prior to this occurring, the anchors and the
         // widget collapse onto a single line. 
         shiftPending_ = true;
         return;
      } 
      else
      {
         lastWidgetRow_ = lineWidget_.getRow();
      }

      if (delta == 0)
      {
         // this happens if the line beneath the line widget is deleted; when
         // it happens, our anchors wind up on the same line, so reset the
         // end anchor
         endAnchor_.detach();
         createEndAnchor();
      }
      else if (delta > 1)
      {
         // this happens if a line is inserted between the chunk and the 
         // output; when it happens, we need to move the output so it remains
         // glued to the end of the chunk.
         
         // mark the chunk as moving (so we don't process this as an actual
         // chunk remove when events are triggered)
         moving_ = true;

         // move the chunk to the start anchor
         endAnchor_.detach();
         display_.removeLineWidget(lineWidget_);
         lineWidget_.setRow(startAnchor_.getRow());
         display_.addLineWidget(lineWidget_);
         parent_ = lineWidget_.getElement().getParentElement();
         createEndAnchor();

         // restore state
         moving_ = false;
      }
   }
   
   private void createEndAnchor()
   {
      endAnchor_ = display_.createAnchor(
            Position.create(startAnchor_.getRow() + 1, 0));
      endAnchor_.addOnChangeHandler(new Command()
      {
         @Override
         public void execute()
         {
            syncEndAnchor();
         }
      });
   }

   private boolean checkForRemoval()
   {
      LineWidget w = display_.getLineWidgetForRow(lineWidget_.getRow());
      if (w == null)
      {
         // ensure widget is detached
         widget_.getElement().removeFromParent();
         detachAnchors();
         if (host_ != null)
            host_.onLineWidgetRemoved(lineWidget_);
         return true;
      }
      return false;
   }
   
   private boolean folded()
   {
      String foldState = display_.getFoldState(lastWidgetRow_);
      return foldState != null;
   }
   
   private final DocDisplay display_;
   private final LineWidget lineWidget_;
   private final Widget widget_;
   private final Host host_;
   private final HandlerRegistrations registrations_;
   private Element parent_;
   private int lastWidgetRow_;

   private Anchor endAnchor_;
   private Anchor startAnchor_;

   private boolean moving_;
   private boolean checkForRemove_;
   private boolean shiftPending_;
   private boolean folded_;
}
