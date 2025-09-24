/*
 * DiagnosticsBackgroundPopup.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output.lint;

import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsMap;
import org.rstudio.studio.client.workbench.views.output.OutputConstants;
import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Marker;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.HTML;
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
               start();
         }
      });

      docDisplay_.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            hidePopup();
            stopMonitoring();
            if (handler_ != null)
            {
               handler_.removeHandler();
               handler_ = null;
            }
         }
      });

      stopRequested_ = false;
      popup_ = null;
      start();
   }

   public void start()
   {
      isRunning_ = true;
      stopRequested_ = false;

      if (handler_ != null)
      {
         handler_.removeHandler();
         handler_ = null;
      }

      handler_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent event)
         {
            if (event.getTypeInt() == Event.ONMOUSEMOVE)
            {
               lastMouseMoveTime_ = System.currentTimeMillis();
               movedMouseMostRecently_ = true;

               EventTarget eventTarget = event.getNativeEvent().getEventTarget();
               if (!Element.is(eventTarget))
                  return;
               
               Element target = Element.as(eventTarget);
               if (target.hasClassName("ace_gutter-cell"))
               {
                  lastMouseCoords_ = null;
                  hidePopup();
                  return;
               }

               lastMouseCoords_ = ScreenCoordinates.create(
                     event.getNativeEvent().getClientX(),
                     event.getNativeEvent().getClientY());

               if (activeMarker_ != null &&
                     !activeMarker_.getRange().containsRightExclusive(
                           editor_.toDocumentPosition(lastMouseCoords_)))
               {
                  hidePopup();
               }
            }
            else
            {
               movedMouseMostRecently_ = false;
            }
         }
      });

      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         @Override
         public boolean execute()
         {
            if (stopRequested_)
               return stopExecution();

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

            Position currentPos;
            if (movedMouseMostRecently_)
            {
               if (lastMouseCoords_ == null)
                  return completeExecution();

               if ((currentTime - lastMouseMoveTime_) < 500)
                  return completeExecution();

               currentPos = editor_.toDocumentPosition(lastMouseCoords_);
            }
            else
            {
               currentPos = docDisplay_.getCursorPosition();
            }
            
            JsMap<Marker> markers = editor_.getSession().getMarkers(true);
            JsArray<Marker> markerValues = markers.values();
            for (int i = 0; i < markerValues.length(); i++)
            {
               Marker marker = markerValues.get(i);
               if (marker.getRange().containsRightExclusive(currentPos))
               {
                  displayMarkerDiagnostics(marker);
                  return completeExecution();
               }
            }

            hidePopup();
            return completeExecution();
         }
      }, 100);
   }

   private void displayMarkerDiagnostics(Marker marker)
   {
      if (StringUtil.equals(marker.getClazz(), LintResources.INSTANCE.styles().spelling()))
      {
         hidePopup();
         return;
      }

      JsArray<AceAnnotation> annotations = editor_.getAnnotations();
      for (int i = 0; i < annotations.length(); i++)
      {
         AceAnnotation annotation = annotations.get(i);
         int row = annotation.row();
         if (marker.getRange().getStart().getRow() <= row &&
             marker.getRange().getEnd().getRow() >= row)
         {
            activeMarker_ = marker;
            String text = annotation.html();
            if (StringUtil.isNullOrEmpty(text))
               text = annotation.text();
            showPopup(text, marker.getRange());

            return;
         }
      }
   }

   private class DiagnosticsPopupPanel extends PopupPanel
   {
      public DiagnosticsPopupPanel(
            String text,
            Range range)
      {
         super(true, false);
         range_ = range;
         editor_.addCursorChangedHandler(new CursorChangedEvent.Handler()
         {
            @Override
            public void onCursorChanged(CursorChangedEvent event)
            {
               if (!range_.containsRightExclusive(event.getPosition()))
                  hide();
            }
         });
         addStyleName(RES.styles().popup());
         setWidget(new HTML(text));
      }

      private final Range range_;
   }

   private void showPopup(String text, Range range)
   {
      hidePopup();
      popup_ = new DiagnosticsPopupPanel(text, range);
      final Rectangle coords = editor_.toScreenCoordinates(range);
      popup_.setTitle(constants_.diagnosticsLabel());
      popup_.setPopupPositionAndShow(new PositionCallback()
      {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            popup_.setPopupPosition(
                  coords.getRight() + 10,
                  coords.getBottom());
         }
      });
   }

   private void hidePopup()
   {
      activeMarker_ = null;
      if (popup_ != null)
      {
         popup_.hide();
         popup_ = null;
      }
   }

   private boolean completeExecution()
   {
      return true;
   }

   private boolean stopExecution()
   {
      isRunning_ = false;
      stopRequested_ = false;
      activeMarker_ = null;
      return false;
   }

   private void stopMonitoring()
   {
      stopRequested_ = true;
   }

   public interface Resources extends ClientBundle
   {
      public static interface Styles extends CssResource
      {
         String popup();
      }

      @Source("DiagnosticsBackgroundPopup.css")
      Styles styles();

      public static Resources INSTANCE =
            (Resources) GWT.create(Resources.class);
   }

   private final DocDisplay docDisplay_;
   private final AceEditor editor_;
   private DiagnosticsPopupPanel popup_;
   private boolean isRunning_;
   private boolean stopRequested_;
   private Marker activeMarker_;

   private ScreenCoordinates lastMouseCoords_;
   private HandlerRegistration handler_;
   private static final Resources RES = Resources.INSTANCE;

   private long lastMouseMoveTime_;
   private boolean movedMouseMostRecently_;

   static {
      RES.styles().ensureInjected();
   }
   private static final OutputConstants constants_ = com.google.gwt.core.client.GWT.create(OutputConstants.class);

}
