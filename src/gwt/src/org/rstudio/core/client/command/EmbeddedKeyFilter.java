/*
 * EmbeddedKeyFilter.java
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
package org.rstudio.core.client.command;

// Decides which shortcuts may run while focus is in an embedded component
// that handles its own text input, such as the terminal or Posit Assistant.
// Only workbench-level commands run there; editor- and language-specific
// commands leave the key with the component.
public class EmbeddedKeyFilter
{
   public static boolean isWorkbenchLevel(AppCommand.Context context)
   {
      return context == AppCommand.Context.Workbench ||
             context == AppCommand.Context.Addin ||
             context == AppCommand.Context.PackageDevelopment;
   }
}
