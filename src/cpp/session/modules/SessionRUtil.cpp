/*
 * SessionRUtil.cpp
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

#include <session/SessionRUtil.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/Macros.hpp>
#include <core/YamlUtil.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionModuleContext.hpp>

#include <boost/property_tree/ptree.hpp>
#include <boost/property_tree/ini_parser.hpp>
#include <boost/regex.hpp>

#include "shiny/SessionShiny.hpp"

namespace rstudio {

using namespace core;
using namespace core::yaml;

namespace session {
namespace r_utils {


namespace {

Error extractRCode(const std::string& contents,
                   const std::string& reOpen,
                   const std::string& reClose,
                   std::string* pContent)
{
   using namespace r::exec;
   RFunction extract(".rs.extractRCode");
   extract.addParam(contents);
   extract.addParam(reOpen);
   extract.addParam(reClose);
   Error error = extract.callUtf8(pContent);
   return error;
}

} // anonymous namespace

Error extractRCode(const std::string& fileContents,
                   const std::string& documentType,
                   std::string* pCode)
{
   using namespace source_database;
   Error error = Success();
   
   if (documentType == kSourceDocumentTypeRSource)
      *pCode = fileContents;
   else if (documentType == kSourceDocumentTypeRMarkdown)
      error = extractRCode(fileContents,
                           "^\\s*[`]{3}{\\s*[Rr](?:}|[\\s,].*})\\s*$",
                           "^\\s*[`]{3}\\s*$",
                           pCode);
   else if (documentType == kSourceDocumentTypeSweave)
      error = extractRCode(fileContents,
                           "^\\s*<<.*>>=\\s*$",
                           "^\\s*@\\s*$",
                           pCode);
   else if (documentType == kSourceDocumentTypeCpp)
      error = extractRCode(fileContents,
                           "^\\s*/[*]{3,}\\s*[rR]\\s*$",
                           "^\\s*[*]+/",
                           pCode);
   
   return error;
}

std::set<std::string> implicitlyAvailablePackages(const FilePath& filePath,
                                                  const std::string& contents)
{
   std::set<std::string> dependencies;
   
   if (modules::shiny::getShinyFileType(filePath, contents) != 
       modules::shiny::ShinyNone)
      dependencies.insert("shiny");
   
   return dependencies;
}

std::set<std::string> implicitlyAvailablePackages(const FilePath& filePath)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
      LOG_ERROR(error);
   
   return implicitlyAvailablePackages(filePath, contents);
}

namespace {

SEXP rs_fromJSON(SEXP objectSEXP)
{
   std::string contents = r::sexp::asString(objectSEXP);
   
   json::Value jsonValue;
   if (jsonValue.parse(contents))
      return R_NilValue;
   
   r::sexp::Protect protect;
   return r::sexp::create(jsonValue, &protect);
}

SEXP rs_isNullExternalPointer(SEXP objectSEXP)
{
   using namespace r::sexp;
   
   Protect protect;
   return create(isNullExternalPointer(objectSEXP), &protect);
}

SEXP readInitFileLevel(boost::property_tree::ptree pt, r::sexp::Protect& protect)
{
   using namespace boost::property_tree;

   if (pt.empty())
   {
      std::string value = std::string(pt.data());
      SEXP valueSEXP = create(value, &protect);
      return valueSEXP;
   }

   std::map<std::string, SEXP> entries;
   for (ptree::iterator it = pt.begin(); it != pt.end(); it++)
   {
      std::string key = it->first;
      ptree value = it->second;

      entries[key] = readInitFileLevel(value, protect);
   }

   return create(entries, &protect);
}

SEXP rs_readIniFile(SEXP iniPathSEXP)
{
    using namespace boost::property_tree;
    std::string iniPath = r::sexp::asString(iniPathSEXP);
    FilePath iniFile(iniPath);
    if (!iniFile.exists())
      return R_NilValue;

   std::shared_ptr<std::istream> pIfs;
   Error error = FilePath(iniFile).openForRead(pIfs);
   if (error)
   {
      return R_NilValue;
   }

   try
   {
      ptree pt;
      ini_parser::read_ini(iniFile.getAbsolutePath(), pt);

      r::sexp::Protect protect;
      return readInitFileLevel(pt, protect);
   }
   catch(const std::exception& e)
   {
      LOG_ERROR_MESSAGE("Error reading " + iniFile.getAbsolutePath() +
                        ": " + std::string(e.what()));

      return R_NilValue;
   }
}

SEXP rs_rResourcesPath()
{
   r::sexp::Protect protect;
   return r::sexp::create(session::options().rResourcesPath().getAbsolutePath(), &protect);
}

class Process;

std::set<boost::shared_ptr<async_r::AsyncRProcess>> s_registry;

class Process : public async_r::AsyncRProcess
{
public:
   
   Process(SEXP callbacks)
      : callbacks_(callbacks)
   {
   }
   
   ~Process()
   {
   }
   
protected:
   void onStarted(core::system::ProcessOperations& operations)
   {
      invokeCallback("started");
   }
   
   bool onContinue()
   {
      bool ok = AsyncRProcess::onContinue();
      if (!ok)
         return false;
      
      invokeCallback("continue");
      return true;
   }
   
   void onStdout(const std::string& output)
   {
      invokeCallback("stdout", output);
   }
   
   void onStderr(const std::string& output)
   {
      invokeCallback("stderr", output);
   }
   
   void onCompleted(int exitStatus)
   {
      invokeCallback("completed", exitStatus);
      s_registry.erase(shared_from_this());
   }
   
private:
   
   template <typename T>
   void invokeCallback(const std::string& event, const T& output)
   {
      SEXP callback = R_NilValue;
      Error error = r::sexp::getNamedListSEXP(callbacks_.get(), event, &callback);
      if (error)
         return;
      
      r::exec::RFunction(callback).addParam(output).call();
   }
   
   void invokeCallback(const std::string& event)
   {
      SEXP callback = R_NilValue;
      Error error = r::sexp::getNamedListSEXP(callbacks_.get(), event, &callback);
      if (error)
         return;
      
      r::exec::RFunction(callback).call();
   }
   
   r::sexp::PreservedSEXP callbacks_;
};

static void launchProcess(const std::string& code,
                          const FilePath& workingDir,
                          SEXP callbacks)
{
   boost::shared_ptr<Process> process = boost::make_shared<Process>(callbacks);
   process->start(code.c_str(), workingDir, async_r::R_PROCESS_VANILLA);
   s_registry.insert(process);
}

SEXP rs_runAsyncRProcess(SEXP codeSEXP,
                         SEXP workingDirSEXP,
                         SEXP callbacksSEXP)
{
   using namespace async_r;
   
   std::string code = r::sexp::asString(codeSEXP);
   std::string workingDir = r::sexp::asString(workingDirSEXP);
   boost::algorithm::replace_all(code, "\n", "; ");
   launchProcess(code, module_context::resolveAliasedPath(workingDir), callbacksSEXP);
   
   r::sexp::Protect protect;
   return r::sexp::create(true, &protect);
}

} // anonymous namespace

Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_fromJSON, 1);
   RS_REGISTER_CALL_METHOD(rs_isNullExternalPointer, 1);
   RS_REGISTER_CALL_METHOD(rs_readIniFile, 1);
   RS_REGISTER_CALL_METHOD(rs_rResourcesPath, 0);
   RS_REGISTER_CALL_METHOD(rs_runAsyncRProcess, 3);
   
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionRUtil.R"));
   return initBlock.execute();
}

} // namespace r_utils
} // namespace session
} // namespace rstudio
