/*
 * RMarkdownTemplates.cpp
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

#include "RMarkdownTemplates.hpp"

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <shared_core/FilePath.hpp>
#include <core/StringUtils.hpp>
#include <shared_core/json/Json.hpp>
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
      FilePath templateRoot = pkgPath.completePath("rmarkdown")
                                     .completePath("templates");

      // skip if this folder doesn't exist or isn't a directory
      if (!templateRoot.exists() || !templateRoot.isDirectory())
         return;

      // get a list of all template folders under the root
      std::vector<FilePath> templateDirs;
      Error error = templateRoot.getChildren(templateDirs);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      // for each template folder found, look for a valid template inside
      for (const FilePath& templateDir : templateDirs)
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

      std::string templateYaml;
      bool multiFile;

      SEXP templateFile;
      r::sexp::Protect protect;
      Error error = r::exec::RFunction(
         ".rs.getTemplateYamlFile", 
           string_utils::utf8ToSystem(templateDir.getAbsolutePath()))
         .call(&templateFile, &protect);

      // .rs.getTemplateDetails may return null if the template is not
      // well-formed
      if (error || TYPEOF(templateFile) == NILSXP)
         return;

      r::sexp::getNamedListElement(templateFile,
                                   "template_yaml", &templateYaml);
      r::sexp::getNamedListElement(templateFile,
                                   "multi_file", &multiFile);

      dataJson["package_name"] = pkgName;
      dataJson["path"] = templateDir.getAbsolutePath();
      dataJson["template_yaml"] = templateYaml;
      dataJson["multi_file"] = multiFile;

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
   Error error;
   json::Array result;
   for (auto it: s_templates)
   {
      // skip if not an object type
      if (!it.isObject())
         continue;

      // if we already know this template's name, no need to re-parse
      json::Object item = it.getObject();
      if (item.find("name") != item.end())
      {
         result.push_back(item);
         continue;
      }

      // read filename and directory info
      bool multiFile = false;
      std::string templateYaml;
      error = json::readObject(item, 
            "multi_file",    multiFile,
            "template_yaml", templateYaml);
      if (error)
         continue;

      // read template details
      SEXP templateDetails;
      r::sexp::Protect protect;
      error = r::exec::RFunction(
         ".rs.getTemplateDetails", string_utils::utf8ToSystem(templateYaml))
         .call(&templateDetails, &protect);
      if (error)
         continue;

      // load template name/description
      std::string name;
      std::string description;
      bool createDirFlag;
      r::sexp::getNamedListElement(templateDetails,
                                   "name", &name);
      r::sexp::getNamedListElement(templateDetails,
                                   "description", &description);
      r::sexp::getNamedListElement(templateDetails,
                                   "create_dir", &createDirFlag);

      // append to metadata already known
      item["name"] = name;
      item["description"] = description;

      // force directory creation if multi file
      item["create_dir"] = (createDirFlag || multiFile) ? "true" : "false";

      // save result to be delivered to client
      result.push_back(item);
   }
   pResponse->setResult(result);
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
