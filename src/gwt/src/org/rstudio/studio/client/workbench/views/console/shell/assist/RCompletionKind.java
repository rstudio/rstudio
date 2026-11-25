/*
 * RCompletionKind.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.console.shell.ConsoleLanguageTracker;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;

import com.google.gwt.core.client.GWT;

// Names the kind of an R completion for screen readers, which can't see its
// icon. Labels follow the completion's type rather than its icon, since
// several types (e.g. vectors and arguments) share the variable icon.
class RCompletionKind
{
   // Returns "" for types whose kind isn't known (e.g. UNKNOWN, or CONTEXT,
   // which covers unrelated things such as Shiny IDs and Stan identifiers),
   // so they get no label rather than a wrong one. Python and SQL completions,
   // and those from a package's own provider (which sets a help handler, e.g.
   // reticulate for os$path), reuse R's types with other meanings: a SQL
   // table is a DATAFRAME, a Python module an ENVIRONMENT.
   static String label(QualifiedName item)
   {
      if (StringUtil.equals(item.language, ConsoleLanguageTracker.LANGUAGE_PYTHON) ||
          StringUtil.equals(item.language, LANGUAGE_SQL) ||
          item.helpHandler != null)
         return "";

      int type = item.type;

      if (RCompletionType.isFunctionType(type))
         return constants_.functionKind();

      switch (type)
      {
      case RCompletionType.VECTOR:      return constants_.vectorKind();
      case RCompletionType.ARRAY:       return constants_.arrayKind();
      case RCompletionType.DATAFRAME:   return constants_.dataFrameKind();
      case RCompletionType.LIST:        return constants_.listKind();
      case RCompletionType.ENVIRONMENT: return constants_.environmentKind();
      case RCompletionType.ARGUMENT:
      case RCompletionType.SECUNDARY_ARGUMENT:
         return constants_.argumentKind();
      case RCompletionType.S4_CLASS:
      case RCompletionType.R5_CLASS:
         return constants_.classKind();
      case RCompletionType.S4_OBJECT:
      case RCompletionType.R5_OBJECT:
      case RCompletionType.R6_OBJECT:
         return constants_.objectKind();
      case RCompletionType.FILE:        return constants_.fileKind();
      case RCompletionType.DIRECTORY:   return constants_.folderKind();
      case RCompletionType.ROXYGEN:     return constants_.roxygenTagKind();
      case RCompletionType.HELP:        return constants_.helpTopicKind();
      case RCompletionType.STRING:      return constants_.stringKind();
      case RCompletionType.PACKAGE:     return constants_.packageKind();
      case RCompletionType.KEYWORD:     return constants_.keywordKind();
      case RCompletionType.OPTION:      return constants_.optionKind();
      case RCompletionType.DATASET:     return constants_.datasetKind();
      case RCompletionType.YAML_KEY:    return constants_.yamlKeyKind();
      case RCompletionType.YAML_VALUE:  return constants_.yamlValueKind();
      case RCompletionType.COLUMN:      return constants_.columnKind();
      case RCompletionType.DATATABLE_SPECIAL_SYMBOL:
         return constants_.dataTableSymbolKind();
      case RCompletionType.ACTIVE_BINDING:
         return constants_.activeBindingKind();
      case RCompletionType.SNIPPET:     return constants_.snippetKind();
      default:                          return "";
      }
   }

   private static final String LANGUAGE_SQL = "SQL";
   private static final ConsoleConstants constants_ = GWT.create(ConsoleConstants.class);
}
