/*
 * ToolbarButton.java
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
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.*;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.ImageResourceProvider;
import org.rstudio.core.client.command.SimpleImageResourceProvider;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;


public class ToolbarButton extends FocusWidget
{
   private class SimpleHasHandlers extends HandlerManager implements HasHandlers
   {
      private SimpleHasHandlers()
      {
         super(null);
      }
   }
   
   public <T extends EventHandler> ToolbarButton(
                                      String text, 
                                      ImageResource leftImg, 
                                      final HandlerManager eventBus,
                                      final GwtEvent<? extends T> targetEvent)
   {
      this(text, leftImg, new ClickHandler() {
         public void onClick(ClickEvent event)
         {
           eventBus.fireEvent(targetEvent);
         }
      });
   }
   
   public <T extends EventHandler> ToolbarButton(
                                       ImageResource img, 
                                       final HandlerManager eventBus,
                                       final GwtEvent<? extends T> targetEvent)
   {
      this(null, img, eventBus, targetEvent);
   }
   
   public ToolbarButton(String text, 
                        ImageResourceProvider leftImageProvider,
                        ClickHandler clickHandler)
   {
      this(text, leftImageProvider, null, clickHandler);
   }
   
   public ToolbarButton(String text, 
                        ImageResource leftImage,
                        ClickHandler clickHandler)
   {
      this(text, new SimpleImageResourceProvider(leftImage), clickHandler);
   }
   
   public ToolbarButton(ImageResource image,
                        ClickHandler clickHandler)
   {
      this(null, image, clickHandler);
   }
   
   public ToolbarButton(ToolbarPopupMenu menu, boolean rightAlignMenu)
   {
      this((String)null, 
           new ImageResource2x(ThemeResources.INSTANCE.menuDownArrow2x()), 
           (ImageResource) null,
           (ClickHandler) null);
      
      addMenuHandlers(menu, rightAlignMenu);
      
      addStyleName(styles_.toolbarButtonMenu());
      addStyleName(styles_.toolbarButtonMenuOnly());
   }
      
   public ToolbarButton(String text, 
                        ImageResource leftImage,
                        ToolbarPopupMenu menu)
   {
      this(text, leftImage, menu, false);
   }
   
   public ToolbarButton(String text,
                        ImageResourceProvider leftImage,
                        ToolbarPopupMenu menu)
   {
      this(text, leftImage, menu, false);
   }
    
   public ToolbarButton(String text, 
                        ImageResource leftImage,
                        ToolbarPopupMenu menu,
                        boolean rightAlignMenu)
   {
      this(text,
           new SimpleImageResourceProvider(leftImage),
           menu, 
           rightAlignMenu);
   }

   public ToolbarButton(String text, 
                        ImageResourceProvider leftImage,
                        ToolbarPopupMenu menu,
                        boolean rightAlignMenu)
   {
      this(text, leftImage, new ImageResource2x(ThemeResources.INSTANCE.menuDownArrow2x()), null);

      addMenuHandlers(menu, rightAlignMenu);
      
      addStyleName(styles_.toolbarButtonMenu());
   }
   
   
   private void addMenuHandlers(final ToolbarPopupMenu popupMenu, 
                                final boolean rightAlign)
   {
      menu_ = popupMenu;
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

      addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
            event.stopPropagation();
            addStyleName(styles_.toolbarButtonPushed());
            // Some menus are rebuilt on every invocation. Ask the menu for 
            // the most up-to-date version before proceeding.
            popupMenu.getDynamicPopupMenu(
               new ToolbarPopupMenu.DynamicPopupMenuCallback()
            {
               @Override
               public void onPopupMenu(final ToolbarPopupMenu menu)
               {
                  if (menuShowing[0])
                  {
                     removeStyleName(styles_.toolbarButtonPushed());
                     menu.hide();
                  }
                  else
                  {
                     if (rightAlign)
                     {
                        menu.setPopupPositionAndShow(new PositionCallback() 
                        {
                           @Override
                           public void setPosition(int offsetWidth, 
                                                   int offsetHeight)
                           {
                              menu.setPopupPosition(
                                 (rightImageWidget_ != null ?
                                       rightImageWidget_.getAbsoluteLeft() :
                                       leftImageWidget_.getAbsoluteLeft())
                                 + 20 - offsetWidth, 
                                 ToolbarButton.this.getAbsoluteTop() +
                                 ToolbarButton.this.getOffsetHeight());
                           } 
                        });
                     }
                     else
                     {
                        menu.showRelativeTo(ToolbarButton.this);
                     }
                     menuShowing[0] = true;
                  }
               }
            });
         }
      });
      popupMenu.addCloseHandler(new CloseHandler<PopupPanel>()
      {
         public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
         {
            removeStyleName(styles_.toolbarButtonPushed());
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

   private ToolbarButton(String text,
                         ImageResource leftImage,
                         ImageResource rightImage,
                         ClickHandler clickHandler)
   {
      this(text,
           new SimpleImageResourceProvider(leftImage),
           rightImage,
           clickHandler);
   }
   
   public ToolbarButton(String text,
                        Image leftImage,
                        ImageResource rightImage,
                        ClickHandler clickHandler)
   {
      super();

      setElement(binder.createAndBindUi(this));

      this.setStylePrimaryName(styles_.toolbarButton());
      this.addStyleName(styles_.handCursor());

      setText(text);
      setInfoText(null);
      leftImageWidget_ = leftImage == null ? new Image() : leftImage;
      leftImageWidget_.setStylePrimaryName(styles_.toolbarButtonLeftImage());
      leftImageCell_.appendChild(leftImageWidget_.getElement());
      if (rightImage != null)
      {
         rightImageWidget_ = new Image(rightImage);
         rightImageWidget_.setStylePrimaryName(styles_.toolbarButtonRightImage());
         rightImageCell_.appendChild(rightImageWidget_.getElement());
      }

      if (clickHandler != null)
         addClickHandler(clickHandler);
   }
   
   public ToolbarButton(String text, 
                        ImageResourceProvider leftImage,
                        ImageResource rightImage,
                        ClickHandler clickHandler)
   {
      this(text, 
           // extract the supplied left image 
           leftImage != null && leftImage.getImageResource() != null ?
               new Image(leftImage.getImageResource()) :
               new Image(), 
           rightImage, 
           clickHandler);

      // let the image provider know it has a rendered copy if we made one
      if (leftImage != null && leftImageWidget_ != null)
         leftImage.addRenderedImage(leftImageWidget_);
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

      final HandlerRegistration mouseDown = addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            addStyleName(styles_.toolbarButtonPushed());
            down_ = true;
         }
      });

      final HandlerRegistration mouseOut = addMouseOutHandler(new MouseOutHandler()
      {
         public void onMouseOut(MouseOutEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            removeStyleName(styles_.toolbarButtonPushed());
            down_ = false;
         }
      });

      final HandlerRegistration mouseUp = addMouseUpHandler(new MouseUpHandler()
      {
         public void onMouseUp(MouseUpEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            if (down_)
            {
               down_ = false;
               removeStyleName(styles_.toolbarButtonPushed());

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
      });

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

   protected Toolbar getParentToolbar()
   {
      Widget parent = getParent();
      while (parent != null)
      {
         if (parent instanceof Toolbar)
            return (Toolbar) parent;
         parent = parent.getParent();
      }

      return null;
   }

   public ToolbarPopupMenu getMenu()
   {
      return menu_;
   }

   public void setLeftImage(ImageResource imageResource)
   {
      leftImageWidget_.setResource(imageResource);
   }

   public void setText(String label)
   {
      if (!StringUtil.isNullOrEmpty(label))
      {
         label_.setInnerText(label);
         label_.getStyle().setDisplay(Display.BLOCK);
         removeStyleName(styles_.noLabel());
      }
      else
      {
         label_.getStyle().setDisplay(Display.NONE);
         addStyleName(styles_.noLabel());
      }
   }
   
   public void setInfoText(String infoText)
   {
      if (!StringUtil.isNullOrEmpty(infoText))
      {
         infoLabel_.setInnerText(infoText);
         infoLabel_.getStyle().setDisplay(Display.BLOCK);
      }
      else
      {
         infoLabel_.getStyle().setDisplay(Display.NONE);
      }
   }
   
   public String getText()
   {
      return StringUtil.notNull(label_.getInnerText());
   }

   private boolean down_;
   
   private SimpleHasHandlers hasHandlers_ = new SimpleHasHandlers();
   
   interface Binder extends UiBinder<Element, ToolbarButton> { }

   private ToolbarPopupMenu menu_;
   private static final Binder binder = GWT.create(Binder.class);

   private static final ThemeStyles styles_ = ThemeResources.INSTANCE.themeStyles();

   @UiField
   TableCellElement leftImageCell_;
   @UiField
   TableCellElement rightImageCell_;
   @UiField
   DivElement label_;
   @UiField
   DivElement infoLabel_;
   private Image leftImageWidget_;
   private Image rightImageWidget_;
}
