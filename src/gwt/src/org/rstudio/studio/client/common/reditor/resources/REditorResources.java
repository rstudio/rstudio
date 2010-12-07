/*
 * REditorResources.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.reditor.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.DataResource;
import org.rstudio.core.client.resources.StaticDataResource;

public interface REditorResources extends ClientBundle
{
   public static final REditorResources INSTANCE = GWT.create(REditorResources.class);
   
   static interface Styles extends CssResource
   {
      String editbox();
   }
   
   @Source("colors.css")
   @NotStrict
   Styles styles();
   
   @Source("colors.css")
   DataResource colors();

   @Source("parse_r.js")
   StaticDataResource parser_r();

   @Source("parselatex.js")
   StaticDataResource parser_latex();

   @Source("parsesweave.js")
   StaticDataResource parser_sweave();

   @Source("parsedummy.js")
   StaticDataResource parser_dummy();
}
