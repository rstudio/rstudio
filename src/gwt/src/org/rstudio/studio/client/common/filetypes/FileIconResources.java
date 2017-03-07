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

   ImageResource iconCsv();
   ImageResource iconFolder();
   ImageResource iconPublicFolder();
   ImageResource iconUpFolder();
   ImageResource iconPdf();
   ImageResource iconPng();
   ImageResource iconRdata();
   ImageResource iconRproject();
   ImageResource iconRdoc();
   ImageResource iconRhistory();
   ImageResource iconRprofile();
   ImageResource iconTex();
   ImageResource iconText();
   ImageResource iconPython();
   ImageResource iconSql();
   ImageResource iconSh();
   ImageResource iconYaml();
   ImageResource iconXml();
   ImageResource iconMarkdown();
   ImageResource iconMermaid();
   ImageResource iconGraphviz();
   ImageResource iconH();
   ImageResource iconC();
   ImageResource iconHpp();
   ImageResource iconCpp();
   ImageResource iconHTML();
   ImageResource iconCss();
   ImageResource iconJavascript();
   ImageResource iconRsweave();
   ImageResource iconRd();
   ImageResource iconRhtml();
   ImageResource iconRmarkdown();
   ImageResource iconRnotebook();
   ImageResource iconRpresentation();
   @Source("iconSourceViewer_2x.png")
   ImageResource iconSourceViewer2x();
   @Source("iconProfiler_2x.png")
   ImageResource iconProfiler2x();
   ImageResource iconWord();
   ImageResource iconDCF();
   
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
}
