/*
 * CheckBoxList.java
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

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.dom.DomUtils;

/**
 * A set of checkboxes in a scrolling region. Each checkbox has its own tab index, so this
 * isn't a list (accessibility-wise), just a bunch of checkboxes in a group.
 */
public class CheckBoxList extends Composite
{
   public CheckBoxList(Element labelElement)
   {
      resources_ = GWT.create(CheckBoxList.Resources.class);
      style_ = resources_.listStyle();
      style_.ensureInjected();

      ScrollPanel scrollPanel = new ScrollPanel();
      scrollPanel.addStyleName(style_.checkBoxList());
      DomUtils.ensureHasId(labelElement);
      panel_ = new VerticalPanel();
      Roles.getGroupRole().set(panel_.getElement());
      Roles.getGroupRole().setAriaLabelledbyProperty(panel_.getElement(), Id.of(labelElement));
      scrollPanel.setWidget(panel_);
      initWidget(scrollPanel);
   }

   public CheckBoxList(Widget labelWidget)
   {
      this(labelWidget.getElement());
   }

   public void addItem(CheckBox checkBox)
   {
      panel_.add(checkBox);
   }

   public int getItemCount()
   {
      return panel_.getWidgetCount();
   }

   public CheckBox getItemAtIdx(int i)
   {
      if (i >= 0 && i < getItemCount())
         return (CheckBox)panel_.getWidget(i);
      return null;
   }
   
   public boolean contains(String value)
   {
      int count = getItemCount();
      for (int i = 0; i<count; i++)
      {
         if (getItemAtIdx(i).getText().equals(value))
            return true;
      }
      return false;
   }

   public void clearItems()
   {
      panel_.clear();
   }

   public interface ListStyle extends CssResource
   {
      String checkBoxList();
   }

   public interface Resources extends ClientBundle
   {
      @Source("CheckBoxList.css")
      ListStyle listStyle();
   }

   private final VerticalPanel panel_;

   private Resources resources_;
   private ListStyle style_;
}
