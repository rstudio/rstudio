package org.rstudio.studio.client.common.vcs;

import com.google.gwt.resources.client.ImageResource;

public class VCSStrategyGit extends VCSStrategy
{
   @Override
   public ImageResource getSimpleIconForStatus(VCSStatus status)
   {
      switch (status.charAt(1))
      {
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
