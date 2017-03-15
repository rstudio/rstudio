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
   
   @Source("variable_2x.png")
   ImageResource variable2x();

   @Source("function_2x.png")
   ImageResource function2x();

   @Source("clazz_2x.png")
   ImageResource clazz2x();

   @Source("namespace_2x.png")
   ImageResource namespace2x();

   @Source("enumType_2x.png")
   ImageResource enumType2x();

   @Source("enumValue_2x.png")
   ImageResource enumValue2x();

   @Source("keyword_2x.png")
   ImageResource keyword2x();

   @Source("dataFrame_2x.png")
   ImageResource dataFrame2x();

   @Source("help_2x.png")
   ImageResource help2x();

   @Source("rPackage_2x.png")
   ImageResource rPackage2x();

   @Source("file_2x.png")
   ImageResource file2x();

   @Source("folder_2x.png")
   ImageResource folder2x();

   @Source("macro_2x.png")
   ImageResource macro2x();

   @Source("environment_2x.png")
   ImageResource environment2x();

   @Source("context_2x.png")
   ImageResource context2x();

   @Source("snippet_2x.png")
   ImageResource snippet2x();
}
