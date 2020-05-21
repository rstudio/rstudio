/*
 * Marker.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class Marker extends JavaScriptObject
{
   protected Marker() {}
   
   // NOTE: no constructor as these are generated internally by Ace
   public final native Range getRange() /*-{ return this.range; }-*/;
   public final native String getType() /*-{ return this.type; }-*/;
   public final native String getClazz() /*-{ return this.clazz; }-*/;
   public final native boolean getInFront() /*-{ return this.inFront; }-*/;
   public final native int getId() /*-{ return this.id; }-*/;
   
}
