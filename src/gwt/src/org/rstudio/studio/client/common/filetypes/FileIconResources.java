/*
 * FileIconResources.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
   ImageResource iconMarkdown();
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
   ImageResource iconSourceViewer();
}
