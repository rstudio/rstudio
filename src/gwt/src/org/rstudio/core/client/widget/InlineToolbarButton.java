/*
 * InlineToolbarButton.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

import org.rstudio.core.client.StringUtil;

public class InlineToolbarButton extends Composite implements HasButtonMethods
{
   private class SimpleHasHandlers extends HandlerManager implements HasHandlers
   {
      private SimpleHasHandlers()
      {
         super(null);
      }
   }

   interface Binder extends UiBinder<Widget, InlineToolbarButton>
   {}

   @UiConstructor
   public InlineToolbarButton(ImageResource icon,
                              String label,
                              String description)
   {
      icon_ = new Image(icon);
      label_ = new SpanLabel(label, false);
      zeroHeightPanel_ = new ZeroHeightPanel(icon.getWidth(),
                                             icon.getHeight(),
                                             4);

      if (StringUtil.isNullOrEmpty(label))
         label_.setVisible(false);

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      setTitle(description);
   }
   
   public void setLabel(String text)
   {
      label_.setText(text);
      label_.setVisible(!StringUtil.isNullOrEmpty(text));
   }
   
   public void setMenu(final ToolbarPopupMenu menu)
   {
      /*
       * We want clicks on this button to toggle the visibility of the menu,
       * as well as having the menu auto-hide itself as it normally does.
       * It's necessary to manually track the visibility (menuShowing) because
       * in the case where the menu is showing, clicking on this button first
       * causes the menu to auto-hide and then our mouseDown handler is called
       * (so we can't rely on menu.isShowing(), it'll always be false by the
       * time you get into the mousedown handler).
       */

      final boolean[] menuShowing = new boolean[1];

      addDomHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            if (menuShowing[0])
            {
               menu.hide();
            }
            else
            {
               menu.setPopupPositionAndShow(new PositionCallback() {
                  @Override
                  public void setPosition(int offsetWidth, int offsetHeight)
                  {
                     menu.setPopupPosition(
                        InlineToolbarButton.this.getAbsoluteLeft(), 
                        InlineToolbarButton.this.getAbsoluteTop() +
                        InlineToolbarButton.this.getOffsetHeight() + 
                        8 /* toolbar area under botton */);
                  } 
               });
            }
            menuShowing[0] = true;
         }
      }, MouseDownEvent.getType());

      menu.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  menuShowing[0] = false;               
               }
            });
         }
      });
   }


   @Override
   public HandlerRegistration addClickHandler(ClickHandler clickHandler)
   {
      /*
       * When we directly subscribe to this widget's ClickEvent, sometimes the
       * click gets ignored (inconsistent repro but it happens enough to be
       * annoying). Doing it this way fixes it.
       */

      hasHandlers_.addHandler(ClickEvent.getType(), clickHandler);

      final HandlerRegistration mouseDown = addDomHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            down_ = true;
         }
      }, MouseDownEvent.getType());

      final HandlerRegistration mouseOut = addDomHandler(new MouseOutHandler()
      {
         public void onMouseOut(MouseOutEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            down_ = false;
         }
      }, MouseOutEvent.getType());

      final HandlerRegistration mouseUp = addDomHandler(new MouseUpHandler()
      {
         public void onMouseUp(MouseUpEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            if (down_)
            {
               down_ = false;

               NativeEvent clickEvent = Document.get().createClickEvent(
                     1,
                     event.getScreenX(),
                     event.getScreenY(),
                     event.getClientX(),
                     event.getClientY(),
                     event.getNativeEvent().getCtrlKey(),
                     event.getNativeEvent().getAltKey(),
                     event.getNativeEvent().getShiftKey(),
                     event.getNativeEvent().getMetaKey());
               DomEvent.fireNativeEvent(clickEvent, hasHandlers_);
            }
         }
      }, MouseUpEvent.getType());

      return new HandlerRegistration()
      {
         public void removeHandler()
         {
            mouseDown.removeHandler();
            mouseOut.removeHandler();
            mouseUp.removeHandler();
         }
      };
   }

   public void click()
   {
      NativeEvent clickEvent = Document.get().createClickEvent(
            1,
            0,
            0,
            0,
            0,
            false,
            false,
            false,
            false);
      DomEvent.fireNativeEvent(clickEvent, hasHandlers_);
   }

   @UiField(provided = true)
   Image icon_;

   @UiField(provided = true)
   SpanLabel label_;

   @UiField(provided = true)
   ZeroHeightPanel zeroHeightPanel_;

   private SimpleHasHandlers hasHandlers_ = new SimpleHasHandlers();
   private boolean down_;
}
