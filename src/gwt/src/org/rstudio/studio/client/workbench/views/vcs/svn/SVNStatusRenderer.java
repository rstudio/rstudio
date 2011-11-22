/*
 * SVNStatusRenderer.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;

public class SVNStatusRenderer implements SafeHtmlRenderer<String>
{
   @Override
   public SafeHtml render(String object)
   {
      return SafeHtmlUtils.fromString(object);
   }

   @Override
   public void render(String object, SafeHtmlBuilder builder)
   {
      SafeHtml html = render(object);
      if (html != null)
         builder.append(html);
   }
}
