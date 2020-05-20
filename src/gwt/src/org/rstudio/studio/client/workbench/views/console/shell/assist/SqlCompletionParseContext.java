/*
 * SqlCompletionParseContext.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.JsVectorString;
import org.rstudio.core.client.js.JsMapString;

import jsinterop.annotations.JsType;

@JsType
public class SqlCompletionParseContext
{
   public final JsVectorString identifiers = JsVectorString.createVector();
   public final JsVectorString tables = JsVectorString.createVector();
   public final JsVectorString schemas = JsVectorString.createVector();
   public final JsMapString aliases = JsMapString.create();
   public String contextKeyword = "";
   public boolean preferLowercaseKeywords = true;
}
