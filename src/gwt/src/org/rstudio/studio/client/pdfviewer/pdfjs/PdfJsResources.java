/*
 * PdfJsResources.java
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
package org.rstudio.studio.client.pdfviewer.pdfjs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import org.rstudio.core.client.resources.StaticDataResource;

public interface PdfJsResources extends ClientBundle
{
   public static PdfJsResources INSTANCE = GWT.create(PdfJsResources.class);

   @Source("pdf.min.js")
   StaticDataResource pdfjs();

   @Source("compatibility.min.js")
   StaticDataResource compatibilityJs();

   @Source("debugger.min.js")
   StaticDataResource debuggerJs();

   @Source("viewer.min.js")
   StaticDataResource viewerJs();

   @Source("viewer.css")
   StaticDataResource viewerCss();
}
