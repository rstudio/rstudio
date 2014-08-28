/*
 * TokenCursor.java
 *
 * Copyright (C) 2014 by RStudio, Inc.
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

public class TokenCursor extends JavaScriptObject
{
   protected TokenCursor() {}
   
   public native final TokenCursor cloneCursor() /*-{
      return this.cloneCursor();
   }-*/;
   
   public native final Position currentPosition() /*-{
      return this.currentPosition();
   }-*/;
   
   public native final Token currentToken() /*-{
      return this.currentToken();
   }-*/;
   
   public native final boolean moveToNextToken() /*-{
      return !! this.moveToNextToken();
   }-*/;
   
   public native final boolean moveToPreviousToken() /*-{
      return !! this.moveToPreviousToken();
   }-*/;
   
   public native final void moveToStartOfRow(int row) /*-{
      this.moveToStartOfRow(row);
   }-*/;
   
   public native final boolean seekToNearestToken(Position position, int maxRow) /*-{
      return !! this.seekToNearestToken(position, maxRow);
   }-*/;
   
   public native final boolean bwdToNearestToken(Position position) /*-{
      return !! this.bwdToNearestToken(position);
   }-*/;
   
   public native final boolean moveBackwardOverMatchingParens() /*-{
      return !! this.moveBackwardOverMatchingParens();
   }-*/;
   
}

