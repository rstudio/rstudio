package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.user.client.ui.Composite;

public abstract class NewRmdBaseOption extends Composite 
                                       implements NewRmdFormatOption
{
   public NewRmdBaseOption(RmdTemplateFormatOption option)
   {
      option_ = option;
   }
   
   @Override
   public RmdTemplateFormatOption getOption()
   {
      return option_;
   }
   
   private RmdTemplateFormatOption option_;
}
