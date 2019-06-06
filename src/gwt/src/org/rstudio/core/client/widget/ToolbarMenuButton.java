/*
 * ToolbarMenuButton.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.aria.client.ExpandedValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import org.rstudio.core.client.command.ImageResourceProvider;
import org.rstudio.core.client.command.SimpleImageResourceProvider;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;

public class ToolbarMenuButton extends ToolbarButton
{
   public ToolbarMenuButton(String text, String title, ToolbarPopupMenu menu, boolean rightAlignMenu)
   {
      super(text,
            title,
            new ImageResource2x(ThemeResources.INSTANCE.menuDownArrow2x()),
            (ImageResource) null,
            (ClickHandler) null);
      
      leftImageWidget_.addStyleName("rstudio-themes-inverts");
      
      addMenuHandlers(menu, rightAlignMenu);
      
      addStyleName(styles_.toolbarButtonMenu());
      addStyleName(styles_.toolbarButtonMenuOnly());
   }

   public ToolbarMenuButton(String text,
                            String title,
                            ImageResource leftImage,
                            ToolbarPopupMenu menu)
   {
      this(text, title, leftImage, menu, false);
   }

   public ToolbarMenuButton(String text,
                            String title,
                            ImageResourceProvider leftImage,
                            ToolbarPopupMenu menu)
   {
      this(text, title, leftImage, menu, false);
   }

   public ToolbarMenuButton(String text,
                        String title,
                        ImageResource leftImage,
                        ToolbarPopupMenu menu,
                        boolean rightAlignMenu)
   {
      this(text,
           title,
           new SimpleImageResourceProvider(leftImage),
           menu, 
           rightAlignMenu);
   }

   private ToolbarMenuButton(String text,
                             String title,
                             ImageResourceProvider leftImage,
                             ToolbarPopupMenu menu,
                             boolean rightAlignMenu)
   {
      super(text, title, leftImage, new ImageResource2x(ThemeResources.INSTANCE.menuDownArrow2x()), null);
      
      rightImageWidget_.addStyleName("rstudio-themes-inverts");
      addMenuHandlers(menu, rightAlignMenu);
      addStyleName(styles_.toolbarButtonMenu());
   }

   private void addMenuHandlers(final ToolbarPopupMenu popupMenu,
                                final boolean rightAlignMenu)
   {
      Roles.getButtonRole().setAriaHaspopupProperty(getElement(), true);
      
      menu_ = popupMenu;
      rightAlignMenu_ = rightAlignMenu;

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

      addMouseDownHandler(event -> {
         event.preventDefault();
         event.stopPropagation();
         addStyleName(styles_.toolbarButtonPushed());
         // Some menus are rebuilt on every invocation. Ask the menu for 
         // the most up-to-date version before proceeding.
         popupMenu.getDynamicPopupMenu(menu -> {
            if (menuShowing[0])
            {
               removeStyleName(styles_.toolbarButtonPushed());
               menu.hide();
               Roles.getButtonRole().setAriaExpandedState(getElement(), ExpandedValue.FALSE);
            }
            else
            {
               if (rightAlignMenu_)
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
                           ToolbarMenuButton.this.getAbsoluteTop() +
                           ToolbarMenuButton.this.getOffsetHeight());
                     } 
                  });
               }
               else
               {
                  menu.showRelativeTo(ToolbarMenuButton.this);
               }
               menuShowing[0] = true;
               Roles.getButtonRole().setAriaExpandedState(getElement(), ExpandedValue.TRUE);
            }
         });
      });
      popupMenu.addCloseHandler(popupPanelCloseEvent -> {
         removeStyleName(styles_.toolbarButtonPushed());
         Scheduler.get().scheduleDeferred(() -> menuShowing[0] = false);
      });
   }

   public void setRightAlignMenu(boolean rightAlignMenu)
   {
      rightAlignMenu_ = rightAlignMenu;
   }

   public ToolbarPopupMenu getMenu()
   {
      return menu_;
   }

   private ToolbarPopupMenu menu_;
   private boolean rightAlignMenu_;
}
