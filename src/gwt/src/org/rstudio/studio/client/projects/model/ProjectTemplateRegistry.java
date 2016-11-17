/*
 * ProjectTemplateRegistry.java
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

import org.rstudio.core.client.js.JsMap;

import com.google.gwt.core.client.JsArray;

public class ProjectTemplateRegistry extends JsMap<JsArray<ProjectTemplateDescription>>
{
   protected ProjectTemplateRegistry() {}
   
   public static final native ProjectTemplateRegistry createProjectTemplateRegistry()
   /*-{
      return {};
   }-*/;
}
