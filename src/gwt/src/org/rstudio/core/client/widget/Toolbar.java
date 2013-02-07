/*
 * Toolbar.java
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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import org.rstudio.core.client.SeparatorManager;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

import java.util.AbstractList;

public class Toolbar extends Composite
{
   private static class ChildWidgetList extends AbstractList<Widget>
   {
      private ChildWidgetList(ComplexPanel panel)
      {
         panel_ = panel;
      }

      @Override
      public Widget get(int index)
      {
         return panel_.getWidget(index);
      }

      @Override
      public int size()
      {
         return panel_.getWidgetCount();
      }

      private ComplexPanel panel_;
   }

   private class ToolbarSeparatorManager extends SeparatorManager<Widget>
   {
      @Override
      protected boolean isSeparator(Widget item)
      {
         return styles_.toolbarSeparator().equals(item.getStylePrimaryName());
      }

      @Override
      protected boolean isVisible(Widget item)
      {
         return item.isVisible();
      }

      @Override
      protected void setVisible(Widget item, boolean visible)
      {
         item.setVisible(visible);
      }
   }

   public Toolbar(Widget[] leftWidgets, Widget[] rightWidgets)
   {
      this();

      if (leftWidgets != null)
      {
         for (int i = 0; i < leftWidgets.length; i++)
         {
            if (i > 0)
               addLeftSeparator();
            addLeftWidget(leftWidgets[i]);
         }
      }

      if (rightWidgets != null)
      {
         for (int i = 0; i < rightWidgets.length; i++)
         {
            if (i > 0)
               addRightSeparator();
            addRightWidget(rightWidgets[i]);
         }
      }
   }

   public Toolbar()
   {
      super();

      horizontalPanel_ = new HorizontalPanel();
      horizontalPanel_.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      leftToolbarPanel_ = new HorizontalPanel();
      leftToolbarPanel_.setVerticalAlignment(
                                          HasVerticalAlignment.ALIGN_MIDDLE);
      horizontalPanel_.add(leftToolbarPanel_);
      horizontalPanel_.setCellHorizontalAlignment(
                                          leftToolbarPanel_,
                                          HasHorizontalAlignment.ALIGN_LEFT);

      rightToolbarPanel_ = new HorizontalPanel();
      rightToolbarPanel_.setVerticalAlignment(
                                          HasVerticalAlignment.ALIGN_MIDDLE);
      horizontalPanel_.add(rightToolbarPanel_);
      horizontalPanel_.setCellHorizontalAlignment(
                                          rightToolbarPanel_,
                                          HasHorizontalAlignment.ALIGN_RIGHT);

      initWidget(horizontalPanel_);
      setStyleName(styles_.toolbar());
   }

   protected void manageSeparators()
   {
      separatorsInvalidated_ = false;
      new ToolbarSeparatorManager().manageSeparators(
            new ChildWidgetList(leftToolbarPanel_));
      new ToolbarSeparatorManager().manageSeparators(
            new ChildWidgetList(rightToolbarPanel_));
   }

   public void invalidateSeparators()
   {
      if (!separatorsInvalidated_)
      {
         separatorsInvalidated_ = true;
         Scheduler.get().scheduleFinally(new ScheduledCommand()
         {
            public void execute()
            {
               manageSeparators();
            }
         });
      }
   }

   public <TWidget extends Widget> TWidget addLeftWidget(TWidget widget)
   {
      leftToolbarPanel_.add(widget);
      invalidateSeparators();
      return widget;
   }
   
   public <TWidget extends Widget> TWidget addLeftWidget(
         TWidget widget,
         VerticalAlignmentConstant alignment)
   {
      addLeftWidget(widget);
      leftToolbarPanel_.setCellVerticalAlignment(widget, alignment);
      invalidateSeparators();
      return widget;
   }
   
   public interface MenuSource
   {
      ToolbarPopupMenu getMenu();
   }
   
   public void addLeftPopupMenu(Label label, final ToolbarPopupMenu menu)
   {
      addLeftPopupMenu(label, new MenuSource() {

         @Override
         public ToolbarPopupMenu getMenu()
         {
            return menu;
         }
      });
   }
   
   public void addLeftPopupMenu(final Label label, 
                                final MenuSource menuSource)
   {
      label.setStylePrimaryName("rstudio-StrongLabel") ;
      label.setWordWrap(false);
      label.getElement().getStyle().setCursor(Style.Cursor.DEFAULT);
      label.getElement().getStyle().setOverflow(Overflow.HIDDEN);
      addLeftWidget(label) ;
      Image image = new Image(ThemeResources.INSTANCE.menuDownArrow());
      image.getElement().getStyle().setMarginLeft(5, Unit.PX);
      image.getElement().getStyle().setMarginRight(8, Unit.PX);
      image.getElement().getStyle().setMarginBottom(2, Unit.PX);
      addLeftWidget(image);

      final ClickHandler clickHandler = new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            ToolbarPopupMenu menu = menuSource.getMenu();
            menu.showRelativeTo(label);
            menu.getElement().getStyle().setPaddingTop(3, Style.Unit.PX);
         }
      };
      label.addClickHandler(clickHandler);
      image.addClickHandler(clickHandler);
   }

   public Widget addLeftSeparator()
   {
      Image sep = new Image(ThemeResources.INSTANCE.toolbarSeparator());
      sep.setStylePrimaryName(styles_.toolbarSeparator());
      leftToolbarPanel_.add(sep);
      invalidateSeparators();
      return sep;
   }
   
   public Widget addRightSeparator()
   {
      Image sep = new Image(ThemeResources.INSTANCE.toolbarSeparator());
      sep.setStylePrimaryName(styles_.toolbarSeparator());
      rightToolbarPanel_.add(sep);
      invalidateSeparators();
      return sep;
   }
   
   public <TWidget extends Widget> TWidget addRightWidget(TWidget widget)
   {
      rightToolbarPanel_.add(widget);
      invalidateSeparators();
      return widget;
   }
   
   public void removeLeftWidget(Widget widget)
   {
      leftToolbarPanel_.remove(widget);
   }

   public void removeLeftWidgets()
   {
      removeAllWidgets(leftToolbarPanel_);
   }
   
   public void removeRightWidget(Widget widget)
   {
      rightToolbarPanel_.remove(widget);
   }
   
   public void removeRightWidgets()
   {
      removeAllWidgets(rightToolbarPanel_);
   }
   
   public void removeAllWidgets()
   {
      removeLeftWidgets();
      removeRightWidgets();
   }
   

   public int getHeight()
   {
      int offsetHeight = getOffsetHeight();
      if (offsetHeight != 0)
         return offsetHeight;
      else
         return DEFAULT_HEIGHT;
   }
   
   private void removeAllWidgets(HorizontalPanel panel)
   {
      for (int i = panel.getWidgetCount()-1; i >= 0; i--)
         panel.remove(i);
   }

   private HorizontalPanel horizontalPanel_ ;
   private HorizontalPanel leftToolbarPanel_ ;
   private HorizontalPanel rightToolbarPanel_ ;
   protected final ThemeStyles styles_ = ThemeResources.INSTANCE.themeStyles();
   private boolean separatorsInvalidated_ = false;

   public static final int DEFAULT_HEIGHT = 22;
}
