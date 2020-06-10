/*
 * MiniPopupPanel.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.Point;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;

public class MiniPopupPanel extends DecoratedPopupPanel
{
   public MiniPopupPanel()
   {
      super();
      commonInit(true);
   }

   public MiniPopupPanel(boolean autoHide)
   {
      super(autoHide);
      commonInit(true);
   }

   public MiniPopupPanel(boolean autoHide, boolean modal)
   {
      super(autoHide, modal);
      commonInit(true);
   }
   
   public MiniPopupPanel(boolean autoHide, boolean modal, boolean useStyleSheet)
   {
      super(autoHide, modal);
      commonInit(useStyleSheet);
   }
   
   public void positionNearRange(DocDisplay display, Range range)
   {
      Rectangle bounds = display.getRangeBounds(range);
      Point center = bounds.center();
      
      int pageX = center.getX() - (getOffsetWidth() / 2);

      // prefer displaying popup below associated text, but place above text
      // if it won't fit below
      int pageY = bounds.getBottom() + 10;
      if (pageY + getOffsetHeight() > display.getBounds().getBottom())
      {
         pageY = bounds.getTop() - 10 - getOffsetHeight();
      }
      
      // avoid leaking off left side of page
      pageX = Math.max(20, pageX);
      pageY = Math.max(20, pageY);
      
      setPopupPosition(pageX, pageY);
   }
   
   @Override
   public void show()
   {
      addDragHandler();
      addEscHandler();
      super.show();
   }
   
   @Override
   public void hide()
   {
      removeDragHandler();
      removeEscHandler();
      super.hide();
   }
   
   private void commonInit(boolean useStyleSheet)
   {
      if (useStyleSheet)
      {
         addStyleName(RES.styles().popupPanel());
      }
   }
   
   private void addDragHandler()
   {
      if (dragHandler_ != null)
         dragHandler_.removeHandler();
      
      dragHandler_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent npe)
         {
            if (npe.getNativeEvent().getButton() != NativeEvent.BUTTON_LEFT)
               return;
            
            int type = npe.getTypeInt();
            if (type == Event.ONMOUSEDOWN ||
                type == Event.ONMOUSEMOVE ||
                type == Event.ONMOUSEUP)
            {
               if (dragging_)
               {
                  handleDrag(npe);
                  return;
               }
               
               Element target = npe.getNativeEvent().getEventTarget().cast();
               
               String tagName = target.getTagName().toLowerCase();
               for (String tag : TAGS_EXCLUDE_DRAG)
                  if (tagName == tag)
                     return;
               
               if (DomUtils.isDescendantOfElementWithTag(target, TAGS_EXCLUDE_DRAG))
                  return;

               Element self = MiniPopupPanel.this.getElement();
               if (!DomUtils.isDescendant(target, self))
                  return;
               
               handleDrag(npe);
            }
         }
      });
   }
   
   private void handleDrag(NativePreviewEvent npe)
   {
      NativeEvent event = npe.getNativeEvent();
      int type = npe.getTypeInt();
      
      switch (type)
      {
         case Event.ONMOUSEDOWN:
         {
            beginDrag(event);
            event.stopPropagation();
            event.preventDefault();
            break;
         }
         
         case Event.ONMOUSEMOVE:
         {
            if (dragging_)
            {
               drag(event);
               event.stopPropagation();
               event.preventDefault();
            }
            break;
         }
         
         case Event.ONMOUSEUP:
         case Event.ONLOSECAPTURE:
         {
            if (dragging_ && didDrag_)
            {
               event.stopPropagation();
               event.preventDefault();
            }
            
            endDrag(event);
            break;
         }
         
      }
   }
   
   private void drag(NativeEvent event)
   {
      int newMouseX = event.getClientX();
      int newMouseY = event.getClientY();
      
      int diffX = newMouseX - initialMouseX_;
      int diffY = newMouseY - initialMouseY_;
      
      int maxRight = Window.getClientWidth() - this.getOffsetWidth();
      int maxBottom = Window.getClientHeight() - this.getOffsetHeight();
      
      if (diffX != 0 || diffY != 0)
         didDrag_ = true;
      
      setPopupPosition(
            clamp(initialPopupLeft_ + diffX, 0, maxRight),
            clamp(initialPopupTop_ + diffY, 0, maxBottom));
   }
   
   private int clamp(int value, int low, int high)
   {
      if (value < low) return low;
      else if (value > high) return high;
      return value;
   }
   
   private void beginDrag(NativeEvent event)
   {
      DOM.setCapture(getElement());
      dragging_ = true;
      didDrag_ = false;
      
      initialMouseX_ = event.getClientX();
      initialMouseY_ = event.getClientY();
      
      initialPopupLeft_ = getPopupLeft();
      initialPopupTop_ = getPopupTop();
   }
   
   private void endDrag(NativeEvent event)
   {
      DOM.releaseCapture(getElement());
      dragging_ = false;
      
      // Suppress click events fired immediately after a drag end
      if (didDrag_)
      {
         if (clickAfterDragHandler_ != null)
            clickAfterDragHandler_.removeHandler();

         clickAfterDragHandler_ =
               Event.addNativePreviewHandler(new NativePreviewHandler()
               {

                  @Override
                  public void onPreviewNativeEvent(NativePreviewEvent event)
                  {
                     if (event.getTypeInt() == Event.ONCLICK)
                        event.cancel();

                     clickAfterDragHandler_.removeHandler();
                  }
               });
      }
   }
   
   private void removeDragHandler()
   {
      if (dragHandler_ != null)
      {
         dragHandler_.removeHandler();
         dragHandler_ = null;
      }
   }

   private void addEscHandler()
   {
      escapeHandler_ = Event.addNativePreviewHandler(nativePreviewEvent ->
      {
         if (nativePreviewEvent.getTypeInt() == Event.ONKEYDOWN &&
            nativePreviewEvent.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE)
         {
            nativePreviewEvent.cancel();
            escapeHandler_.removeHandler();
            escapeHandler_ = null;
            hide();
         }
      });
   }

   private void removeEscHandler()
   {
      if (escapeHandler_ != null)
      {
         escapeHandler_.removeHandler();
         escapeHandler_ = null;
      }
   }

   private int initialPopupLeft_ = 0;
   private int initialPopupTop_ = 0;
   
   private int initialMouseX_ = 0;
   private int initialMouseY_ = 0;
   
   private boolean dragging_ = false;
   private boolean didDrag_ = false;
   
   private HandlerRegistration dragHandler_;
   private HandlerRegistration clickAfterDragHandler_;
   private HandlerRegistration escapeHandler_;
   
   private static final String[] TAGS_EXCLUDE_DRAG = new String[] {
      "a", "input", "button", "select"
   };
   
   // Styles ------------------------------------------
   
   public interface Styles extends CssResource
   {
      String popupPanel();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("MiniPopupPanel.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }

}
