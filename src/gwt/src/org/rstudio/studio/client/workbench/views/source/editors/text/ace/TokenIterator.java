/*
 * TokenIterator.java
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

public class TokenIterator extends JavaScriptObject
{
   public static native TokenIterator create(EditSession session,
                                             int initialRow,
                                             int initialColumn) /*-{
      var TokenIterator = $wnd.require('ace/token_iterator').TokenIterator;
      return new TokenIterator(session, initialRow, initialColumn);
   }-*/;

   protected TokenIterator() {}

   public native final Token stepForward() /*-{
      return this.stepForward();
   }-*/;

   public native final Token stepBackward() /*-{
      return this.stepBackward();
   }-*/;

   public native final Token getCurrentToken() /*-{
      return this.getCurrentToken();
   }-*/;

   public native final int getCurrentTokenRow() /*-{
      return this.getCurrentTokenRow();
   }-*/;

   public native final int getCurrentTokenColumn() /*-{
      return this.getCurrentTokenColumn();
   }-*/;
}
