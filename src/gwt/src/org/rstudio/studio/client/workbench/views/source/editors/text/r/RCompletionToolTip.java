/*
 * RCompletionToolTip.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.r;

import java.util.ArrayList;

import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionToolTip;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Window;

public class RCompletionToolTip extends CppCompletionToolTip
{
   public RCompletionToolTip(DocDisplay docDisplay)
   {
      // save references
      docDisplay_ = docDisplay;

      // set the max width
      setMaxWidth(Window.getClientWidth() - 200);
      getElement().getStyle().setZIndex(10000);
   }
   
   public boolean previewKeyDown(NativeEvent event)
   {
      if (!isShowing())
         return false;
      
      if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
      {
         event.stopPropagation();
         event.preventDefault();
         hide();
         return true;
      }
      
      return false;
   }
   
   public void resolvePositionAndShow(String signature,
                                      Rectangle rectangle)
   {
      resolvePositionAndShow(
            signature,
            rectangle.getLeft(),
            rectangle.getTop());
   }
   
   public void resolvePositionAndShow(String signature)
   {
      setCursorAnchor();
      resolvePositionAndShow(signature, docDisplay_.getCursorBounds());
   }
   
   private String truncateSignature(String signature)
   {
      return truncateSignature(signature, 200);
   }
   
   private String truncateSignature(String signature, int length)
   {
      // Perform smart truncation -- look for a comma at or after the
      // length specified (so we don't cut argument names in half)
      if (signature.length() > length)
      {
         String truncated = signature;
         ArrayList<Integer> commaIndices = StringUtil.indicesOf(signature, ',');
         if (commaIndices.size() == 0)
         {
            truncated = signature.substring(0, length);
         }
         
         for (int i = 0; i < commaIndices.size(); i++)
         {
            int index = commaIndices.get(i);
            if (index >= length)
            {
               truncated = signature.substring(0, index + 1);
               break;
            }
         }
         
         return truncated + " <...truncated...> )";
      }
      
      return signature;
   }
   
   public void resolvePositionAndShow(String signature,
                                      int left,
                                      int top)
   {
      signature = truncateSignature(signature);
      if (signature != null)
         setText(signature);
      
      resolveWidth(signature);
      resolvePositionRelativeTo(left, top);
      
      setVisible(true);
      show();
      
   }
   
   private void resolveWidth(String signature)
   {
      if (signature.length() > 400)
         setWidth("800px");
      else if (signature.length() > 300)
         setWidth("700px");
      else if (signature.length() > 200)
         setWidth("600px");
      else
         setWidth(getOffsetWidth() + "px");
   }
   
   public void resolvePositionAndShow(String signature,
                                      Position displayPos)
   {
      resolvePositionAndShow(signature, docDisplay_.getPositionBounds(displayPos));
   }
   
   public void resolvePositionAndShow(String signature, Range activeRange)
   {
      setAnchor(activeRange.getStart(), activeRange.getEnd());
      resolvePositionAndShow(
            signature,
            docDisplay_.getPositionBounds(activeRange.getStart()));
   }
   
   private void resolvePositionRelativeTo(final int left,
                                          final int top)
   {
      // some constants
      final int H_PAD = 12;
      final int V_PAD = tooltipTopPadding(docDisplay_);
      final int H_BUFFER = 100;
      final int MIN_WIDTH = 300;
      
      // do we have enough room to the right? if not then
      int roomRight = Window.getClientWidth() - left;
      int maxWidth = Math.min(roomRight - H_BUFFER, 500);
      final boolean showLeft = maxWidth < MIN_WIDTH;
      if (showLeft)
         maxWidth = left - H_BUFFER;

      setMaxWidth(maxWidth);
      setPopupPositionAndShow(new PositionCallback(){

         @Override
         public void setPosition(int offsetWidth,
                                 int offsetHeight)
         {
            // if we are showing left then adjust
            int adjustedLeft = left;
            if (showLeft)
               adjustedLeft = left - offsetWidth - H_PAD;

            setPopupPosition(adjustedLeft, top - getOffsetHeight() - V_PAD);
         }
      });

   }
   
   @SuppressWarnings("unused")
   private void setAnchor(Position start, Position end)
   {
      int startCol = start.getColumn();
      if (startCol > 0)
         start.setColumn(start.getColumn() - 1);
      
      end.setColumn(end.getColumn() + 1);
      anchor_ = docDisplay_.createAnchoredSelection(start, end);
   }
   
   private void setCursorAnchor()
   {
      Position start = docDisplay_.getSelectionStart();
      start = Position.create(start.getRow(), start.getColumn() - 1);
      Position end = docDisplay_.getSelectionEnd();
      end = Position.create(end.getRow(), end.getColumn() + 1);
      anchor_ = docDisplay_.createAnchoredSelection(start, end);
   }
   
   @Override
   protected void onLoad()
   {
      super.onLoad();
      nativePreviewReg_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         public void onPreviewNativeEvent(NativePreviewEvent e)
         {
            int eventType = e.getTypeInt();
            if (eventType == Event.ONKEYDOWN ||
                eventType == Event.ONMOUSEDOWN)
            {
               // dismiss if we've left our anchor zone
               // (defer this so the current key has a chance to 
               // enter the editor and affect the cursor)
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                  @Override
                  public void execute()
                  {
                     Position cursorPos = docDisplay_.getCursorPosition();
                     if (anchor_ != null)
                     {
                        Range anchorRange = anchor_.getRange();

                        if (cursorPos.isBeforeOrEqualTo(anchorRange.getStart()) ||
                              cursorPos.isAfterOrEqualTo(anchorRange.getEnd()))
                        {
                           hide();
                        }
                     }
                  }
               });
            }
         }
      });
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      nativePreviewReg_.removeHandler();
   }
   
   public String getSignature()
   {
      return getLabel();
   }
   
   private final DocDisplay docDisplay_;
   private HandlerRegistration nativePreviewReg_;
   private AnchoredSelection anchor_;
   
}
