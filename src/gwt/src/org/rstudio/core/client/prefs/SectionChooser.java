/*
 * SectionChooser.java
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
package org.rstudio.core.client.prefs;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.aria.client.SelectedValue;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import org.rstudio.core.client.ElementIds;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.widget.DecorativeImage;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Vertical tab control used by Preferences dialogs. Follows the ARIA tab pattern.
 * 
 * Each tab:
 *    role="tab"
 *    aria-controls=id_of_panel
 *    aria-selected=true|false
 *    tabindex=-1 (0 for the currently selected tab only)
 * 
 * Each panel controlled by a tab:
 *    role="tabpanel"
 *    aria-labelled-by=id_of_tab
 * 
 * Navigation between tabs is via up/down arrow keys, with automatic activation of the
 * tab when it gets focus.
 */
class SectionChooser extends SimplePanel implements
                                                HasSelectionHandlers<Integer>
{
   private static class ClickableVerticalPanel extends VerticalPanel
   {
      HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }

      HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
      {
         return addDomHandler(handler, KeyDownEvent.getType());
      }
   }

   public SectionChooser(String tabListLabel)
   {
      setStyleName(res_.styles().sectionChooser());
      inner_.setStyleName(res_.styles().sectionChooserInner());
      setWidget(inner_);
      Roles.getTablistRole().set(getElement());
      A11y.setARIATablistOrientation(getElement(), true /*vertical*/);
      Roles.getTablistRole().setAriaLabelProperty(getElement(), tabListLabel);
   }

   /**
    * Add a section (tab) to the chooser.
    * @param icon
    * @param name
    * @return Element ID of the section tab
    */
   public Id addSection(ImageResource icon, String name)
   {
      DecorativeImage img = new DecorativeImage(icon.getSafeUri());
      nudgeDown(img);
      img.setSize("29px", "20px");
      Label label = new Label(name, false);
      final ClickableVerticalPanel panel = new ClickableVerticalPanel();
      panel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
      final HorizontalPanel innerPanel = new HorizontalPanel();
      innerPanel.setWidth("0px");
      innerPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
      innerPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
      innerPanel.add(img);
      innerPanel.add(nudgeRightPlus(label));
      panel.add(innerPanel);
      panel.setStyleName(res_.styles().section());
      Id sectionTabId = Id.of(ElementIds.idFromLabel(name) + "_options");
      panel.getElement().setId(sectionTabId.getAriaValue());

      panel.addClickHandler(event -> select(inner_.getWidgetIndex(panel)));
      panel.addKeyDownHandler(event ->
      {
         switch(event.getNativeKeyCode())
         {
            case KeyCodes.KEY_UP:
               selectPreviousSection();
               break;
            case KeyCodes.KEY_DOWN:
               selectNextSection();
               break;
            case KeyCodes.KEY_HOME:
               selectFirstSection();
               break;
            case KeyCodes.KEY_END:
               selectLastSection();
               break;
         }
      });

      Roles.getTabRole().set(panel.getElement());
      panel.getElement().setTabIndex(-1);
      Roles.getTabRole().setAriaSelectedState(panel.getElement(), SelectedValue.FALSE);
      Roles.getTabRole().setAriaControlsProperty(panel.getElement(), getTabPanelId(sectionTabId));
      inner_.add(panel);

      // FireFox fails to enumerate the tabs when building its accessibility tree, 
      // perhaps due to the deep nesting of layout tables. Use the aria-owns attribute 
      // to assist it. https://github.com/rstudio/rstudio/issues/5120
      tabIds_.add(sectionTabId);
      Roles.getTablistRole().setAriaOwnsProperty(getElement(), tabIds_.toArray(new Id[0]));

      return sectionTabId;
   }

   public void select(Integer index)
   {
      if (selectedIndex_ != null)
      {
         Widget prevItem = inner_.getWidget(selectedIndex_);
         prevItem.removeStyleName(res_.styles().activeSection());
         prevItem.getElement().setTabIndex(-1);
         Roles.getTabRole().setAriaSelectedState(prevItem.getElement(), SelectedValue.FALSE);
      }

      selectedIndex_ = index;

      if (index != null)
      {
         Widget newItem = inner_.getWidget(index);
         newItem.addStyleName(res_.styles().activeSection());
         newItem.getElement().setTabIndex(0);
         Roles.getTabRole().setAriaSelectedState(newItem.getElement(), SelectedValue.TRUE);
      }

      SelectionEvent.fire(this, index);
   }

   private void focusCurrent()
   {
      if (selectedIndex_ == null)
         return;

      Widget currentItem = inner_.getWidget(selectedIndex_);
      currentItem.getElement().focus();
   }

   public void hideSection(Integer index)
   {
      if (index != null)
         inner_.getWidget(index).setVisible(false);
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }

   private void selectNextSection()
   {
      if (selectedIndex_ == null)
         return;
      for (int i = selectedIndex_ + 1; i < sectionCount(); i++)
      {
         if (inner_.getWidget(i).isVisible())
         {
            select(i);
            focusCurrent();
            return;
         }
      }
      selectFirstSection();
   }

   private void selectPreviousSection()
   {
      if (selectedIndex_ == null)
         return;
      for (int i = selectedIndex_ - 1; i >= 0; i--)
      {
         if (inner_.getWidget(i).isVisible())
         {
            select(i);
            focusCurrent();
            return;
         }
      }
      selectLastSection();
   }

   private void selectFirstSection()
   {
      for (int i = 0; i < sectionCount(); i++)
      {
         if (inner_.getWidget(i).isVisible())
         {
            select(i);
            focusCurrent();
            return;
         }
      }
   }

   private void selectLastSection()
   {
      for (int i = sectionCount() - 1; i >= 0; i--)
      {
         if (inner_.getWidget(i).isVisible())
         {
            select(i);
            focusCurrent();
            return;
         }
      }
   }

   public int getDesiredWidth()
   {
      return 122;
   }

   public void focus()
   {
      if (selectedIndex_ != null)
      {
         Widget currentItem = inner_.getWidget(selectedIndex_);
         currentItem.getElement().focus();
      }
   }

   /**
    * @param tabId element id for a tab
    * @return element id to use for associated tabpanel
    */
   public static Id getTabPanelId(Id tabId)
   {
      return Id.of(tabId.getAriaValue() + "_panel");
   }

   private Widget nudgeRightPlus(Widget widget)
   {
      widget.addStyleName(res_.styles().nudgeRightPlus());
      return widget;
   }

   private Widget nudgeDown(Widget widget)
   {
      widget.addStyleName(res_.styles().nudgeDown());
      return widget;
   }

   private int sectionCount()
   {
      return inner_.getWidgetCount();
   }

   private Integer selectedIndex_;
   private final VerticalPanel inner_ = new VerticalPanel();
   private static final PreferencesDialogBaseResources res_ = PreferencesDialogBaseResources.INSTANCE;
   private final Set<Id> tabIds_ = new LinkedHashSet<>();
}
