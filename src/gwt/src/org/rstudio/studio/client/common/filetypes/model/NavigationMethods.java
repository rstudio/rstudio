/*
 * NavigationMethods.java
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
package org.rstudio.studio.client.common.filetypes.model;

public class NavigationMethods
{
   public final static int DEFAULT = 0;
   public final static int HIGHLIGHT_LINE = 1;
   public final static int DEBUG_STEP = 2;
   public final static int DEBUG_END = 3;

   // To the position given, then on to the document's first Git change as
   // its gutter markers show it, once they are current (the Git pane's
   // go-to-change: the position is the line Git reports, which the markers
   // can place differently).
   public final static int GIT_FIRST_CHANGE = 4;
}
