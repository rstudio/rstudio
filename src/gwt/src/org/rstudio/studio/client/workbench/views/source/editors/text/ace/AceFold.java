/*
 * AceFold.java
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
import com.google.gwt.core.client.JsArray;

public class AceFold extends JavaScriptObject
{
   public static native AceFold createFold(Range range, String placeholder) /*-{
      var Fold = $wnd.require('ace/edit_session/fold').Fold;
      return new Fold(range, placeholder);
   }-*/;

   protected AceFold() {}

   public native final Range getRange() /*-{ return this.range; }-*/;
   public native final Position getStart() /*-{ return this.start; }-*/;
   public native final Position getEnd() /*-{ return this.end; }-*/;
   public native final String getPlaceholder() /*-{ return this.placeholder; }-*/;

   public native final JsArray<AceFold> getSubFolds() /*-{
      return this.subFolds;
   }-*/;
}
