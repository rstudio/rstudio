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

import org.rstudio.core.client.container.SafeMap;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.icons.code.CodeIcons;
import org.rstudio.studio.client.workbench.codesearch.model.SourceItem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;

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
      {
         XRefKind kind = XREF_KINDS.get(xrefType);
         return kind == null ? "" : kind.label;
      }

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

   // Returns null for a cross-reference type with no icon of its own.
   static ImageResource xrefIcon(String xrefType)
   {
      XRefKind kind = XREF_KINDS.get(xrefType);
      return kind == null ? null : new ImageResource2x(kind.icon);
   }

   private static class XRefKind
   {
      XRefKind(ImageResource icon, String label)
      {
         this.icon = icon;
         this.label = label;
      }

      final ImageResource icon;
      final String label;
   }

   // One entry per cross-reference type, so a type can't get an icon
   // without a label or the reverse.
   private static SafeMap<String, XRefKind> createXRefKinds()
   {
      CodeIcons icons = CodeIcons.INSTANCE;
      SafeMap<String, XRefKind> map = new SafeMap<>();

      // section headers
      map.put("h1", new XRefKind(icons.sectionH12x(), constants_.headingKind(1)));
      map.put("h2", new XRefKind(icons.sectionH22x(), constants_.headingKind(2)));
      map.put("h3", new XRefKind(icons.sectionH32x(), constants_.headingKind(3)));
      map.put("h4", new XRefKind(icons.sectionH42x(), constants_.headingKind(4)));
      map.put("h5", new XRefKind(icons.sectionH52x(), constants_.headingKind(5)));
      map.put("h6", new XRefKind(icons.sectionH62x(), constants_.headingKind(6)));
      map.put("sec", new XRefKind(icons.section2x(), constants_.sectionKind()));

      // figures, tables, and code listings
      map.put("fig", new XRefKind(icons.figure2x(), constants_.figureKind()));
      map.put("tab", new XRefKind(icons.table2x(), constants_.tableKind()));
      map.put("tbl", new XRefKind(icons.table2x(), constants_.tableKind()));
      map.put("lst", new XRefKind(icons.snippet2x(), constants_.listingKind()));

      // math-related sections (e.g. theorems)
      map.put("thm", new XRefKind(icons.function2x(), constants_.theoremKind()));
      map.put("lem", new XRefKind(icons.function2x(), constants_.lemmaKind()));
      map.put("cor", new XRefKind(icons.function2x(), constants_.corollaryKind()));
      map.put("prp", new XRefKind(icons.function2x(), constants_.propositionKind()));
      map.put("cnj", new XRefKind(icons.function2x(), constants_.conjectureKind()));
      map.put("def", new XRefKind(icons.function2x(), constants_.definitionKind()));
      map.put("exm", new XRefKind(icons.function2x(), constants_.exampleKind()));
      map.put("exr", new XRefKind(icons.function2x(), constants_.exerciseKind()));
      map.put("eq", new XRefKind(icons.function2x(), constants_.equationKind()));

      return map;
   }

   private static final CodeSearchConstants constants_ = GWT.create(CodeSearchConstants.class);
   private static final SafeMap<String, XRefKind> XREF_KINDS = createXRefKinds();
}
