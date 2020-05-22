/*
 * DiffResult.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class DiffResult extends JavaScriptObject
{
   protected DiffResult() {}

   /**
    * The encoding that we assumed for the underlying source file. This is
    * important because we need to reverse the encoding when sending the
    * diff back to the server as a patch.
    */
   public native final String getSourceEncoding() /*-{
      return this.source_encoding;
   }-*/;

   /**
    * The actual value of the diff. This is always in UTF-8 (or if no encoding
    * could be determined, the raw bytes).
    */
   public native final String getDecodedValue() /*-{
      return this.decoded_value;
   }-*/;
}
