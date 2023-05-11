/*
 * RUtil.java
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
package org.rstudio.core.client;

public class RUtil
{
   // Given some text, enquote and escape it so that it can be safely submitted
   // as an R string. For example, an input
   //
   //    Hello \ "World"!
   //
   // would be escaped as:
   //
   //    "Hello \\ \"World\"!"
   //
   public static String asStringLiteral(String text)
   {
      return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
   }
}