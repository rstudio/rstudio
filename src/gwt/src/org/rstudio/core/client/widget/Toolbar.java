/*
 * Toolbar.java
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

import com.google.gwt.aria.client.ExpandedValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import org.rstudio.core.client.SeparatorManager;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.resources.ImageResource2x;
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
         return styles_.toolbarSeparator() == item.getStylePrimaryName();
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

   /**
    * This is only used in a couple places, and should someday be replaced with standard
    * ToolbarMenuButton; for now didn't want to have to rework the code using it to
    * conform to the different model so duplicated some logic from ToolbarMenuButton to support
    * screen readers and keyboard-use.
    */
   private class ToolbarPopupButton extends FocusWidget
   {
      public ToolbarPopupButton(final MenuLabel menuLabel, MenuSource menuSource)
      {
         menuSource_ = menuSource;

         setElement(Document.get().createPushButtonElement());
         getElement().setClassName(styles_.toolbarButton());
         getElement().addClassName(styles_.toolbarButtonMenu());
         getElement().addClassName(styles_.popupButton());

         HorizontalPanel container = new HorizontalPanel();
         container.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
         getElement().appendChild(container.getElement());

         label_ = menuLabel.asWidget();
         label_.setStylePrimaryName("rstudio-StrongLabel");
         label_.getElement().getStyle().setOverflow(Overflow.HIDDEN);
         container.add(label_);

         Image image = new DecorativeImage(new ImageResource2x(ThemeResources.INSTANCE.menuDownArrow2x()));
         image.getElement().getStyle().setMarginLeft(5, Unit.PX);
         image.getElement().getStyle().setMarginRight(8, Unit.PX);
         image.getElement().getStyle().setMarginBottom(2, Unit.PX);
         image.addStyleName("rstudio-themes-inverts");
         image.addStyleName(styles_.toolbarButtonRightImage());
         container.add(image);

         addMenuHandlers();
      }

      private void addMenuHandlers()
      {
         Roles.getButtonRole().setAriaHaspopupProperty(getElement(), true);
         setMenuShowing(false);

         addMouseDownHandler(event ->
         {
            event.preventDefault();
            event.stopPropagation();
            menuClick();
         });
         menuSource_.getMenu().addCloseHandler(popupPanelCloseEvent ->
         {
            removeStyleName(styles_.toolbarButtonPushed());
            Scheduler.get().scheduleDeferred(() ->
            {
               setMenuShowing(false);
               setFocus(true);
            });
         });
         addKeyPressHandler(event ->
         {
            char charCode = event.getCharCode();
            if (charCode == KeyCodes.KEY_ENTER || charCode == KeyCodes.KEY_SPACE)
            {
               event.preventDefault();
               event.stopPropagation();
               menuClick();
            }
         });
      }

      private void menuClick()
      {
         addStyleName(styles_.toolbarButtonPushed());
         if (menuShowing_)
         {
            removeStyleName(styles_.toolbarButtonPushed());
            menuSource_.getMenu().hide();
            setMenuShowing(false);
            setFocus(true);
         }
         else
         {
            menuSource_.getMenu().showRelativeTo(label_);
            menuSource_.getMenu().getElement().getStyle().setPaddingTop(3, Unit.PX);
            setMenuShowing(true);
            menuSource_.getMenu().focus();
         }
      }

      private void setMenuShowing(boolean showing)
      {
         if (showing)
            Roles.getMenuRole().setAriaExpandedState(getElement(), ExpandedValue.TRUE);
         else
            A11y.setARIANotExpanded(getElement());

         menuShowing_ = showing;
      }

      private boolean menuShowing_;
      private final MenuSource menuSource_;
      private final Widget label_;
   }

   public Toolbar(Widget[] leftWidgets, Widget[] rightWidgets, String label)
   {
      this(label);

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

   public Toolbar(String label)
   {
      super();

      toolbarWrapper_ = new HTMLPanel("");

      Roles.getToolbarRole().set(toolbarWrapper_.getElement());
      Roles.getToolbarRole().setAriaLabelProperty(toolbarWrapper_.getElement(), label);

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

      horizontalPanel_.addStyleName(styles_.toolbar());

      toolbarWrapper_.add(horizontalPanel_);
      initWidget(toolbarWrapper_);

      setStyleName(styles_.rstheme_toolbarWrapper());
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
   
   public <TWidget extends Widget> TWidget insertWidget(TWidget widget, TWidget beforeWidget)
   {
      int beforeIndex = leftToolbarPanel_.getWidgetIndex(beforeWidget);
      leftToolbarPanel_.insert(widget, beforeIndex);
      invalidateSeparators();
      return widget;
   }
   
   /**
    * Manually size the given widget in the left toolbar. Gives the cell contain
    * the widget the given size, then tells the widget to fill its cell.
    * 
    * @param w The widget to size
    * @param width The widget's desired width
    */
   public void setLeftWidgetWidth(Widget w, String width)
   {
      leftToolbarPanel_.setCellWidth(w, width);
      leftToolbarPanel_.setWidth("100%");
   }
   
   /**
    * Clear the size of a manually sized widget in the left toolbar (undoes the
    * effect of a previous call to setLeftWidgetWidth)
    * 
    * @param w The widget to size
    */
   public void clearLeftWidgetWidth(Widget w)
   {
      leftToolbarPanel_.setCellWidth(w, "");
      leftToolbarPanel_.setWidth("");
   }
   
   public interface MenuSource
   {
      ToolbarPopupMenu getMenu();
   }
   
   public Widget addLeftPopupMenu(Label label, final ToolbarPopupMenu menu)
   {
      return addToolbarPopupMenu(new SimpleMenuLabel(label), menu, true);
   }
   
   public Widget addLeftPopupMenu(MenuLabel label, final ToolbarPopupMenu menu)
   {
      return addToolbarPopupMenu(label, menu, true);
   }

   public Widget addRightPopupMenu(MenuLabel label, final ToolbarPopupMenu menu)
   {
      return addToolbarPopupMenu(label, menu, false);
   }
   
   public static Widget getSeparator()
   {
      Image sep = new Image(ThemeResources.INSTANCE.toolbarSeparator());
      return sep;
   }

   public Widget addLeftSeparator()
   {
      Image sep = new ToolbarSeparator();
      leftToolbarPanel_.add(sep);
      invalidateSeparators();
      return sep;
   }
   
   public Widget addRightSeparator()
   {
      Image sep = new ToolbarSeparator();
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

      // clear any manually specified width on the left toolbar
      leftToolbarPanel_.setWidth("");

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

   @Override
   public void addStyleName(String styleName)
   {
      horizontalPanel_.addStyleName(styleName);
   }

   public Widget getWrapper()
   {
      return toolbarWrapper_;
   }

   /**
    * Change the accessibility label for the toolbar
    * @param label
    */
   public void setLabel(String label)
   {
      Roles.getToolbarRole().setAriaLabelProperty(toolbarWrapper_.getElement(), label);
   }

   private void removeAllWidgets(HorizontalPanel panel)
   {
      for (int i = panel.getWidgetCount()-1; i >= 0; i--)
         panel.remove(i);
   }

   private Widget addToolbarPopupMenu(
         MenuLabel label, 
         final ToolbarPopupMenu menu,
         boolean left)
   {
      return addPopupMenu(label, new MenuSource() {
         @Override
         public ToolbarPopupMenu getMenu()
         {
            return menu;
         }
      }, left);
   }

   private Widget addPopupMenu(final MenuLabel menuLabel, 
         final MenuSource menuSource,
         boolean left)
   {
      ToolbarPopupButton button = new ToolbarPopupButton(menuLabel, menuSource);

      if (left)
         addLeftWidget(button);
      else
         addRightWidget(button);

      return button;
   }

   private HorizontalPanel horizontalPanel_;
   private HorizontalPanel leftToolbarPanel_;
   private HorizontalPanel rightToolbarPanel_;
   private HTMLPanel toolbarWrapper_;
   protected final ThemeStyles styles_ = ThemeResources.INSTANCE.themeStyles();
   private boolean separatorsInvalidated_ = false;

   public static final int DEFAULT_HEIGHT = 22;
}
