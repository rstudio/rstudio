#
# SessionRMarkdown.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("scalarListFromList", function(l)
{
   # hint that every non-list element of the hierarchical list l
   # is a scalar value
   l <- lapply(l, function(ele) {
      if (is.list(ele)) 
         .rs.scalarListFromList(ele)
      else
         .rs.scalar(ele)
   })
})

.rs.addFunction("updateRMarkdownPackage", function(archive) 
{
  pkgDir <- find.package("rmarkdown")
  .rs.forceUnloadPackage("rmarkdown")
  utils::install.packages(archive,
                          lib = dirname(pkgDir),
                          repos = NULL,
                          type = "source")
})

.rs.addJsonRpcHandler("convert_to_yaml", function(input)
{
   list(yaml = .rs.scalar(yaml::as.yaml(input)))
})

.rs.addJsonRpcHandler("convert_from_yaml", function(yaml)
{
   list(data = .rs.scalarListFromList(yaml::yaml.load(yaml)))
})
