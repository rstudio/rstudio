/*
 * Selection.java
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
import com.google.gwt.user.client.Command;

import org.rstudio.core.client.CommandWithArg;

public class Selection extends JavaScriptObject
{
   protected Selection() {}
   
   public native final void selectWord() /*-{
      this.selectWord();
   }-*/;

   public native final Range getRange() /*-{
      return this.getRange();
   }-*/;
   
   public native final void addRange(Range range, boolean blockChangeEvents) /*-{
      this.addRange(range, blockChangeEvents);
   }-*/;
   
   public native final void setSelectionRange(Range range) /*-{
      this.session.unfold(range, true);
      this.setSelectionRange(range);
   }-*/;

   public native final Position getCursor() /*-{
      return this.getCursor();
   }-*/;

   public native final void moveCursorTo(int row,
                                         int column,
                                         boolean preventUpdateDesiredColumn) /*-{
      this.session.unfold({row: row, column: column}, true);
      this.moveCursorTo(row, column, preventUpdateDesiredColumn);
      this.setSelectionAnchor(row, column);
   }-*/;

   public native final boolean isEmpty() /*-{
      return this.isEmpty();
   }-*/;

   public native final void selectAll() /*-{
      this.selectAll();
   }-*/;

   public final void addCursorChangeHandler(final CommandWithArg<Position> handler)
   {
      onCursorChange(new Command()
      {
         public void execute()
         {
            handler.execute(getCursor());
         }
      });
   }

   public native final void moveCursorFileEnd() /*-{
      this.moveCursorFileEnd();
      var cursor = this.getCursor();
      this.setSelectionAnchor(cursor.row, cursor.column);
   }-*/;

   private native void onCursorChange(Command command) /*-{
      this.on("changeCursor",
              $entry(function () {
                 command.@com.google.gwt.user.client.Command::execute()();
              }));
   }-*/;
   
   public native final Range[] getAllRanges() /*-{
      return this.getAllRanges();
   }-*/;
   
}
