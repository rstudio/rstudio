/*
 * VimMarks.java
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

import org.rstudio.core.client.js.JsMap;

import com.google.gwt.core.client.JavaScriptObject;

/**
 *  Extract Vim marks with 'editor.state.cm.state.vim.marks'
 *  Add new marks with 'editor.state.cm.
 */
public class VimMarks extends JavaScriptObject
{
   protected VimMarks() {}
   
   // Encoding: "<key>:<row>,<column>", split with newlines '\n'
   public static native final String encode(JsMap<Position> marks) /*-{
      var result = [];
      for (var key in marks) {
         var mark = marks[key];
         result.push(key + ":" + mark.row + "," + mark.column);
      }
      return result.join("\n"); 
   }-*/;
   
   public static final native JsMap<Position> decode(String encoded) /*-{
   
      if (encoded == null || encoded.length === 0)
         return {};
         
      var result = {};
      var splat = encoded.split("\n");
      for (var i = 0; i < splat.length; i++) {
         
         var spec = splat[i].split(":");
         var key = spec[0];
         var pos = spec[1].split(",");
         
         result[key] = {
            row: parseInt(pos[0], 10),
            column: parseInt(pos[1], 10)
         };
         
      }
      
      return result;
   
   }-*/;
}
