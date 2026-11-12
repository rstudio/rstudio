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

   public void testPackageSkeletonTemplatesCreatePackages()
   {
      assertTrue(description("Rcpp", "Rcpp.package.skeleton").createsPackage());
      assertTrue(description("RcppArmadillo", "RcppArmadillo.package.skeleton").createsPackage());
      assertTrue(description("RcppEigen", "RcppEigen.package.skeleton").createsPackage());
      assertTrue(description("devtools", "create").createsPackage());
   }

   public void testOtherTemplatesDoNotCreatePackages()
   {
      assertFalse(description("bookdown", "bookdown_skeleton").createsPackage());
      assertFalse(description("(local)", "create").createsPackage());
   }

   // a package and binding must both match, not just one of them
   public void testPackageAndBindingMustMatchTogether()
   {
      assertFalse(description("Rcpp", "Rcpp.plugin.maker").createsPackage());
      assertFalse(description("mypkg", "Rcpp.package.skeleton").createsPackage());
   }

   private static ProjectTemplateDescription description(String pkg, String binding)
   {
      return JsonUtils.safeEval(
            "{\"package\": \"" + pkg + "\", \"binding\": \"" + binding + "\"," +
            " \"title\": \"Test\"}");
   }
}
