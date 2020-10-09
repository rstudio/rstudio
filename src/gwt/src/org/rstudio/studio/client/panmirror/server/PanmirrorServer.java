/*
 * PanmirrorServer.java
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

package org.rstudio.studio.client.panmirror.server;


import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocServer;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorServer
{    
   public PanmirrorServer()
   {
      this.pandoc = new PanmirrorPandocServer();
      this.crossref = new PanmirrorCrossrefServer();
      this.datacite = new PanmirrorDataCiteServer();
      this.pubmed = new PanmirrorPubMedServer();
      this.zotero = new PanmirrorZoteroServer();
      this.xref = new PanmirrorXRefServer();
      this.doi = new PanmirrorDOIServer();
   }
   
   public PanmirrorPandocServer pandoc;
   public PanmirrorCrossrefServer crossref;
   public PanmirrorDataCiteServer datacite;
   public PanmirrorPubMedServer pubmed;
   public PanmirrorZoteroServer zotero;
   public PanmirrorXRefServer xref;
   public PanmirrorDOIServer doi;
}
