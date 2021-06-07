/*
 * SessionQuarto.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include "SessionQuarto.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/YamlUtil.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/projects/SessionProjects.hpp>

#include <string>


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace quarto {
namespace {

const char * const kQuartoXt = "quarto-document";

bool s_quartoInstalled = false;

bool pathHasQuartoConfig(const FilePath& filePath)
{
   return filePath.completePath("_quarto.yml").exists() ||
          filePath.completePath("_quarto.yaml").exists();
}

std::string onDetectQuartoSourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!pDoc->path().empty())
   {
      FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
      if (filePath.getExtensionLowerCase() == ".qmd")
      {
         return kQuartoXt;
      }
      else if (filePath.getExtensionLowerCase() == ".rmd" ||
          filePath.getExtensionLowerCase() == ".md")
      {
         // if we have a format: or knit: quarto render then it's a quarto document
         std::string yamlHeader = yaml::extractYamlHeader(pDoc->contents());
         static const boost::regex reOutput("output:\\s*");
         static const boost::regex reFormat("format:\\s*");
         static const boost::regex reJupyter("jupyter:\\s*");
         static const boost::regex reKnitQuarto("knit:\\s*quarto\\s+render");
         // format: without output:
         if (regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reFormat) &&
             !regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reOutput))
         {
            return kQuartoXt;
         }
         // knit: quarto render
         else if (regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reKnitQuarto))
         {
            return kQuartoXt;
         }
         // jupyter:
         else if (filePath.getExtensionLowerCase() == ".md" &&
                  regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reJupyter))
         {
            return kQuartoXt;
         }
         // project has quarto config in build target dir
         else if (filePath.isWithin(projects::projectContext().buildTargetPath()) && projectIsQuarto())
         {
            return kQuartoXt;

         // file has a parent directory with a quarto config
         } else if (isInstalled()) {

            // list all paths up to root from home dir
            // if we hit the anchor path, or any parent directory of that path
            FilePath anchorPath = module_context::userHomePath();
            std::vector<FilePath> anchorPaths;
            for (; anchorPath.exists(); anchorPath = anchorPath.getParent())
               anchorPaths.push_back(anchorPath);

            // scan through parents
            for (FilePath targetPath = filePath.getParent(); targetPath.exists(); targetPath = targetPath.getParent())
            {
               // bail if we've hit our anchor
               for (const FilePath& anchorPath : anchorPaths)
               {
                  if (targetPath == anchorPath)
                     return "";
               }

               // see if we have a config
               if (pathHasQuartoConfig(targetPath))
               {
                  return kQuartoXt;
               }
            }
         }
      }
   }

   // quarto type not detected
   return "";
}


} // anonymous namespace

bool isInstalled(bool check)
{
   if (check)
   {
      s_quartoInstalled = !module_context::findProgram("quarto").isEmpty();
   }

   return s_quartoInstalled;
}

bool projectIsQuarto()
{
   using namespace session::projects;
   const ProjectContext& context = projectContext();
   if (context.hasProject())
   {
      return pathHasQuartoConfig(context.buildTargetPath());
   } else {
      return false;
   }
}

Error initialize()
{
   // update status at startup
   isInstalled(true);

   module_context::events().onDetectSourceExtendedType
                                        .connect(onDetectQuartoSourceType);

   return Success();
}

} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio
