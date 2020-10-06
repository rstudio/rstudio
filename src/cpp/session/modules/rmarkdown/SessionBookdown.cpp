/*
 * SessionBookdown.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include "SessionBookdown.hpp"

#include <core/Exec.hpp>
#include <shared_core/FilePath.hpp>

#include <r/RExec.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionBookdownXRefs.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace {

std::vector<std::string> bookdownFrontMatterValue(const std::string& name)
{
   std::vector<std::string> values;
   if (module_context::isBookdownProject() && module_context::isPackageInstalled("bookdown"))
   {
      FilePath buildTargetPath = projects::projectContext().buildTargetPath();
      std::string inputDir = string_utils::utf8ToSystem(buildTargetPath.getAbsolutePath());
      Error error = r::exec::RFunction(".rs.bookdown.frontMatterValue", inputDir, name).call(&values);
      if (error)
         LOG_ERROR(error);
   }
   return values;
}



} // anonymous namespace

namespace module_context {

// currently we implement this function in SessionBookdown.cpp b/c it's the
// only known source of project level bibliographies
std::vector<FilePath> bookdownBibliographies()
{
   std::vector<std::string> biblios = bookdownBibliographiesRelative();
   if (biblios.size() > 0)
   {
       FilePath buildTargetPath = projects::projectContext().buildTargetPath();
       std::vector<FilePath> biblioPaths;
       std::transform(biblios.begin(), biblios.end(), std::back_inserter(biblioPaths),
                      boost::bind(&FilePath::completeChildPath, &buildTargetPath, _1));
       return biblioPaths;
   }
   else
   {
      return std::vector<FilePath>();
   }
}

std::vector<std::string> bookdownBibliographiesRelative()
{
   return bookdownFrontMatterValue("bibliography");
}

std::vector<std::string> bookdownZoteroCollections()
{
  return bookdownFrontMatterValue("zotero");
}

FilePath bookdownCSL()
{
   std::vector<std::string> cslVector = bookdownFrontMatterValue("csl");
   std::string csl = cslVector.size() > 0 ? cslVector[0] : "";
   if (!csl.empty())
   {
      FilePath buildTargetPath = projects::projectContext().buildTargetPath();
      return buildTargetPath.completePath(csl);
   }
   else
   {
      return FilePath();
   }
}

} // namespace module_context

namespace modules {
namespace rmarkdown {
namespace bookdown {

using namespace rstudio::core;

namespace {


} // anonymous namespace

Error initialize()
{
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bookdown::xrefs::initialize)
   ;
   return initBlock.execute();
}

} // namespace bookdown
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
