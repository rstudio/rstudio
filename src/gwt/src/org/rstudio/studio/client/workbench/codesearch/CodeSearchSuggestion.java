package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.codesearch.model.CodeNavigationTarget;
import org.rstudio.studio.client.workbench.codesearch.model.RFileItem;
import org.rstudio.studio.client.workbench.codesearch.model.RSourceItem;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchResources;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

class CodeSearchSuggestion implements Suggestion
{
   public CodeSearchSuggestion(RFileItem fileItem)
   {
      navigationTarget_ = new CodeNavigationTarget(
                     fileItem.getDirectory() + "/" + fileItem.getFilename());
      matchedString_ = fileItem.getFilename();
            
      // compute display string
      ImageResource image = 
         fileTypeRegistry_.getIconForFilename(fileItem.getFilename());
   
      displayString_ = createDisplayString(image,
                                           RES.styles().fileImage(),
                                           fileItem.getFilename(),
                                           null);   
   }
   
   public CodeSearchSuggestion(RSourceItem sourceItem)
   {
      // save result
      navigationTarget_ = CodeNavigationTarget.fromRSourceItem(sourceItem);
      matchedString_ = sourceItem.getFunctionName();
      
      // compute display string
      ImageResource image = RES.function();
      if (sourceItem.getType() == RSourceItem.METHOD)
         image = RES.method();
      else if (sourceItem.getType() == RSourceItem.CLASS)
         image = RES.cls();
      displayString_ = createDisplayString(image, 
                                           RES.styles().itemImage(),
                                           sourceItem.getFunctionName(),
                                           sourceItem.getContext());
   }
   
   public String getMatchedString()
   {
      return matchedString_;
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
   
   public CodeNavigationTarget getNavigationTarget()
   {
      return navigationTarget_;
   }
   
   private String createDisplayString(ImageResource image, 
                                      String imageStyle,
                                      String name, 
                                      String context)
   {    
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      SafeHtmlUtil.appendImage(sb, imageStyle, image);
      SafeHtmlUtil.appendSpan(sb, RES.styles().itemName(), name);      
      if (context != null)
      {
         SafeHtmlUtil.appendSpan(sb, 
                                 RES.styles().itemContext(),
                                 "(" + context + ")");
      }
      return sb.toSafeHtml().asString();
   }
   
   
   private final CodeNavigationTarget navigationTarget_ ;
   private final String matchedString_;
   private final String displayString_;
   private static final FileTypeRegistry fileTypeRegistry_ =
                              RStudioGinjector.INSTANCE.getFileTypeRegistry();
   private static final CodeSearchResources RES = CodeSearchResources.INSTANCE;
}