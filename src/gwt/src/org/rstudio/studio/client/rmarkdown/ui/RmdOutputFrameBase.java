/*
 * RmdOutputFrameBase.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;

public abstract class RmdOutputFrameBase implements RmdOutputFrame
{
   @Override
   public RmdPreviewParams getPreviewParams()
   {
      return params_;
   }
   
   public void showRmdPreview(RmdPreviewParams params)
   {
      params_ = params;
   }

   private RmdPreviewParams params_;
}
