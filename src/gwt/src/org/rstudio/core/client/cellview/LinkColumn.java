/*
 * LinkColumn.java
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

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.ListDataProvider;


// package name column which includes a hyperlink to package docs
public abstract class LinkColumn<T> extends Column<T, String>
{
   public LinkColumn(ListDataProvider<T> dataProvider,
                     OperationWithInput<T> onClicked)
   {
      this(dataProvider, onClicked, false);
   }

   public LinkColumn(final ListDataProvider<T> dataProvider,
                     final OperationWithInput<T> onClicked,
                     final boolean alwaysUnderline)
   {
      super(new ClickableTextCell(){

         // render anchor using custom styles. detect selection and
         // add selected style to invert text color
         @Override
         protected void render(Context context,
                               SafeHtml value,
                               SafeHtmlBuilder sb)
         {
            if (value != null)
            {
              Styles styles = RESOURCES.styles();
              StringBuilder div = new StringBuilder();
              div.append("<div class=\"");
              div.append(styles.link() + " ");
              div.append(ThemeResources.INSTANCE.themeStyles().handCursor());
              if (alwaysUnderline)
                 div.append(" " + styles.linkUnderlined());
              div.append("\" title=\"" + value.asString() + "\">");

              sb.appendHtmlConstant(div.toString());
              sb.append(value);
              sb.appendHtmlConstant("</div>");
            }
          }

         // click event which occurs on the actual package link div
         // results in showing help for that package
         @Override
         public void onBrowserEvent(Context context, Element parent,
                                    String value, NativeEvent event,
                                    ValueUpdater<String> valueUpdater)
         {
           super.onBrowserEvent(context, parent, value, event, valueUpdater);
           if ("click".equals(event.getType()))
           {
              // verify that the click was on the package link
              JavaScriptObject evTarget = event.getEventTarget().cast();
              if (Element.is(evTarget) &&
                  Element.as(evTarget).getClassName().startsWith(
                                     RESOURCES.styles().link()))
              {
                 int idx = context.getIndex();
                 if (idx >= 0 && idx < dataProvider.getList().size())
                    onClicked.execute(dataProvider.getList().get(idx));
              }
           }
         }
      });
   }

   static interface Styles extends CssResource
   {
      String link();
      String linkUnderlined();
   }

   interface Resources extends ClientBundle
   {
      @Source("LinkColumn.css")
      Styles styles();
   }

   static Resources RESOURCES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
}
