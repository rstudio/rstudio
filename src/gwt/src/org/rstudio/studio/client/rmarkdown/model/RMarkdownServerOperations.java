/*
 * RMarkdownServerOperations.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rmarkdown.model;

import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.JavaScriptObject;

public interface RMarkdownServerOperations extends CryptoServerOperations
{
   void getRMarkdownContext(
            ServerRequestCallback<RMarkdownContext> requestCallback);
   
   void installRMarkdown(
         ServerRequestCallback<ConsoleProcess> requestCallback);
   
   void renderRmd(String file, int line, String encoding,
                  ServerRequestCallback<Boolean> requestCallback);
   
   void terminateRenderRmd(ServerRequestCallback<Void> requestCallback);
   
   
   void convertToYAML(JavaScriptObject input, 
                      ServerRequestCallback<RmdYamlResult> requestCallback);

   public String getApplicationURL(String pathName);
}
