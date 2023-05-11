/*
 * LintResources.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.output.lint;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface LintResources extends ClientBundle
{
   public static interface Styles extends CssResource
   {
      String ignored();
      String note();
      String info();
      String style();
      
      String warning();
      String spelling();
      
      String error();
      String fatal();
   }
   
   @Source("Lint.css")
   Styles styles();
   
   @Source("LintRetina.css")
   Styles retinaStyles();
   
   ImageResource note();
   ImageResource warning();
   ImageResource error();
   ImageResource spelling();

   ImageResource note2x();
   ImageResource warning2x();
   ImageResource error2x();
   ImageResource spelling2x();
   
   public static final LintResources INSTANCE =
         GWT.create(LintResources.class);
   
}
