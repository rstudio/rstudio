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
}
