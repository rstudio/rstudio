/*
 * AsyncShimGenerator.java
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
package org.rstudio.core.rebind;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.ext.DefaultExtensions;
import com.google.gwt.resources.ext.ResourceGeneratorType;
import com.google.gwt.resources.rg.ImageResourceGenerator;

@DefaultExtensions(value = {"_2x.png","_2x.jpg","_2x.gif","_2x.bmp",".png",".jpg",".gif",".bmp"})
@ResourceGeneratorType(ImageResourceGenerator.class)
public interface ImageResource_2x extends ImageResource
{
}