package org.rstudio.studio.client.workbench.views.source.editors.text.r;

import org.rstudio.core.client.Rectangle;
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
   }
   
   public void previewKeyDown(NativeEvent event)
   {
      if (!isShowing())
         return;
      
      if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
      {
         hide();
         return;
      }
   }
   
   public void resolvePositionAndShow(String signature)
   {
      if (signature != null)
         setText(signature);
      
      setAnchor();
      resolvePositionRelativeTo(
            docDisplay_.getCursorBounds());
      
      setVisible(true);
      show();
   }
   
   private void resolvePositionRelativeTo(final Rectangle bounds)
   {
      // some constants
      final int H_PAD = 12;
      final int V_PAD = -3;
      final int H_BUFFER = 100;
      final int MIN_WIDTH = 300;
      
      final int left = bounds.getLeft();
      final int top = bounds.getTop();

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
            {
               adjustedLeft = getAbsoluteLeft() -
                     offsetWidth - H_PAD;
            }

            setPopupPosition(adjustedLeft, top - getOffsetHeight() + V_PAD);
         }
      });

   }
   
   private void setAnchor()
   {
      cursorBounds_ = docDisplay_.getCursorBounds();
      Position start = docDisplay_.getSelectionStart();
      start = Position.create(start.getRow(), start.getColumn() - 1);
      Position end = docDisplay_.getSelectionEnd();
      end = Position.create(end.getRow(), end.getColumn() + 1);
      anchor_ = docDisplay_.createAnchoredSelection(start, end);
   }
   
   // TODO: Refactor into common base class (e.g. AnchoredToolTip)
   @Override
   protected void onLoad()
   {
      super.onLoad();
      nativePreviewReg_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         public void onPreviewNativeEvent(NativePreviewEvent e)
         {
            if (e.getTypeInt() == Event.ONKEYDOWN)
            {
               // dismiss if we've left our anchor zone
               // (defer this so the current key has a chance to 
               // enter the editor and affect the cursor)
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                  @Override
                  public void execute()
                  {
                     Position cursorPos = docDisplay_.getCursorPosition();
                     Range anchorRange = anchor_.getRange();
                     
                     if (cursorPos.isBeforeOrEqualTo(anchorRange.getStart()) ||
                         cursorPos.isAfterOrEqualTo(anchorRange.getEnd()))
                     {
                        hide();
                     }
                  }
               });
            }
         }
      });
   }
   
   
   private final DocDisplay docDisplay_;
   
   private int parenBalance_ = 0;
   private Rectangle cursorBounds_;
   private AnchoredSelection anchor_;
   private HandlerRegistration nativePreviewReg_;

}
