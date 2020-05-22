/*
 * ProjectContext.java
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
package org.rstudio.studio.client.workbench.projects;

import org.rstudio.studio.client.packrat.model.PackratContext;

import com.google.gwt.core.client.JavaScriptObject;

public class ProjectContext extends JavaScriptObject
{
   protected ProjectContext()
   {
   }
   
   public final boolean isActive()
   {
      RenvContext renv = getRenvContext();
      if (renv.active)
         return true;
      
      PackratContext packrat = getPackratContext();
      if (packrat.isModeOn())
         return true;
      
      return false;
   }
   
   public final PackratContext getPackratContext()
   {
      PackratContext context = getPackratContextImpl();
      if (context == null)
         return PackratContext.empty();
      return context;
   }
   
   private final native PackratContext getPackratContextImpl()
   /*-{
      return this.packrat_context;
   }-*/;
   
   public final native RenvContext getRenvContext() /*-{ return this.renv_context; }-*/;
}
