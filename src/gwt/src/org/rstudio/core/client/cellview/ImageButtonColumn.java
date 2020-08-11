/*
 * ImageButtonColumn.java
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
package org.rstudio.core.client.cellview;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;
import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;

public class ImageButtonColumn<T> extends Column<T, T>
{
   public interface TitleProvider<U>
   {
      String get(U object);
   }

   public interface RenderTemplates extends SafeHtmlTemplates
   {
      @Template("<span title=\"{1}\" style=\"cursor: pointer;\">{0}</span>")
      SafeHtml render(SafeHtml image, String title);
   }

   private static class ImageButtonCell<U> extends AbstractCell<U>
   {
      public ImageButtonCell(final ImageResource2x image,
                             final TitleProvider<U> titleProvider)
      {
         super(CLICK, KEYDOWN);

         image_ = image;
         titleProvider_ = titleProvider;
      }

      @Override
      public void render(Context context,
                         U value,
                         SafeHtmlBuilder sb)
      {
         if (value != null)
         {
            sb.append(TEMPLATES.render(image_.getSafeHtml(titleProvider_.get(value)), titleProvider_.get(value)));
         }
      }

      @Override
      public void onBrowserEvent(Context context,
                                 Element parent,
                                 U value,
                                 NativeEvent event,
                                 ValueUpdater<U> valueUpdater)
      {
         super.onBrowserEvent(context, parent, value, event, valueUpdater);

         if (CLICK.equals(event.getType()))
         {
            EventTarget eventTarget = event.getEventTarget();
            if (!Element.is(eventTarget))
               return;

            if (parent.getFirstChildElement().isOrHasChild(Element.as(eventTarget))) {
               // Ignore clicks that occur outside of the main element.
               onEnterKeyDown(context, parent, value, event, valueUpdater);
            }
         }
      }

      @Override
      protected void onEnterKeyDown(Context context,
                                    Element Parent,
                                    U value,
                                    NativeEvent event,
                                    ValueUpdater<U> valueUpdater)
      {
         if (valueUpdater != null)
            valueUpdater.update(value);
      }

      private final ImageResource2x image_;
      private final TitleProvider<U> titleProvider_;
   }

   public ImageButtonColumn(final ImageResource2x image,
                            final OperationWithInput<T> onClick,
                            final TitleProvider<T> titleProvider)
   {
      super(new ImageButtonCell<>(image, titleProvider));

      setFieldUpdater((index, object, value) -> {
         if (value != null)
            onClick.execute(object);
      });
   }

   public ImageButtonColumn(final ImageResource2x image,
                            final OperationWithInput<T> onClick,
                            final String title)
   {
      this(image, onClick, object -> title);
   }

   @Override
   public T getValue(T object)
   {
      if (showButton(object))
         return object;
      else
         return null;
   }

   protected boolean showButton(T object)
   {
      return true;
   }

   private static final RenderTemplates TEMPLATES = GWT.create(RenderTemplates.class);
}
