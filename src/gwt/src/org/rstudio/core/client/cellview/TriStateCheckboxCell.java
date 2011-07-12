/*
 * TriStateCheckboxCell.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import java.util.HashSet;
import java.util.Set;

public class TriStateCheckboxCell implements Cell<Boolean>
{
   interface Resources extends ClientBundle
   {
      ImageResource checkboxIndeterminate();
      ImageResource checkboxOn();
      ImageResource checkboxOff();
   }

   public TriStateCheckboxCell()
   {
      consumedEvents_ = new HashSet<String>();
      consumedEvents_.add("click");
      consumedEvents_.add("keydown");
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
   public boolean isEditing(Context context, Element parent, Boolean value)
   {
      return false;
   }

   @Override
   public void onBrowserEvent(Context context,
                              Element parent,
                              Boolean value,
                              NativeEvent event,
                              ValueUpdater<Boolean> booleanValueUpdater)
   {
      if ("click".equals(event.getType()))
      {
         booleanValueUpdater.update(value == null ? true : !value);
      }
   }

   @Override
   public void render(Context context, Boolean value, SafeHtmlBuilder sb)
   {
      ImageResource img;
      if (value == null)
         img = RES.checkboxIndeterminate();
      else if (value)
         img = RES.checkboxOn();
      else
         img = RES.checkboxOff();

      sb.append(SafeHtmlUtils.fromTrustedString(
            AbstractImagePrototype.create(img).getHTML()));
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
   private static final Resources RES = GWT.create(Resources.class);
}
