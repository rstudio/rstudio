/*
 * CodeSearchItemKind.java
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
package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.studio.client.workbench.codesearch.model.SourceItem;

import com.google.gwt.core.client.GWT;

// Names the kind of a code search result, used as alt text for its icon.
// Labels follow the item's type rather than its icon, since several kinds
// (e.g. theorem-style cross-references) reuse another kind's icon.
class CodeSearchItemKind
{
   // Returns "" for a cross-reference type with no known kind, so the icon is
   // treated as decorative rather than misnamed.
   static String label(int type, String xrefType)
   {
      if (xrefType != null)
         return xrefLabel(xrefType);

      switch (type)
      {
      case SourceItem.FUNCTION:   return constants_.functionKind();
      case SourceItem.METHOD:     return constants_.methodKind();
      case SourceItem.CLASS:      return constants_.classKind();
      case SourceItem.ENUM:       return constants_.enumKind();
      case SourceItem.ENUM_VALUE: return constants_.enumValueKind();
      case SourceItem.NAMESPACE:  return constants_.namespaceKind();
      case SourceItem.SECTION:    return constants_.sectionKind();
      case SourceItem.TEST:       return constants_.testKind();
      case SourceItem.ROXYGEN:    return constants_.roxygenKind();
      case SourceItem.MACRO:      return constants_.macroKind();
      case SourceItem.PACKAGE:    return constants_.packageKind();
      // the backend indexes R variables with the NONE type
      case SourceItem.NONE:
      default:                    return constants_.variableKind();
      }
   }

   private static String xrefLabel(String xrefType)
   {
      switch (xrefType)
      {
      case "h1": return constants_.headingKind(1);
      case "h2": return constants_.headingKind(2);
      case "h3": return constants_.headingKind(3);
      case "h4": return constants_.headingKind(4);
      case "h5": return constants_.headingKind(5);
      case "h6": return constants_.headingKind(6);
      case "sec": return constants_.sectionKind();
      case "fig": return constants_.figureKind();
      case "tab":
      case "tbl": return constants_.tableKind();
      case "lst": return constants_.listingKind();
      case "thm": return constants_.theoremKind();
      case "lem": return constants_.lemmaKind();
      case "cor": return constants_.corollaryKind();
      case "prp": return constants_.propositionKind();
      case "cnj": return constants_.conjectureKind();
      case "def": return constants_.definitionKind();
      case "exm": return constants_.exampleKind();
      case "exr": return constants_.exerciseKind();
      case "eq":  return constants_.equationKind();
      default:    return "";
      }
   }

   private static final CodeSearchConstants constants_ = GWT.create(CodeSearchConstants.class);
}
