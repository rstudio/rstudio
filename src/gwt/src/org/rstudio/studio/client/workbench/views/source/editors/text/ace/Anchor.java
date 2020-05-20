/*
 * Anchor.java
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

public class Anchor extends JavaScriptObject
{
   protected Anchor()
   {}
   
   public native final int getRow() /*-{ return this.row; }-*/;
   public native final int getColumn() /*-{ return this.column; }-*/;

   public native final Position getPosition() /*-{
      return this.getPosition();
   }-*/;

   public native final void detach() /*-{
      this.removeAllListeners("change");
      this.detach();
   }-*/;
   
   public native final void setInsertRight(boolean insertRight) /*-{
      this.$insertRight = insertRight;
   }-*/;

   public native final void addOnChangeHandler(Command onChange) /*-{
      this.on("change", 
             $entry(function () {
                onChange.@com.google.gwt.user.client.Command::execute()();
             }));
   }-*/;

   public native static Anchor createAnchor(Document document, int row, int column) /*-{
      var Anchor = $wnd.require('ace/anchor').Anchor;
      return new Anchor(document, row, column);
   }-*/;
}
