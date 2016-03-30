/*
 * PinnedLineWidget.java
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

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Anchor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FoldChangeEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

public class PinnedLineWidget
             implements FoldChangeEvent.Handler
{
   public interface Host
   {
      void onLineWidgetRemoved(LineWidget widget);
   }

   public PinnedLineWidget(String type, DocDisplay display, Widget widget, 
         int row, JavaScriptObject data, Host host)
   {
      display_ = display;
      widget_ = widget;
      moving_ = false;
      host_ = host;

      lineWidget_ = LineWidget.create(type, row, widget_.getElement(), data);
      lineWidget_.setFixedWidth(true); 
      display_.addLineWidget(lineWidget_);

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
      return lineWidget_.getRow();
   }
   
   public void detach()
   {
      detachAnchors();
      display_.removeLineWidget(lineWidget_);
   }
   
   public LineWidget getLineWidget()
   {
      return lineWidget_;
   }
   
   // Event handlers ----------------------------------------------------------

   @Override
   public void onFoldChange(FoldChangeEvent event)
   {
      // the FoldChangeEvent is fired at the moment that Ace removes the line
      // widget, but before our anchors are updated. set a flag so we know to
      // check for an attached widget next time.
      checkForRemove_ = true;
   }

   // Private methods ---------------------------------------------------------

   private void detachAnchors()
   {
      startAnchor_.detach();
      endAnchor_.detach();
      registrations_.removeHandler();
   }
   
   private void syncEndAnchor()
   {
      if (checkForRemove_)
      {
         checkForRemove_ = false;
         if (checkForRemoval())
            return;
      }

      int delta = endAnchor_.getRow() - startAnchor_.getRow();
      if (delta == 0)
      {
         // this happens if the line beneath the chunk output is deleted; when
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
         host_.onLineWidgetRemoved(lineWidget_);
         return true;
      }
      return false;
   }
   
   private final DocDisplay display_;
   private final Anchor startAnchor_;
   private final LineWidget lineWidget_;
   private final Widget widget_;
   private final Host host_;
   private final HandlerRegistrations registrations_;

   private boolean moving_;
   private boolean checkForRemove_;
   private Anchor endAnchor_;
}
