/*
 * VCSStrategyGit.java
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

public class VCSStrategyGit extends VCSStrategy
{
   @Override
   public ImageResource getSimpleIconForStatus(VCSStatus status)
   {
      switch (status.charAt(1))
      {
         case '?':
         case 'A':
            return VCSStatusIcons.INSTANCE.greendot();
         case 'M':
            if (status.charAt(0) =='A')
               return VCSStatusIcons.INSTANCE.greendot();
            else
               return VCSStatusIcons.INSTANCE.bluedot();
         case 'U':
            return VCSStatusIcons.INSTANCE.bluedot();
         case 'D':
            return VCSStatusIcons.INSTANCE.greendot(); // This would be surprising
         case ' ':
            switch (status.charAt(0))
            {
               case 'A':
                  return VCSStatusIcons.INSTANCE.greendot();
               case 'M':
               case 'R':
               case 'C':
                  return VCSStatusIcons.INSTANCE.bluedot();
            }
            break;
      }
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }
}
