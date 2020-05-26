/*
 * RenvAction.java
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

import com.google.gwt.core.client.JavaScriptObject;

public class RenvAction extends JavaScriptObject
{
   protected RenvAction()
   {
   }
   
   public final native String getPackageName()     /*-{ return this.packageName;     }-*/;
   public final native String getLockfileVersion() /*-{ return this.lockfileVersion; }-*/;
   public final native String getLockfileSource()  /*-{ return this.lockfileSource;  }-*/;
   public final native String getLibraryVersion()  /*-{ return this.libraryVersion;  }-*/;
   public final native String getLibrarySource()   /*-{ return this.librarySource;   }-*/;
   public final native String getAction()          /*-{ return this.action;          }-*/;
}
