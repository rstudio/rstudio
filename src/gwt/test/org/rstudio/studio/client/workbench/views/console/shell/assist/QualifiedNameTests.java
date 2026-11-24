/*
 * QualifiedNameTests.java
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

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.junit.client.GWTTestCase;

public class QualifiedNameTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Renders a completion as the list shows it.
   private static Element render(QualifiedName name)
   {
      Element row = Document.get().createDivElement();
      row.setInnerHTML(name.toString());
      return row;
   }

   // Returns the text only screen readers see, or null if there is none.
   private static Element hiddenKind(Element row)
   {
      NodeList<Element> spans = row.getElementsByTagName("span");
      Element found = null;
      for (int i = 0; i < spans.getLength(); i++)
      {
         if (spans.getItem(i).hasClassName(ThemeStyles.INSTANCE.visuallyHidden()))
         {
            assertNull("more than one hidden kind", found);
            found = spans.getItem(i);
         }
      }
      return found;
   }

   private static void assertKindFollowsName(Element row, String name, String kind)
   {
      Element hidden = hiddenKind(row);
      assertNotNull(hidden);
      assertEquals(", " + kind, hidden.getInnerText());
      assertEquals(name, hidden.getPreviousSiblingElement().getInnerText());
   }

   public void testFunctionReadsNameThenKindThenPackage()
   {
      Element row = render(
            new QualifiedName("cat", "base", false, RCompletionType.FUNCTION));
      assertKindFollowsName(row, "cat", "function");
      assertEquals("{base}", hiddenKind(row).getNextSiblingElement().getInnerText());
   }

   public void testTypesSharingAnIconHaveTheirOwnKind()
   {
      // Vectors and arguments both show the variable icon.
      assertKindFollowsName(
            render(new QualifiedName("x", "", false, RCompletionType.VECTOR)),
            "x", "vector");
      assertKindFollowsName(
            render(new QualifiedName("na.rm = ", "mean", false, RCompletionType.ARGUMENT)),
            "na.rm = ", "argument");
   }

   public void testUnknownTypeHasNoKind()
   {
      // Its kind isn't known, so it gets no label rather than a wrong one.
      assertNull(hiddenKind(render(
            new QualifiedName("x", "", false, RCompletionType.UNKNOWN))));
      assertNull(hiddenKind(render(
            new QualifiedName("plotOutput", "", false, RCompletionType.CONTEXT))));
   }

   private static QualifiedName inLanguage(String name, int type, String language)
   {
      return new QualifiedName(name, name, "", false, type, false, false, "",
            null, language, RCompletionManager.AutocompletionContext.TYPE_UNKNOWN);
   }

   public void testPythonAndSqlHaveNoKind()
   {
      // They reuse R's types with other meanings: a Python module is an
      // ENVIRONMENT, and a SQL table a DATAFRAME and its fields DATASETs.
      assertNull(hiddenKind(render(
            inLanguage("path", RCompletionType.ENVIRONMENT, "Python"))));
      assertNull(hiddenKind(render(
            inLanguage("mtcars", RCompletionType.DATAFRAME, "SQL"))));
      assertNull(hiddenKind(render(
            inLanguage("mpg", RCompletionType.DATASET, "SQL"))));
   }

   public void testCompletionsBuiltWithoutLanguageHaveKind()
   {
      // YAML completions are built in the client, with no language.
      assertKindFollowsName(
            render(inLanguage("title", RCompletionType.YAML_KEY, null)),
            "title", "YAML key");
   }

   public void testFileInFolderReadsFileNameThenKind()
   {
      // The file name is shown before the folder it's in.
      Element row = render(new QualifiedName(
            "R/data/iris.csv", "", false, RCompletionType.FILE));
      assertKindFollowsName(row, "iris.csv", "file");
   }

   public void testFolderReadsNameThenKind()
   {
      assertKindFollowsName(
            render(new QualifiedName("data", "", false, RCompletionType.DIRECTORY)),
            "data", "folder");
   }
}
