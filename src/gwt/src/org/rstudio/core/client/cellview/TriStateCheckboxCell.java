/*
 * TriStateCheckboxCell.java
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
package org.rstudio.core.client.cellview;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.view.client.SelectionModel;

import java.util.HashSet;
import java.util.Set;

import org.rstudio.core.client.resources.ImageResource2x;

/**
 * WARNING: If you use this, take a look at ChangelistTable.NotEditingTextCell,
 * which was necessary to get the table out of a state where cellIsEditing is
 * stuck on true due to this cell getting mouseover but not mouseout.
 */
public class TriStateCheckboxCell<TKey> implements Cell<Boolean>
{
   interface Resources extends ClientBundle
   {
      @Source("checkboxIndeterminate_2x.png")
      ImageResource checkboxIndeterminate2x();

      @Source("checkboxOn_2x.png")
      ImageResource checkboxOn2x();

      @Source("checkboxOff_2x.png")
      ImageResource checkboxOff2x();
   }

   public TriStateCheckboxCell(SelectionModel<TKey> selectionModel)
   {
      selectionModel_ = selectionModel;
      consumedEvents_ = new HashSet<String>();
      consumedEvents_.add("click");
      consumedEvents_.add("keydown");
      consumedEvents_.add("mouseover");
      consumedEvents_.add("mouseout");
   }

   @Override
   public boolean dependsOnSelection()
   {
      return false;
   }

   @Override
   public Set<String> getConsumedEvents()
   {
      return consumedEvents_;
   }

   @Override
   public boolean handlesSelection()
   {
      return false;
   }

   @Override
   @SuppressWarnings("unchecked")
   public boolean isEditing(Context context, Element parent, Boolean value)
   {
      // We aren't actually editing here, of course. All we're trying to do
      // is prevent selection from changing, if the user is clicking on the
      // checkbox of a cell that's in a selected row.
      return mouseInCheckbox_ &&
             selectionModel_.isSelected((TKey) context.getKey());
   }

   @Override
   public void onBrowserEvent(Context context,
                              Element parent,
                              Boolean value,
                              NativeEvent event,
                              ValueUpdater<Boolean> booleanValueUpdater)
   {
      if (Element.is(event.getEventTarget()) &&
          Element.as(event.getEventTarget()).getTagName().equalsIgnoreCase("img"))
      {
         if ("click".equals(event.getType()))
         {
            booleanValueUpdater.update(value == null ? true : !value);
         }
         else if ("mouseover".equals(event.getType()))
         {
            mouseInCheckbox_ = true;
         }
         else if ("mouseout".equals(event.getType()))
         {
            // WARNING!!!! Sometimes we get mouseover without a corresponding
            // mouseout!! See comment at top of this class!
            mouseInCheckbox_ = false;
         }
      }
   }

   @Override
   public void render(Context context, Boolean value, SafeHtmlBuilder sb)
   {
      ImageResource2x img;
      if (value == null)
         img = new ImageResource2x(RES.checkboxIndeterminate2x());
      else if (value)
         img = new ImageResource2x(RES.checkboxOn2x());
      else
         img = new ImageResource2x(RES.checkboxOff2x());

      sb.append(img.getSafeHtml());
   }

   @Override
   public boolean resetFocus(Context context, Element parent, Boolean value)
   {
      return false;
   }

   @Override
   public void setValue(Context context, Element parent, Boolean value)
   {
      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      render(context, value, builder);
      parent.setInnerHTML(builder.toSafeHtml().asString());
   }

   private final HashSet<String> consumedEvents_;
   private boolean mouseInCheckbox_;
   private final SelectionModel<TKey> selectionModel_;
   private static final Resources RES = GWT.create(Resources.class);
}
