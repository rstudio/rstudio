/*
 * RMarkdownTemplates.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <boost/foreach.hpp>

#include "RMarkdownTemplates.hpp"

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>

#include <session/SessionPackageProvidedExtension.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace templates {
namespace {

json::Array s_templates;

// This class is responsible for discovering R Markdown document templates in
// all installed packages. It works by crawling the installed package base
// looking for an installed template folder, and maintaining a cached JSON
// array of template metadata that can be returned to the client when requested.
class Worker : public ppe::Worker
{
   void onIndexingStarted()
   {
      s_templates.clear();
   }
   
   void onWork(const std::string& pkgName, const FilePath& pkgPath)
   {
      // form the path to the template folder
      FilePath templateRoot = pkgPath.complete("rmarkdown")
                                     .complete("templates");

      // skip if this folder doesn't exist or isn't a directory
      if (!templateRoot.exists() || !templateRoot.isDirectory())
         return;

      // get a list of all template folders under the root
      std::vector<FilePath> templateDirs;
      Error error = templateRoot.children(&templateDirs);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      // for each template folder found, look for a valid template inside
      BOOST_FOREACH(const FilePath& templateDir, templateDirs)
      {
         // skip if not a directory
         if (!templateDir.isDirectory())
            continue;

         discoverTemplate(pkgName, templateDir);
      }
   }

   void discoverTemplate(const std::string& pkgName, 
         const FilePath& templateDir)
   {
      json::Object dataJson;

      std::string name;
      std::string description;
      std::string createDir = "default";

      SEXP templateDetails;
      r::sexp::Protect protect;
      Error error = r::exec::RFunction(
         ".rs.getTemplateDetails", 
           string_utils::utf8ToSystem(templateDir.absolutePath()))
         .call(&templateDetails, &protect);

      // .rs.getTemplateDetails may return null if the template is not
      // well-formed
      if (error || TYPEOF(templateDetails) == NILSXP)
         return;

      r::sexp::getNamedListElement(templateDetails,
                                   "name", &name);
      r::sexp::getNamedListElement(templateDetails,
                                   "description", &description);

      bool createDirFlag = false;
      error = r::sexp::getNamedListElement(templateDetails,
                                           "create_dir",
                                           &createDirFlag);
      createDir = createDirFlag ? "true" : "false";

      dataJson["package_name"] = pkgName;
      dataJson["path"] = templateDir.absolutePath();
      dataJson["name"] = name;
      dataJson["description"] = description;
      dataJson["create_dir"] = createDir;

      s_templates.push_back(dataJson);
   }
   
   void onIndexingCompleted(json::Object* pPayload)
   {
   }
   
public:
   
   Worker() : ppe::Worker() 
   {
   }
};

Error getRmdTemplates(const json::JsonRpcRequest&,
                      json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(s_templates);
   return Success();
}

boost::shared_ptr<Worker>& worker()
{
   static boost::shared_ptr<Worker> instance(new Worker);
   return instance;
}

} // anonymous namespace

core::Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ppe::indexer().addWorker(worker());

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_rmd_templates", getRmdTemplates));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
