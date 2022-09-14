/*
 * Clipboard.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.dom;

public class Clipboard
{
   public static final native void setText(String text)
   /*-{ 
      
      // Use newer clipboard APIs if available.
      var clipboard = ($wnd.navigator || {}).clipboard;
      if (clipboard != null) {
         try {
            clipboard.writeText(text);
         } catch (e) {
            console.warn("Copy to clipboard failed: ", e);
         }
         return;
      }
      
      // Use 'document.execCommand()' for older browsers.
      if ($doc.queryCommandSupported && $doc.queryCommandSupported("copy")) {
      
         // prepare text area for copy
         var textarea = $doc.createElement("textarea");
         textarea.textContent = text;
         textarea.style.position = "fixed";  // Prevent scrolling to bottom of page in Microsoft Edge.
         $doc.body.appendChild(textarea);
         textarea.select();
         
         // Security exception may be thrown by some browsers. 
         try {
            $doc.execCommand("copy");
         }
         catch (ex) {
            console.warn("Copy to clipboard failed.", ex);
         }
         finally {
            $doc.body.removeChild(textarea);
         }
      }
      
   }-*/;
      
}
