/*
 * SlideNavigationToolbarMenu.java
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

package org.rstudio.studio.client.common.presentation;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

public class SlideNavigationToolbarMenu 
                     implements SlideNavigationMenu
{
   public SlideNavigationToolbarMenu(Toolbar toolbar)
   {
      this(toolbar, 200, 300, false);
   }
    
   public SlideNavigationToolbarMenu(Toolbar toolbar,
                                     int maxWidth,
                                     int heightOffset,
                                     boolean separatorAfter)
   { 

      Commands commands = RStudioGinjector.INSTANCE.getCommands();
      AppCommand presHome = commands.presentationHome();
      homeButton_ = new ToolbarButton(
            ToolbarButton.NoText,
            presHome.getTooltip(),
            presHome.getImageResource());
      toolbar.addLeftWidget(homeButton_);
      homeSeparatorWidget_ = toolbar.addLeftSeparator();
      
      titleLabel_.addStyleName(ThemeResources.INSTANCE.themeStyles()
                                          .presentationNavigatorLabel());
      titleLabel_.getElement().getStyle().setProperty("maxWidth", 
                                                      maxWidth + "px");
      
      menuWidget_ = toolbar.addLeftPopupMenu(titleLabel_, slidesMenu_);
      heightOffset_ = heightOffset;
     
      AppCommand presEdit = commands.presentationEdit();
      editSeparatorWidget_ = toolbar.addLeftSeparator();
      editButton_ = new ToolbarButton(ToolbarButton.NoText, presEdit.getTooltip(), presEdit.getImageResource());
      toolbar.addLeftWidget(editButton_); 
      
      if (separatorAfter)
         separatorWidget_ = toolbar.addLeftSeparator();
      
      setDropDownVisible(false);
   }
   
   @Override
   public boolean isVisible()
   {
      return titleLabel_.isVisible();
   }
   
   
   @Override
   public void setVisible(boolean visible)
   {
      homeButton_.setVisible(visible);
      homeSeparatorWidget_.setVisible(visible);
      titleLabel_.setVisible(visible);
      setDropDownVisible(visible);
      setEditButtonVisible(visible);
      if (separatorWidget_ != null)
         separatorWidget_.setVisible(visible);
   }

   @Override
   public void setCaption(String caption)
   {
      titleLabel_.setText(caption);
   }

   @Override
   public void addItem(MenuItem menu)
   {
      slidesMenu_.addItem(menu);  
   }

   @Override
   public void clear()
   {
      slidesMenu_.clearItems();
   }

   @Override
   public void setDropDownVisible(boolean visible)
   {
      menuWidget_.setVisible(isVisible() && visible);
   }
   
   @Override
   public void setEditButtonVisible(boolean visible)
   {
      visible = isVisible() && visible;
      editButton_.setVisible(visible);
      editSeparatorWidget_.setVisible(visible);
   }
   
   @Override
   public HasClickHandlers getHomeButton()
   {
      return homeButton_;
   }

   @Override
   public HasClickHandlers getEditButton()
   {
      return editButton_;
   }
   
   private class SlidesPopupMenu extends ScrollableToolbarPopupMenu
   {
      public SlidesPopupMenu()
      {
         addStyleName(ThemeStyles.INSTANCE.statusBarMenu());
      }
      
      @Override
      protected int getMaxHeight()
      {
         if (BrowseCap.isInternetExplorer())
         {
            return 300;
         }
         else
         {
            return Window.getClientHeight() - titleLabel_.getAbsoluteTop() -
                  titleLabel_.getOffsetHeight() - heightOffset_;
         }
      }
   }
   
   private ToolbarButton homeButton_;
   private Widget homeSeparatorWidget_;
   private ToolbarButton editButton_;
   private Widget editSeparatorWidget_;
   private Label titleLabel_ = new Label();
   private SlidesPopupMenu slidesMenu_ = new SlidesPopupMenu();
   private Widget menuWidget_;
   private Widget separatorWidget_ = null;
   private final int heightOffset_;
}
