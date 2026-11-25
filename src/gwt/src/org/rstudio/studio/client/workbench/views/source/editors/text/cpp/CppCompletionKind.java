/*
 * CppCompletionKind.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletion;

import com.google.gwt.core.client.GWT;

// Names the kind of a C/C++ completion for screen readers, which can't see
// its icon. Labels follow the completion's type rather than its icon, since
// e.g. constructors and destructors share the function icon.
class CppCompletionKind
{
   // Returns "" for UNKNOWN, so it gets no label rather than a wrong one.
   static String label(int type)
   {
      switch (type)
      {
      case CppCompletion.VARIABLE:    return constants_.variableKind();
      case CppCompletion.FUNCTION:    return constants_.functionKind();
      case CppCompletion.CONSTRUCTOR: return constants_.constructorKind();
      case CppCompletion.DESTRUCTOR:  return constants_.destructorKind();
      case CppCompletion.CLASS:       return constants_.classKind();
      case CppCompletion.STRUCT:      return constants_.structKind();
      case CppCompletion.NAMESPACE:   return constants_.namespaceKind();
      case CppCompletion.ENUM:        return constants_.enumKind();
      case CppCompletion.ENUM_VALUE:  return constants_.enumValueKind();
      case CppCompletion.KEYWORD:     return constants_.keywordKind();
      case CppCompletion.MACRO:       return constants_.macroKind();
      case CppCompletion.FILE:        return constants_.fileKind();
      case CppCompletion.DIRECTORY:   return constants_.folderKind();
      case CppCompletion.SNIPPET:     return constants_.snippetKind();
      default:                        return "";
      }
   }

   private static final ConsoleConstants constants_ = GWT.create(ConsoleConstants.class);
}
