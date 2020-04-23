/*
 * UndoManager.java
 *
 * Copyright (C) 2009-12 by RStudio, PBC
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

public class UndoManager extends JavaScriptObject
{
   protected UndoManager()
   {}

   public native final JavaScriptObject peek() /*-{
      return this.peek();
   }-*/;
   
   public native final void undo() /*-{
      this.undo();
   }-*/;
   
   public native final void redo() /*-{
      this.redo();
   }-*/;
   
   public native final boolean canUndo() /*-{
      return this.canUndo();
   }-*/;
   
   public native final boolean canRedo() /*-{
      return this.canRedo();
   }-*/;
   
   public native final int getRevision() /*-{
      this.getRevision();
   }-*/;
   
   public native final void bookmark() /*-{
      this.bookmark();
   }-*/;
   
   public native final void bookmark(int revision) /*-{
      this.bookmark(revision);
   }-*/;
   
   public native final boolean isAtBookmark() /*-{
      return this.isAtBookmark();
   }-*/;
}
