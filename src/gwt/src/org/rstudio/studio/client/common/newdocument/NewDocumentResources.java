/*
 * NewDocumentResources.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

package org.rstudio.studio.client.common.newdocument;


import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface NewDocumentResources extends ClientBundle
{
   @Source("MarkdownPresentationIcon_2x.png")
   ImageResource presentationIcon2x();

   @Source("MarkdownDocumentIcon_2x.png")
   ImageResource documentIcon2x();

   @Source("MarkdownOptionsIcon_2x.png")
   ImageResource optionsIcon2x();
   
   @Source("MarkdownTemplateIcon_2x.png")
   ImageResource templateIcon2x();

   @Source("MarkdownShinyIcon_2x.png")
   ImageResource shinyIcon2x();
   
   @Source("NewDocumentStyles.css")
   NewDocumentStyles styles();
}

