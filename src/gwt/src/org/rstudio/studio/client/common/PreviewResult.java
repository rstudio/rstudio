/*
 * PreviewResult.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.common;

import com.google.gwt.core.client.JavaScriptObject;

// Result of a preview consent RPC ('preview_sql', 'preview_r2d3'). The
// 'action' field is one of:
//   "ok"      - the preview is safe to run (and, for SQL, already ran)
//   "error"   - the preview failed; 'message' describes why
//   "confirm" - the preview wants to run code that is not statically safe;
//               'expression' is the R code awaiting the user's consent
public class PreviewResult extends JavaScriptObject
{
   protected PreviewResult() {}

   public final native String getAction()     /*-{ return this.action || ""; }-*/;
   public final native String getMessage()    /*-{ return this.message || ""; }-*/;
   public final native String getExpression() /*-{ return this.expression || ""; }-*/;

   public final boolean isOk()      { return "ok".equals(getAction()); }
   public final boolean isError()   { return "error".equals(getAction()); }
   public final boolean isConfirm() { return "confirm".equals(getAction()); }
}
