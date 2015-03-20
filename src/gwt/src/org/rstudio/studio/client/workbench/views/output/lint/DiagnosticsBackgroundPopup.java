/*
 * DiagnosticsBackgroundPopup.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.lint;

import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Marker;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Markers;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

public class DiagnosticsBackgroundPopup
{
   public DiagnosticsBackgroundPopup(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      editor_ = (AceEditor) docDisplay_;
      docDisplay_.addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            if (editor_ != null && !isRunning_)
               DiagnosticsBackgroundPopup.this.start();
         }
      });
      
      docDisplay_.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            if (popup_ != null)
               popup_.hide();
         }
      });
      
      popup_  = null;
      start();
   }
   
   public void start()
   {
      isRunning_ = true;
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         
         @Override
         public boolean execute()
         {
            // If the document is not focused, stop execution (can be
            // restarted later)
            if (!docDisplay_.isFocused())
               return stopExecution();
            
            long currentTime = System.currentTimeMillis();
            long lastModifiedTime = docDisplay_.getLastModifiedTime();
            long lastCursorChangedTime = docDisplay_.getLastCursorChangedTime();
            
            // If the document was modified recently, or the
            // cursor was moved recently, then bail
            if ((currentTime - lastModifiedTime) < 500)
               return completeExecution();
            
            if ((currentTime - lastCursorChangedTime) < 500)
               return completeExecution();
            
            Markers markers = editor_.getSession().getMarkers(true);
            int[] keys = markers.getIds();
            for (int i = 0; i < keys.length; i++)
            {
               Marker marker = markers.get(keys[i]);
               if (marker.getRange().contains(docDisplay_.getCursorPosition()))
               {
                  displayMarkerDiagnostics(marker);
                  return completeExecution();
               }
            }
            
            return completeExecution();
         }
      }, 500);
   }
   
   private void displayMarkerDiagnostics(Marker marker)
   {
      JsArray<AceAnnotation> annotations = editor_.getAnnotations();
      for (int i = 0; i < annotations.length(); i++)
      {
         AceAnnotation annotation = annotations.get(i);
         int row = annotation.row();
         if (marker.getRange().getStart().getRow() <= row &&
             marker.getRange().getEnd().getRow() >= row)
         {
            Rectangle coords = editor_.toScreenCoordinates(
                  marker.getRange());
            
            showPopup(annotation.text(), marker.getRange());
            return;
         }
      }
   }
   
   class DiagnosticsPopupPanel extends ThemedPopupPanel
   {
      public DiagnosticsPopupPanel(
            String text,
            Range range)
      {
         super(true, false);
         range_ = range;
         editor_.addCursorChangedHandler(new CursorChangedHandler()
         {
            @Override
            public void onCursorChanged(CursorChangedEvent event)
            {
               if (!range_.contains(event.getPosition()))
                  hide();
            }
         });
         setWidget(new Label(text));
      }
      
      public void hide()
      {
         super.hide();
      }
      
      private final Range range_;
   }
   
   private void showPopup(String text, Range range)
   {
      if (popup_ != null)
         popup_.hide();
      
      popup_ = new DiagnosticsPopupPanel(text, range);
      final Rectangle coords = editor_.toScreenCoordinates(range);
      popup_.setTitle("Diagnostics");
      popup_.setPopupPositionAndShow(new PositionCallback()
      {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            popup_.setPopupPosition(
                  coords.getRight() + 20,
                  coords.getTop());
         }
      });
   }
   
   private boolean completeExecution()
   {
      return true;
   }
   
   private boolean stopExecution()
   {
      isRunning_ = false;
      return false;
   }
   
   private final DocDisplay docDisplay_;
   private final AceEditor editor_;
   private DiagnosticsPopupPanel popup_;
   private boolean isRunning_;
}
