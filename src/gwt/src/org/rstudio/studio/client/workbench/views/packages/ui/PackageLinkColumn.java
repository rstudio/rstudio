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

import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackageVulnerabilityTypes.PackageVulnerability;
import org.rstudio.studio.client.workbench.views.packages.model.PackageVulnerabilityTypes.PackageVulnerabilityList;
import org.rstudio.studio.client.workbench.views.packages.model.PackageVulnerabilityTypes.PackageVulnerabilityListMap;
import org.rstudio.studio.client.workbench.views.packages.model.PackageVulnerabilityTypes.RepositoryPackageVulnerabilityListMap;

import com.google.gwt.cell.client.AbstractCell;
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
public abstract class PackageLinkColumn extends Column<PackageInfo, PackageInfo>
{
   public static class PackageLinkCell extends AbstractCell<PackageInfo>
   {
      public PackageLinkCell(final ListDataProvider<PackageInfo> dataProvider,
                             final PackagesDataGridStyle style,
                             final RepositoryPackageVulnerabilityListMap vulns,
                             final OperationWithInput<PackageInfo> onClicked,
                             final boolean alwaysUnderline)
      {
         super("click");

         dataProvider_ = dataProvider;
         style_ = style;
         vulns_ = vulns;
         onClicked_ = onClicked;
         alwaysUnderline_ = alwaysUnderline;
      }

      // render anchor using custom styles. detect selection and
      // add selected style to invert text color
      @Override
      public void render(Context context, PackageInfo value, SafeHtmlBuilder sb)
      {
         if (value == null)
            return;

         String classNames = alwaysUnderline_
               ? RESOURCES.styles().link() + " " + RESOURCES.styles().linkUnderlined()
               : RESOURCES.styles().link();

         sb.appendHtmlConstant("<div class=\"" + style_.packageColumn() + "\">");
         addVulnerabilityInfo(context, value, sb);
         sb.append(TEMPLATES.renderPackageName(classNames, value.getName()));
         sb.appendHtmlConstant("</div>");
      }

      private void addVulnerabilityInfo(Context context, PackageInfo value, SafeHtmlBuilder sb)
      {
         final Mutable<Boolean> didFindVulnerability = new Mutable<>(false);

         String name = value.getName();
         String version = value.getVersion();

         vulns_.forEach((String key) ->
         {
            PackageVulnerabilityListMap pvlMap = vulns_.get(key);
            if (pvlMap == null || !pvlMap.has(name))
               return;

            String vulnText = "";
            List<PackageVulnerability> pvList = pvlMap.get(name).asList();
            for (PackageVulnerability pvItem : pvList)
            {
               if (pvItem.versions.has(version))
               {
                  vulnText += "\n- " + pvItem.id + ": " + pvItem.summary;
               }
            }
            if (vulnText.isEmpty())
               return;

            String title =
               name + " " + version + " has the following known vulnerabilities:\n" + vulnText;

            SafeUri uri = RESOURCES.iconWarning().getSafeUri();
            sb.append(TEMPLATES.renderIcon(RESOURCES.styles().icon(), title, uri));
            didFindVulnerability.set(true);
            return;
         });

         if (!didFindVulnerability.get())
         {
            sb.append(TEMPLATES.renderIconPlaceholder(RESOURCES.styles().iconPlaceholder()));
         }
      }

      // click event which occurs on the actual package link div
      // results in showing help for that package
      @Override
      public void onBrowserEvent(Context context, Element parent, PackageInfo value,
                                 NativeEvent event, ValueUpdater<PackageInfo> valueUpdater)
      {
         super.onBrowserEvent(context, parent, value, event, valueUpdater);
         if ("click".equals(event.getType()))
         {
            // verify that the click was on the package link
            JavaScriptObject target = event.getEventTarget().cast();
            if (!Element.is(target))
               return;

            Element targetEl = Element.as(target);
            if (targetEl.hasClassName(RESOURCES.styles().link()))
            {
               onPackageNameClicked(context);
               return;
            }

            if (targetEl.hasClassName(RESOURCES.styles().icon()))
            {
               onIconClicked(context);
               return;
            }
         }
      }

      private void onPackageNameClicked(Context context)
      {
         int idx = context.getIndex();
         List<PackageInfo> data = dataProvider_.getList();
         if (idx >= 0 && idx < dataProvider_.getList().size())
         {
            onClicked_.execute(data.get(idx));
         }
      }

      private void onIconClicked(Context context)
      {
         int idx = context.getIndex();
         List<PackageInfo> data = dataProvider_.getList();
         if (idx >= 0 && idx < dataProvider_.getList().size())
         {
            PackageInfo info = data.get(idx);
            String name = info.getName();
            vulns_.forEach((String key) ->
            {
               PackageVulnerabilityListMap pvlMap = vulns_.get(key);
               if (pvlMap == null || !pvlMap.has(name))
                  return;

               PackageVulnerabilityList pvList = pvlMap.get(name);
               PackageVulnerabilityModalDialog dialog = new PackageVulnerabilityModalDialog(info, pvList);
               dialog.showModal();
            });
         }
      }

      private final ListDataProvider<PackageInfo> dataProvider_;
      private final PackagesDataGridStyle style_;
      private final RepositoryPackageVulnerabilityListMap vulns_;
      private final OperationWithInput<PackageInfo> onClicked_;
      private final boolean alwaysUnderline_;
   }

   public PackageLinkColumn(final ListDataProvider<PackageInfo> dataProvider,
                            final PackagesDataGridStyle styles,
                            final RepositoryPackageVulnerabilityListMap vulns,
                            final OperationWithInput<PackageInfo> onClicked,
                            final boolean alwaysUnderline)
   {
      super(new PackageLinkCell(dataProvider, styles, vulns, onClicked, alwaysUnderline));
   }

   interface Templates extends SafeHtmlTemplates
   {
      @Template("<span class=\"{0}\" title=\"{1}\">{1}</span>")
      SafeHtml renderPackageName(String className, String title);

      @Template("<img class=\"rstudio_vulnerability_icon {0}\" title=\"{1}\" src=\"{2}\"></img>")
      SafeHtml renderIcon(String className, String hoverInfo, SafeUri imgUri);

      @Template("<div class=\"{0}\"></div>")
      SafeHtml renderIconPlaceholder(String className);
   }

   interface Styles extends CssResource
   {
      String icon();
      String iconPlaceholder();
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

   static Templates TEMPLATES = GWT.create(Templates.class);
}
