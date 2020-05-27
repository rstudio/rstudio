/*
 * SynctexServerOperations.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

package org.rstudio.studio.client.common.synctex.model;

import org.rstudio.studio.client.server.ServerRequestCallback;

public interface SynctexServerOperations
{
   void applyForwardConcordance(String rootDocument,
                                SourceLocation sourceLocation,
                                ServerRequestCallback<SourceLocation> callback);
   
   void synctexForwardSearch(String rootDocument,
                             SourceLocation sourceLocation, 
                             ServerRequestCallback<PdfLocation> callback);
   
   
   void synctexInverseSearch(PdfLocation pdfLocation,
                             ServerRequestCallback<SourceLocation> callback);
   
   void applyInverseConcordance(SourceLocation sourceLocation,
                                ServerRequestCallback<SourceLocation> callback);
  
}
