/*
 * ProjectTemplateWidgetDescription.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class ProjectTemplateWidgetDescription extends JavaScriptObject
{
   protected ProjectTemplateWidgetDescription()
   {
   }
   
   public final native String getParameter()     /*-{ return this["parameter"]; }-*/;
   public final native String getType()          /*-{ return this["type"]; }-*/;
   public final native String getLabel()         /*-{ return this["label"]; }-*/;
   public final native String getDefault()       /*-{ return this["default"]; }-*/;
   public final native String getPosition()      /*-{ return this["position"]; }-*/;
   public final native JsArrayString getFields() /*-{ return this["fields"]; }-*/;
}
