/*
 * ProjectTemplateDescriptionTests.java
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
package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.junit.client.GWTTestCase;

public class ProjectTemplateDescriptionTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCreatesPackageWhenSessionSetsFlag()
   {
      assertTrue(description(", \"creates_package\": true").createsPackage());
   }

   public void testDoesNotCreatePackageWhenFlagIsFalse()
   {
      assertFalse(description(", \"creates_package\": false").createsPackage());
   }

   // templates from other packages' DCF files never carry the flag
   public void testDoesNotCreatePackageWhenFlagIsMissing()
   {
      assertFalse(description("").createsPackage());
   }

   private static ProjectTemplateDescription description(String extraFields)
   {
      return JsonUtils.safeEval(
            "{\"package\": \"Rcpp\", \"binding\": \"Rcpp.package.skeleton\"," +
            " \"title\": \"Test\"" + extraFields + "}");
   }
}
