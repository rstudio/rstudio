/*
 * PackageLinkColumn.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.List;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.ListDataProvider;


// package name column which includes a hyperlink to package docs
public abstract class PackageLinkColumn extends Column<PackageInfo, String>
{
   public PackageLinkColumn(ListDataProvider<PackageInfo> dataProvider,
                            OperationWithInput<PackageInfo> onClicked)
   {
      this(dataProvider, onClicked, false);
   }

   public PackageLinkColumn(final ListDataProvider<PackageInfo> dataProvider,
                            final OperationWithInput<PackageInfo> onClicked,
                            final boolean alwaysUnderline)
   {
      super(new ClickableTextCell()
      {
         // render anchor using custom styles. detect selection and
         // add selected style to invert text color
         @Override
         protected void render(Context context,
                               SafeHtml value,
                               SafeHtmlBuilder sb)
         {
            if (value != null)
            {
               SafeUri uri;
               if (context.getIndex() == 3)
               {
                  uri = RESOURCES.iconError().getSafeUri();
               }
               else if (context.getIndex() == 7)
               {
                  uri = RESOURCES.iconWarning().getSafeUri();
               }
               else
               {
                  uri = RESOURCES.iconOk().getSafeUri();
               }

               sb.append(ICON_TEMPLATE.render(RESOURCES.styles().icon(), uri));

               String classNames = alwaysUnderline
                  ? RESOURCES.styles().link() + " " + RESOURCES.styles().linkUnderlined()
                  : RESOURCES.styles().link();

               sb.append(NAME_TEMPLATE.render(classNames, value.asString()));
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
               JavaScriptObject target = event.getEventTarget().cast();
               if (!Element.is(target))
                  return;
               
               Element targetEl = Element.as(target);
               if (!targetEl.hasClassName(RESOURCES.styles().link()))
                  return;
               
               int idx = context.getIndex();
               List<PackageInfo> data = dataProvider.getList();
               if (idx >= 0 && idx < dataProvider.getList().size())
               {
                  onClicked.execute(data.get(idx));
               }
            }
         }
      });
   }

   interface NameTemplate extends SafeHtmlTemplates
   {
      @Template("<span class=\"{0}\" title=\"{1}\">{1}</span>")
      SafeHtml render(String className, String title);
   }

   interface IconTemplate extends SafeHtmlTemplates
   {
      @Template("<img class=\"{0}\" src=\"{1}\"></img>")
      SafeHtml render(String className, SafeUri imgUri);
   }

   interface Styles extends CssResource
   {
      String icon();
      String link();
      String linkUnderlined();
   }

   interface Resources extends ClientBundle
   {
      @Source("iconOk.png")
      ImageResource iconOk();

      @Source("iconWarning.png")
      ImageResource iconWarning();

      @Source("iconError.png")
      ImageResource iconError();

      @Source("PackageLinkColumn.css")
      Styles styles();
   }

   static Resources RESOURCES = GWT.create(Resources.class);
   static
   {
      RESOURCES.styles().ensureInjected();
   }

   static NameTemplate NAME_TEMPLATE = GWT.create(NameTemplate.class);
   static IconTemplate ICON_TEMPLATE = GWT.create(IconTemplate.class);
}
