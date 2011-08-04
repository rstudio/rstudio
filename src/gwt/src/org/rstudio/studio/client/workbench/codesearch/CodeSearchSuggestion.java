package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.studio.client.workbench.codesearch.model.RSourceItem;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchResources;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

class CodeSearchSuggestion implements Suggestion
{
   public CodeSearchSuggestion(RSourceItem sourceItem)
   {
      // save result
      sourceItem_ = sourceItem;

      // compute display string
      CodeSearchResources res = CodeSearchResources.INSTANCE;
      CodeSearchResources.Styles styles = res.styles();
      
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      ImageResource image = res.function();
      if (sourceItem_.getType() == RSourceItem.METHOD)
         image = res.method();
      else if (sourceItem_.getType() == RSourceItem.CLASS)
         image = res.cls();
      SafeHtmlUtil.appendImage(sb, styles.itemImage(), image);
      SafeHtmlUtil.appendSpan(sb, 
                              styles.itemName(), 
                              sourceItem_.getFunctionName());                   
      SafeHtmlUtil.appendSpan(sb, 
                              styles.itemContext(),
                              "(" + sourceItem_.getContext() + ")");
      displayString_ = sb.toSafeHtml().asString();
   }

   @Override
   public String getDisplayString()
   {
      return displayString_;
   }

   @Override
   public String getReplacementString()
   {
      return "" ;
   }
   
   public RSourceItem getSourceItem()
   {
      return sourceItem_;
   }
   
   private final RSourceItem sourceItem_ ;
   private final String displayString_;
}