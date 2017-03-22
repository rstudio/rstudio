/*
 * SVNStatusRenderer.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable.ChangelistTableCellTableResources;

public class SVNStatusRenderer implements SafeHtmlRenderer<String>
{
   interface StatusResources extends ClientBundle
   {
      @Source("images/statusAdded_2x.png")
      ImageResource statusAdded2x();
      @Source("images/statusConflicted_2x.png")
      ImageResource statusConflicted2x();
      @Source("images/statusDeleted_2x.png")
      ImageResource statusDeleted2x();
      @Source("images/statusExternal_2x.png")
      ImageResource statusExternal2x();
      @Source("images/statusIgnored_2x.png")
      ImageResource statusIgnored2x();
      @Source("images/statusMissing_2x.png")
      ImageResource statusMissing2x();
//      @Source("images/statusMerged")
//      ImageResource statusMerged();
      @Source("images/statusModified_2x.png")
      ImageResource statusModified2x();
      @Source("images/statusNone_2x.png")
      ImageResource statusNone2x();
      @Source("images/statusObstructed_2x.png")
      ImageResource statusObstructed2x();
      @Source("images/statusUnversioned_2x.png")
      ImageResource statusUnversioned2x();
   }

   public SVNStatusRenderer()
   {
   }

   @Override
   public SafeHtml render(String str)
   {
      if (str.length() != 1)
         return SafeHtmlUtils.fromString(str);

      ImageResource2x img = imgForStatus(str.charAt(0));

      if (img == null)
         return SafeHtmlUtils.fromString(str);

      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      builder.append(SafeHtmlUtils.fromTrustedString(
            "<span " +
            "class=\"" + ctRes_.cellTableStyle().status() + "\" " +
            "title=\"" +
            SafeHtmlUtils.htmlEscape(descForStatus(str)) +
            "\">"));

      builder.append(img.getSafeHtml());

      builder.appendHtmlConstant("</span>");

      return builder.toSafeHtml();
   }

   private String descForStatus(String str)
   {
      char c = str.charAt(0);
      
      switch (c)
      {
         case 'A':
            return "Added";
         case 'C':
            return "Conflicted";
         case 'D':
            return "Deleted";
         case 'X':
            return "External";
         case 'I':
            return "Ignored";
         case '!':
            return "Missing";
//         case 'G':
//            return resources_.statusMerged();
         case 'M':
            return "Modified";
         case ' ':
            return "";
         case '~':
            return "Obstructed";
         case '?':
            return "Unversioned";
         default:
            return "";
      }
      
   }

  
   
   private ImageResource2x imgForStatus(char c)
   {
      switch (c)
      {
         case 'A':
            return new ImageResource2x(resources_.statusAdded2x());
         case 'C':
            return new ImageResource2x(resources_.statusConflicted2x());
         case 'D':
            return new ImageResource2x(resources_.statusDeleted2x());
         case 'X':
            return new ImageResource2x(resources_.statusExternal2x());
         case 'I':
            return new ImageResource2x(resources_.statusIgnored2x());
         case '!':
            return new ImageResource2x(resources_.statusMissing2x());
//         case 'G':
//            return resources_.statusMerged();
         case 'M':
            return new ImageResource2x(resources_.statusModified2x());
         case ' ':
            return new ImageResource2x(resources_.statusNone2x());
         case '~':
            return new ImageResource2x(resources_.statusObstructed2x());
         case '?':
            return new ImageResource2x(resources_.statusUnversioned2x());
         default:
            return null;
      }
   }

   @Override
   public void render(String str, SafeHtmlBuilder builder)
   {
      SafeHtml safeHtml = render(str);
      if (safeHtml != null)
         builder.append(safeHtml);
   }

   private static final StatusResources resources_ = GWT.create(StatusResources.class);
   private static final ChangelistTableCellTableResources ctRes_ = GWT.create(ChangelistTableCellTableResources.class);
}
