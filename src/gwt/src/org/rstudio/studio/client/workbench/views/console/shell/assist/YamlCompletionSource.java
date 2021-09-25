/*
 * YamlCompletionSource.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

import elemental2.core.JsObject;

public interface YamlCompletionSource
{
   public static final String LOCATION_FILE = "file";
   public static final String LOCATION_FRONT_MATTER = "front-matter";
   public static final String LOCATION_CELL = "cell";
   
   boolean isActive(CompletionContext context);
   
   void getCompletions(String location, String line, String code, Position pos, CommandWithArg<JsObject> results);
}
