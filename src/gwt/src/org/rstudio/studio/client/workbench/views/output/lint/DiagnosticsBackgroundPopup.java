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
import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Marker;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Markers;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
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
            
            // If the document was modified recently, bail
            if ((currentTime - lastModifiedTime) < 2000)
               return completeExecution();
            
            Markers markers = editor_.getSession().getMarkers(true);
            for (int i = 0; i < markers.size(); i++)
            {
               Marker marker = markers.get(i);
               if (marker.getRange().contains(docDisplay_.getCursorPosition()))
               {
                  displayMarkerDiagnostics(marker);
                  return completeExecution();
               }
            }
            
            return completeExecution();
         }
      }, 2000);
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
            showPopupWithText(annotation.text());
            return;
         }
      }
   }
   
   static class DiagnosticsPopupPanel extends PopupPanel
   {
      public DiagnosticsPopupPanel(String text)
      {
         super(true, false);
         setWidget(new Label(text));
      }
   }
   
   private void showPopupWithText(String text)
   {
      final PopupPanel popup = new DiagnosticsPopupPanel(text);
      popup.setTitle("Diagnostics");
      popup.setPopupPositionAndShow(new PositionCallback()
      {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            Rectangle pos = docDisplay_.getCursorBounds();
            popup.setPopupPosition(pos.getLeft(), pos.getTop());
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
   private boolean isRunning_;
}
