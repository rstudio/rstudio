/*
 * ProjectTemplateDescription.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.projects.model;

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
}
