package org.rstudio.studio.client.workbench.codesearch.model;

import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.studio.client.workbench.codesearch.CodeSearchResources;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

class CodeSearchSuggestion implements Suggestion
{
   public CodeSearchSuggestion(CodeSearchResult result)
   {
      // save result
      result_ = result;

      // compute display string
      CodeSearchResources res = CodeSearchResources.INSTANCE;
      CodeSearchResources.Styles styles = res.styles();
      
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      SafeHtmlUtil.appendImage(sb, styles.functionImage(), res.function());
      SafeHtmlUtil.appendSpan(sb, 
                              styles.functionName(), 
                              result_.getFunctionName());                   
      SafeHtmlUtil.appendSpan(sb, 
                              styles.functionContext(),
                              "(" + result_.getContext() + ")");
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
   
   public CodeSearchResult getResult()
   {
      return result_;
   }
   
   private final CodeSearchResult result_ ;
   private final String displayString_;
}