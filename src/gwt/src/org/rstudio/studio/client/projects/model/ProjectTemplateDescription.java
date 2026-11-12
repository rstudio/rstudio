/*
 * ProjectTemplateDescription.java
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
package org.rstudio.studio.client.projects.model;

import java.util.Arrays;
import java.util.List;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.resources.client.ImageResource;

public class ProjectTemplateDescription extends JavaScriptObject
{
   protected ProjectTemplateDescription() {}
   
   public native final String getPackage()          /*-{ return this["package"]; }-*/;
   public native final String getBinding()          /*-{ return this["binding"]; }-*/;
   public native final String getTitle()            /*-{ return this["title"]; }-*/;
   public native final String getSubTitle()         /*-{ return this["subtitle"]; }-*/;
   public native final String getPageCaption()      /*-{ return this["caption"]; }-*/;
   public native final JsArrayString getOpenFiles() /*-{ return this["open_files"]; }-*/;
   
   // These templates (registered by addKnownProjectTemplates() in
   // SessionProjectTemplate.cpp) pass the project directory name to their
   // skeleton function as the package name
   public final boolean createsPackage()
   {
      return PACKAGE_TEMPLATES.contains(getPackage() + "::" + getBinding());
   }

   public final ImageResource getIcon()      { return getIconImpl("icon"); }
   public final ImageResource getIconLarge() { return getIconImpl("icon_large"); }
   
   public native final JsArray<ProjectTemplateWidgetDescription> getWidgetDescription()
   /*-{
      return this["widgets"] || [];
   }-*/;
   
   public final ImageResource getIconImpl(String field)
   {
      String data = getIconData(field);
      if (StringUtil.isNullOrEmpty(data))
         return null;
      
      return new ProjectTemplateIconImageResource(field, data);
   }
   
   private native final String getIconData(String field) /*-{ return this[field]; }-*/;

   private static final List<String> PACKAGE_TEMPLATES = Arrays.asList(
         "devtools::create",
         "Rcpp::Rcpp.package.skeleton",
         "RcppArmadillo::RcppArmadillo.package.skeleton",
         "RcppEigen::RcppEigen.package.skeleton");
}
