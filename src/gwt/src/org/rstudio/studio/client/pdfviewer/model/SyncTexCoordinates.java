/*
 * SyncTexCoordinates.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.pdfviewer.model;

public class SyncTexCoordinates
{
   public SyncTexCoordinates(int pageNum, int x, int y)
   {
      pageNum_ = pageNum;
      x_ = x;
      y_ = y;
   }

   public int getPageNum()
   {
      return pageNum_;
   }

   public int getX()
   {
      return x_;
   }

   public int getY()
   {
      return y_;
   }

   private final int pageNum_;
   private final int x_;
   private final int y_;
}
