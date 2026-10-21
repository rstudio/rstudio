/*
 * GitDiffBaseContents.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

// The version of a file that the editor's Git diff gutter compares against.
public class GitDiffBaseContents extends JavaScriptObject
{
   protected GitDiffBaseContents() {}

   // False when the file is outside the repository or not in the index, in
   // which case there is nothing to diff against.
   public final native boolean isTracked() /*-{
      return !!this.tracked;
   }-*/;

   public final native String getContents() /*-{
      return this.contents;
   }-*/;
}
