package org.rstudio.studio.client.workbench.views.plots;

import org.rstudio.studio.client.workbench.views.plots.model.ManipulatorContext;

public class Manipulator
{
   public interface Display
   {
      void show(ManipulatorContext context);
   }
   
   public Manipulator(Plots.Parent parent)
   {
      parent_ = parent;
   }
   
   
   
   
   
   private final Plots.Parent parent_;
}
