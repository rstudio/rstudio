/*
 * RCompletionToolTip.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.r;

import java.util.ArrayList;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner.Coordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionToolTip;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Window;

public class RCompletionToolTip extends CppCompletionToolTip
{
   public RCompletionToolTip(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      handlers_ = new HandlerRegistrations();

      getElement().getStyle().setZIndex(10000);
      
      addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (event.isAttached())
               attachHandlers();
            else
               detachHandlers();
         }
      });
   }
   
   private void attachHandlers()
   {
      handlers_.add(docDisplay_.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            hide();
         }
      }));
   }
   
   private void detachHandlers()
   {
      handlers_.removeHandler();
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
            (rectangle.getTop() + rectangle.getBottom()) / 2);
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
      
      // resolve the tooltip width (allow the left bounds to be
      // nudged if the tooltip would be too large to fit)
      left = resolveWidth(left, signature);
      
      // show the tooltip
      resolvePositionRelativeTo(left, top);
      setVisible(true);
      show();
      
   }
   
   private int resolveWidth(int left, String signature)
   {
      int targetWidth = 400;
      
      // adjust width based on size of signature
      if (signature.length() > 400)
         targetWidth = 700;
      else if (signature.length() > 300)
         targetWidth = 600;
      else if (signature.length() > 200)
         targetWidth = 500;
      
      // check for overflow
      if (targetWidth > (Window.getClientWidth() - left - 40))
      {
         // try nudging the 'left' value
         left = Window.getClientWidth() - targetWidth - 40;
         
         // if 'left' is now too small, force to larger and
         // clamp the target width
         if (left < 40)
         {
            left = 40;
            targetWidth = Window.getClientWidth() - 80;
         }
      }
      
      Style styles = getElement().getStyle();
      styles.setProperty("maxWidth", targetWidth + "px");
      
      return left;
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
      setPopupPositionAndShow(new PositionCallback()
      {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            Coordinates position = PopupPositioner.getPopupPosition(
                  offsetWidth,
                  offsetHeight,
                  left,
                  top,
                  8,
                  false);
            setPopupPosition(position.getLeft(), position.getTop());
         }
      });
   }
   
   private void setAnchor(Position start, Position end)
   {
      int startCol = start.getColumn();
      if (startCol > 0)
         start.setColumn(start.getColumn() - 1);
      
      end.setColumn(end.getColumn() + 1);
      if (anchor_ != null)
         anchor_.detach();
      anchor_ = docDisplay_.createAnchoredSelection(start, end);
   }
   
   private void setCursorAnchor()
   {
      Position start = docDisplay_.getSelectionStart();
      start = Position.create(start.getRow(), start.getColumn() - 1);
      Position end = docDisplay_.getSelectionEnd();
      end = Position.create(end.getRow(), end.getColumn() + 1);
      if (anchor_ != null)
         anchor_.detach();
      anchor_ = docDisplay_.createAnchoredSelection(start, end);
   }
   
   @Override
   protected void onLoad()
   {
      super.onLoad();
      if (nativePreviewReg_ != null)
      {
         nativePreviewReg_.removeHandler();
         nativePreviewReg_ = null;
      }
      
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
                           anchor_.detach();
                           anchor_ = null;
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
      if (nativePreviewReg_ != null)
      {
         nativePreviewReg_.removeHandler();
         nativePreviewReg_ = null;
      }
   }
   
   public String getSignature()
   {
      return getLabel();
   }
   
   private final DocDisplay docDisplay_;
   private final HandlerRegistrations handlers_;
   
   private HandlerRegistration nativePreviewReg_;
   private AnchoredSelection anchor_;
   
}
