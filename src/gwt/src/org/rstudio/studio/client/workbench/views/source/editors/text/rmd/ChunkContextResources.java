/*
 * ChunkContextResources.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface ChunkContextResources extends ClientBundle
{
   @Source("runChunk_2x.png")
   ImageResource runChunk2x();

   @Source("runChunkPending_2x.png")
   ImageResource runChunkPending2x();

   @Source("runPreviousChunksLight_2x.png")
   ImageResource runPreviousChunksLight2x();

   @Source("runPreviousChunksDark_2x.png")
   ImageResource runPreviousChunksDark2x();

   @Source("interruptChunk_2x.png")
   ImageResource interruptChunk2x();

   @Source("chunkOptionsLight_2x.png")
   ImageResource chunkOptionsLight2x();

   @Source("chunkOptionsDark_2x.png")
   ImageResource chunkOptionsDark2x();
}
