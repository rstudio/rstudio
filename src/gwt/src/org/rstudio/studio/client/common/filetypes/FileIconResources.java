/*
 * FileIconResources.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface FileIconResources extends ClientBundle
{
   public static final FileIconResources INSTANCE =
                                           GWT.create(FileIconResources.class);

   @Source("iconCsv_2x.png")
   ImageResource iconCsv2x();

   @Source("iconFolder_2x.png")
   ImageResource iconFolder2x();

   @Source("iconPublicFolder_2x.png")
   ImageResource iconPublicFolder2x();

   @Source("iconUpFolder_2x.png")
   ImageResource iconUpFolder2x();

   @Source("iconPdf_2x.png")
   ImageResource iconPdf2x();

   @Source("iconPng_2x.png")
   ImageResource iconPng2x();

   @Source("iconRdata_2x.png")
   ImageResource iconRdata2x();

   @Source("iconRproject_2x.png")
   ImageResource iconRproject2x();

   @Source("iconRdoc_2x.png")
   ImageResource iconRdoc2x();

   @Source("iconRhistory_2x.png")
   ImageResource iconRhistory2x();

   @Source("iconRprofile_2x.png")
   ImageResource iconRprofile2x();

   @Source("iconTex_2x.png")
   ImageResource iconTex2x();

   @Source("iconText_2x.png")
   ImageResource iconText2x();

   @Source("iconPython_2x.png")
   ImageResource iconPython2x();

   @Source("iconSql_2x.png")
   ImageResource iconSql2x();

   @Source("iconSh_2x.png")
   ImageResource iconSh2x();

   @Source("iconYaml_2x.png")
   ImageResource iconYaml2x();

   @Source("iconXml_2x.png")
   ImageResource iconXml2x();

   @Source("iconMarkdown_2x.png")
   ImageResource iconMarkdown2x();

   @Source("iconMermaid_2x.png")
   ImageResource iconMermaid2x();

   @Source("iconGraphviz_2x.png")
   ImageResource iconGraphviz2x();

   @Source("iconH_2x.png")
   ImageResource iconH2x();

   @Source("iconRmarkdown_2x.png")
   ImageResource iconRmarkdown2x();

   @Source("iconC_2x.png")
   ImageResource iconC2x();

   @Source("iconHpp_2x.png")
   ImageResource iconHpp2x();

   @Source("iconCpp_2x.png")
   ImageResource iconCpp2x();

   @Source("iconHTML_2x.png")
   ImageResource iconHTML2x();

   @Source("iconCss_2x.png")
   ImageResource iconCss2x();

   @Source("iconJavascript_2x.png")
   ImageResource iconJavascript2x();

   @Source("iconRsweave_2x.png")
   ImageResource iconRsweave2x();

   @Source("iconRd_2x.png")
   ImageResource iconRd2x();

   @Source("iconRhtml_2x.png")
   ImageResource iconRhtml2x();

   @Source("iconRnotebook_2x.png")
   ImageResource iconRnotebook2x();

   @Source("iconRpresentation_2x.png")
   ImageResource iconRpresentation2x();

   @Source("iconWord_2x.png")
   ImageResource iconWord2x();

   @Source("iconDCF_2x.png")
   ImageResource iconDCF2x();

   @Source("iconSourceViewer_2x.png")
   ImageResource iconSourceViewer2x();

   @Source("iconProfiler_2x.png")
   ImageResource iconProfiler2x();
   
   // Ace modes
   @Source("iconClojure_2x.png")
   ImageResource iconClojure2x();

   @Source("iconCoffee_2x.png")
   ImageResource iconCoffee2x();

   @Source("iconCsharp_2x.png")
   ImageResource iconCsharp2x();

   @Source("iconGitignore_2x.png")
   ImageResource iconGitignore2x();

   @Source("iconGo_2x.png")
   ImageResource iconGo2x();

   @Source("iconGroovy_2x.png")
   ImageResource iconGroovy2x();

   @Source("iconHaskell_2x.png")
   ImageResource iconHaskell2x();

   @Source("iconHaxe_2x.png")
   ImageResource iconHaxe2x();

   @Source("iconJava_2x.png")
   ImageResource iconJava2x();

   @Source("iconJulia_2x.png")
   ImageResource iconJulia2x();

   @Source("iconLisp_2x.png")
   ImageResource iconLisp2x();

   @Source("iconLua_2x.png")
   ImageResource iconLua2x();

   @Source("iconMakefile_2x.png")
   ImageResource iconMakefile2x();

   @Source("iconMatlab_2x.png")
   ImageResource iconMatlab2x();

   @Source("iconPerl_2x.png")
   ImageResource iconPerl2x();

   @Source("iconRuby_2x.png")
   ImageResource iconRuby2x();

   @Source("iconRust_2x.png")
   ImageResource iconRust2x();

   @Source("iconScala_2x.png")
   ImageResource iconScala2x();

   @Source("iconSnippets_2x.png")
   ImageResource iconSnippets2x();
   
   @Source("iconStan_2x.png")
   ImageResource iconStan2x();
   
   @Source("iconObjectExplorer_2x.png")
   ImageResource iconObjectExplorer2x();
}
