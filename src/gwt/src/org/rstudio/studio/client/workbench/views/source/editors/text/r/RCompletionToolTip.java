package org.rstudio.studio.client.workbench.views.source.editors.text.r;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionToolTip;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;

public class RCompletionToolTip extends CppCompletionToolTip
{
   public RCompletionToolTip(DocDisplay docDisplay)
   {
      addCloseHandler(new CloseHandler<PopupPanel>()
      {
         @Override
         public void onClose(CloseEvent<PopupPanel> event)
         {
            reset();
         }
      });
      docDisplay_ = docDisplay;
   }
   
   public void previewKeyPress(char c)
   {
      if (!isShowing())
         return;
      
      if (c == '{')
      {
         reset();
         return;
      }
      
      if (updateParenCount(c))
         return;
      
   }
   
   public void previewKeyDown(NativeEvent event)
   {
      if (!isShowing())
         return;
      
      if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
      {
         reset();
         return;
      }
      
      if (event.getKeyCode() == KeyCodes.KEY_RIGHT)
      {
         char ch = docDisplay_.getCharacterAtCursor();
         updateParenCount(ch);
      }
      
      if (event.getKeyCode() == KeyCodes.KEY_BACKSPACE)
      {
         if (StringUtil.isNullOrEmpty(docDisplay_.getSelectionValue()))
         {
            char ch = docDisplay_.getCharacterBeforeCursor();
            updateParenCount(ch, true);
         }
      }
   }
   
   // Returns true if the popup was reset
   private boolean updateParenCount(char ch,
                                    boolean reverse)
   {
      char open = reverse ? ')' : '(';
      char close = reverse ? '(' : ')';
      
      if (ch == open)
         ++parenBalance_;
      
      if (ch == close)
      {
         if (parenBalance_ <= 0)
         {
            reset();
            return true;
         }
         --parenBalance_;
      }
      return false;
   }
   
   private boolean updateParenCount(char ch)
   {
      return updateParenCount(ch, false);
   }
   
   public void reset()
   {
      parenBalance_ = 0;
      hide();
   }
   
   public void resolvePositionRelativeTo(final int left, final int top)
   {
      // some constants
      final int H_PAD = 12;
      final int V_PAD = -3;
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
            {
               adjustedLeft = getAbsoluteLeft() -
                     offsetWidth - H_PAD;
            }

            setPopupPosition(adjustedLeft, top - getOffsetHeight() + V_PAD);
         }
      });

   }
   
   private int parenBalance_ = 0;
   private final DocDisplay docDisplay_;

}
