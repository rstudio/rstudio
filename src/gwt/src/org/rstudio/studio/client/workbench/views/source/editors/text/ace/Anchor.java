/*
 * Anchor.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class Anchor extends JavaScriptObject
{
   protected Anchor()
   {}

   public native final Position getPosition() /*-{
      return this.getPosition();
   }-*/;

   public native final void detach() /*-{
      this.detach();
   }-*/;

   public native static Anchor createAnchor(Document document, int row, int column) /*-{
      var Anchor = $wnd.require('ace/anchor').Anchor;
      return new Anchor(document, row, column);
   }-*/;
}
