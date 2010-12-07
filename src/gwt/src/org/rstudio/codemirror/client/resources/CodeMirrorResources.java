/*
 * CodeMirrorResources.java
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
package org.rstudio.codemirror.client.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.TextResource;
import org.rstudio.core.client.resources.StaticDataResource;

public interface CodeMirrorResources extends ClientBundle
{
   public static final CodeMirrorResources INSTANCE = GWT.create(CodeMirrorResources.class);

   @Source("codemirror.min.js")
   TextResource codemirror();

   @Source("codemirror_inner.min.js")
   StaticDataResource codemirror_inner();

   @CssResource.NotStrict
   CssResource linenumbers();
}
