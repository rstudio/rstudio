/*
 * LspServerOperations.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.lsp;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;

public interface LspServerOperations
{
   /**
    * Notify the session that a source document has received focus. The
    * session broadcasts this to interested modules (e.g. IDE assistants)
    * as an LSP-style textDocument/didFocus event.
    */
   public void lspDocFocused(String documentId,
                             ServerRequestCallback<VoidResponse> requestCallback);
}
