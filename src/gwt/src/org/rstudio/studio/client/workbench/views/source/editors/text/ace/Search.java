/*
 * Search.java
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

public class Search extends JavaScriptObject
{
   public static native Search create(String needle,
                                      boolean backwards,
                                      boolean wrap,
                                      boolean caseSensitive,
                                      boolean wholeWord,
                                      boolean fromSelection,
                                      boolean selectionOnly,
                                      boolean regexpMode) /*-{
      var Search = $wnd.require('ace/search').Search;
      return new Search().set({
         needle: needle,
         backwards: backwards,
         wrap: wrap,
         caseSensitive: caseSensitive,
         wholeWord: wholeWord,
         start: fromSelection ? null : {row: 0, column: 0},
         scope: selectionOnly ? Search.SELECTION : Search.ALL,
         regExp: regexpMode
      })
   }-*/;

   protected Search() {}

   public final native Range find(EditSession session) /*-{
      return this.find(session);
   }-*/;
}
