/*
 * Range.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

public class Range extends JavaScriptObject
{
   protected Range() {}

   public static native Range fromPoints(Position start, Position end) /*-{
      var Range = $wnd.require('ace/range').Range;
      return Range.fromPoints(start, end);
   }-*/;

   public final native Position getStart() /*-{
      return this.start;
   }-*/;

   public final native Position getEnd() /*-{
      return this.end;
   }-*/;

   public final native boolean isEmpty() /*-{
      return this.isEmpty();
   }-*/;

   public final native Range extend(int row, int column) /*-{
      return this.extend(row, column);
   }-*/;
}
