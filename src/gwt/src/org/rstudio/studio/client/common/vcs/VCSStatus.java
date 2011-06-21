/*
 * VCSStatus.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.resources.client.ImageResource;

public enum VCSStatus
{
   // Must stay in sync with VCSStatus enum in SessionSourceControl.hpp
   Unmodified,
   Untracked,
   Modified,
   Added,
   Deleted,
   Renamed,
   Copied,
   Unmerged,
   Ignored,
   Replaced,
   External,
   Missing,
   Obstructed;

   private static VCSStatus[] all = values();

   public static VCSStatus statusForInt(Integer intVal)
   {
      if (intVal == null || intVal < 0 || intVal >= all.length)
         return Unmodified;
      return all[intVal];
   }

   public static ImageResource getIconForStatus(VCSStatus status)
   {
      switch (status)
      {
         case Untracked:
            return VCSStatusIcons.INSTANCE.untracked();
         case Modified:
            return VCSStatusIcons.INSTANCE.modified();
         case Added:
            return VCSStatusIcons.INSTANCE.added();
         case Deleted:
            return VCSStatusIcons.INSTANCE.deleted();
         case Renamed:
            return VCSStatusIcons.INSTANCE.renamed();
         case Copied:
            return VCSStatusIcons.INSTANCE.copied();
         case Unmerged:
            return VCSStatusIcons.INSTANCE.unmerged();
         case Unmodified:
         default:
            return null;
      }
   }
}
