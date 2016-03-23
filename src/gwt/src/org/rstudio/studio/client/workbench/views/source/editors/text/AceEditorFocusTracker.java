/*
 * AceEditorFocusTracker.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.user.client.Window;

public class AceEditorFocusTracker
{
   public static final void setLastFocusedEditor(AceEditor editor)
   {
      setLastFocusedEditor(editor, getMainWindow());
   }
   
   public static final AceEditor getLastFocusedEditor()
   {
      return getLastFocusedEditor(getMainWindow());
   }
   
   private static final native void setLastFocusedEditor(
         AceEditor editor,
         Window wnd)
   /*-{
      wnd.$lastFocusedEditor = editor;
   }-*/;
   
   private static final native AceEditor getLastFocusedEditor(Window wnd)
   /*-{
      return wnd.$lastFocusedEditor;
   }-*/;
   
   private static final native Window getMainWindow() /*-{
      var wnd = $wnd;
      if (wnd.opener)
         wnd = wnd.opener;
      return wnd;
   }-*/;
}
