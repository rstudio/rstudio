/*
 * CodeSearchResources.java
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
package org.rstudio.studio.client.workbench.codesearch.ui;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;


public interface CodeSearchResources extends ClientBundle
{
   public static interface Styles extends CssResource
   {
      String codeSearchWidget();
      
      String fileImage();
      String fileItem();
      
      String smallCodeImage();
      String smallCodeItem();
      String smallItemContext();
      
      String codeImage();
      String codeItem();
      
      String xrefImage();
      String xrefItem();
      
      String itemContext();
      String codeSearchDialogMainWidget();
   }

  
   @Source("CodeSearch.css")
   Styles styles();
 
   @Source("gotoFunction_2x.png")
   ImageResource gotoFunction2x();
   
   public static CodeSearchResources INSTANCE = 
      (CodeSearchResources)GWT.create(CodeSearchResources.class);
}
