/*
 * CodeIcons.java
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
package org.rstudio.studio.client.common.icons.code;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface CodeIcons extends ClientBundle
{
   public static final CodeIcons INSTANCE = GWT.create(CodeIcons.class);
   
   ImageResource variable();
   ImageResource function();
   ImageResource clazz();
   ImageResource namespace();
   ImageResource enumType();
   ImageResource enumValue();
   ImageResource keyword();
   ImageResource dataFrame();
   ImageResource help();
   ImageResource rPackage();
   ImageResource file();
   ImageResource folder();
   ImageResource macro();
   ImageResource environment();
   ImageResource context();
   ImageResource snippet();
}
