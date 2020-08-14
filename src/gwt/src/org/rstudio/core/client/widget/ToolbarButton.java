/*
 * ToolbarButton.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.*;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ClassIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.ImageResourceProvider;
import org.rstudio.core.client.command.SimpleImageResourceProvider;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

public class ToolbarButton extends FocusWidget
{
   // button with no visible text
   public static String NoText = null;
   
   // button with no tooltip/accessibility text
   public static String NoTitle = null;
   
   private class SimpleHasHandlers extends HandlerManager implements HasHandlers
   {
      private SimpleHasHandlers()
      {
         super(null);
      }
   }
   
   public <T extends EventHandler> ToolbarButton(
                                      String text,
                                      String title,
                                      ImageResource leftImg,
                                      final HandlerManager eventBus,
                                      final GwtEvent<? extends T> targetEvent)
   {
      this(text, title, leftImg, event -> eventBus.fireEvent(targetEvent));
   }
   
   public ToolbarButton(String text,
                        String title,
                        ImageResourceProvider leftImageProvider,
                        ClickHandler clickHandler)
   {
      this(text, title, leftImageProvider, null, clickHandler);
   }
   
   public ToolbarButton(String text,
                        String title,
                        ImageResource leftImage)
   {
      this(text, title, new SimpleImageResourceProvider(leftImage), null);
   }
   
   public ToolbarButton(String text,
                        String title,
                        ImageResource leftImage,
                        ClickHandler clickHandler)
   {
      this(text, title, new SimpleImageResourceProvider(leftImage), clickHandler);
   }
   
   public ToolbarButton(String text,
                        String title,
                        ImageResource leftImage,
                        ImageResource rightImage,
                        ClickHandler clickHandler)
   {
      this(text,
           title,
           new SimpleImageResourceProvider(leftImage),
           rightImage,
           clickHandler);
   }
   
   public ToolbarButton(String text, // visible text
                        String title, // a11y / tooltip text
                        DecorativeImage leftImage,
                        ImageResource rightImage,
                        ClickHandler clickHandler)
   {
      super();

      setElement(binder.createAndBindUi(this));
      setClassId(null);

      this.setStylePrimaryName(styles_.toolbarButton());
      this.addStyleName(styles_.handCursor());

      setText(text);
      setTitle(title);
      setInfoText(null);
      leftImageWidget_ = leftImage == null ? new DecorativeImage() : leftImage;
      leftImageWidget_.setStylePrimaryName(styles_.toolbarButtonLeftImage());
      leftImageCell_.appendChild(leftImageWidget_.getElement());
      if (rightImage != null)
      {
         rightImageWidget_ = new DecorativeImage(rightImage);
         rightImageWidget_.setStylePrimaryName(styles_.toolbarButtonRightImage());
         rightImageCell_.appendChild(rightImageWidget_.getElement());
      }

      if (clickHandler != null)
         addClickHandler(clickHandler);

      Roles.getPresentationRole().set(wrapper_);
   }
   
   public ToolbarButton(String text,
                        String title,
                        ImageResourceProvider leftImage,
                        ImageResource rightImage,
                        ClickHandler clickHandler)
   {
      this(text,
           title,
           // extract the supplied left image 
           leftImage != null && leftImage.getImageResource() != null ?
               new DecorativeImage(leftImage.getImageResource()) :
               new DecorativeImage(), 
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

      final HandlerRegistration mouseDown = addMouseDownHandler(event ->
      {
         event.preventDefault();
         event.stopPropagation();

         addStyleName(styles_.toolbarButtonPushed());
         down_ = true;
      });

      final HandlerRegistration mouseOut = addMouseOutHandler(event ->
      {
         event.preventDefault();
         event.stopPropagation();

         removeStyleName(styles_.toolbarButtonPushed());
         down_ = false;
      });

      final HandlerRegistration mouseUp = addMouseUpHandler(event ->
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
      });

      final HandlerRegistration keyPress = addKeyPressHandler(event ->
      {
         char charCode = event.getCharCode();
         if (charCode == KeyCodes.KEY_ENTER || charCode == KeyCodes.KEY_SPACE)
         {
            event.preventDefault();
            event.stopPropagation();
            click();
         }
      });

      return () ->
      {
         mouseDown.removeHandler();
         mouseOut.removeHandler();
         mouseUp.removeHandler();
         keyPress.removeHandler();
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

   public void setLeftImage(ImageResource imageResource)
   {
      leftImageWidget_.setResource(imageResource);
   }

   public void setClassId(String name)
   {
      if (!StringUtil.isNullOrEmpty(displayClassId_))
         ClassIds.removeClassId(getElement(), displayClassId_);

      displayClassId_ = ClassIds.TOOLBAR_BTN + "_" + ClassIds.idSafeString(getTitle());
      if (!StringUtil.isNullOrEmpty(name))
         displayClassId_ += "_" + ClassIds.idSafeString(name);

      ClassIds.assignClassId(getElement(), displayClassId_);
   }

   public void setText(boolean visible, String text)
   {
      if (visible)
      {
         setText(text);
         Roles.getButtonRole().setAriaLabelProperty(getElement(), "");
      }
      else
      {
         setText("");
         Roles.getButtonRole().setAriaLabelProperty(getElement(), text);
      }
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
   
   public void setTitle(String title)
   {
      super.setTitle(title);
      if (!StringUtil.isNullOrEmpty(title))
         Roles.getButtonRole().setAriaLabelProperty(getElement(), title);
   }

   // Class name displayed by Help / Diagnostics / Show DOM Elements command. A default value is
   // set in the constructor, but this can be updated to be more specific.
   private String displayClassId_;

   private boolean down_;
   
   private final SimpleHasHandlers hasHandlers_ = new SimpleHasHandlers();
   
   interface Binder extends UiBinder<Element, ToolbarButton> { }

   private static final Binder binder = GWT.create(Binder.class);

   protected static final ThemeStyles styles_ = ThemeResources.INSTANCE.themeStyles();

   @UiField
   TableCellElement leftImageCell_;
   @UiField
   TableCellElement rightImageCell_;
   @UiField
   DivElement label_;
   @UiField
   DivElement infoLabel_;
   @UiField
   TableElement wrapper_;
   protected DecorativeImage leftImageWidget_;
   protected DecorativeImage rightImageWidget_;
}
