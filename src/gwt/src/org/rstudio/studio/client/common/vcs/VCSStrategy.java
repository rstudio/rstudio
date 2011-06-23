package org.rstudio.studio.client.common.vcs;

import com.google.gwt.resources.client.ImageResource;

public abstract class VCSStrategy
{
   public abstract ImageResource getSimpleIconForStatus(VCSStatus status);

   public static VCSStrategy getCurrentStrategy()
   {
      return new VCSStrategyGit();
   }
}
