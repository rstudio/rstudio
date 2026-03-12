/*
 * SessionTrust.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionTrust.hpp"

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>
#include <core/system/Xdg.hpp>

#include <shared_core/json/Json.hpp>

#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionClientEvent.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace trust {

namespace {

enum class TrustStatus
{
   Trusted,
   Untrusted,
   Unknown,     // needs prompt
   NotRequired  // no risky files present
};

TrustStatus s_trustStatus = TrustStatus::NotRequired;
FilePath s_projectDir;

const char* const kRiskyFiles[] = {
   ".Rprofile",
   ".RData",
   ".Renviron"
};

FilePath trustFilePath()
{
   return core::system::xdg::userDataDir().completePath("trust.json");
}

Error readTrustFile(json::Array* pTrusted, json::Array* pUntrusted)
{
   FilePath filePath = trustFilePath();
   if (!filePath.exists())
      return Success();

   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
      return error;

   json::Value value;
   if (value.parse(contents))
      return Success();

   if (!json::isType<json::Object>(value))
      return Success();

   json::Object obj = value.getObject();

   auto trusted = obj.find("trustedDirectories");
   if (trusted != obj.end() && json::isType<json::Array>((*trusted).getValue()))
      *pTrusted = (*trusted).getValue().getArray();

   auto untrusted = obj.find("untrustedDirectories");
   if (untrusted != obj.end() && json::isType<json::Array>((*untrusted).getValue()))
      *pUntrusted = (*untrusted).getValue().getArray();

   return Success();
}

Error writeTrustFile(const json::Array& trusted, const json::Array& untrusted)
{
   json::Object obj;
   obj["trustedDirectories"] = trusted;
   obj["untrustedDirectories"] = untrusted;

   std::ostringstream os;
   obj.writeFormatted(os);
   return writeStringToFile(trustFilePath(), os.str());
}

bool matchesDirectoryList(const FilePath& dir, const json::Array& dirs)
{
   FilePath canonicalDir(dir.getCanonicalPath());
   for (const json::Value& val : dirs)
   {
      if (!json::isType<std::string>(val))
         continue;

      FilePath listed(val.getString());
      if (canonicalDir.isWithin(listed))
         return true;
   }
   return false;
}

// Check if an .Rprofile contains only a call to source("renv/activate.R").
// Strips comments and blank lines, then checks if the sole remaining
// content is the renv activation call.
bool isRprofileSafe(const FilePath& rprofilePath)
{
   std::string contents;
   Error error = readStringFromFile(rprofilePath, &contents);
   if (error)
      return false;

   // process line-by-line: strip comments and blank lines
   std::istringstream stream(contents);
   std::string line;
   std::vector<std::string> codeLines;

   while (std::getline(stream, line))
   {
      // trim whitespace
      std::string trimmed = string_utils::trimWhitespace(line);

      // skip blank lines and comment-only lines
      if (trimmed.empty() || trimmed[0] == '#')
         continue;

      // strip trailing comments (respecting quoted strings)
      bool inSingleQuote = false;
      bool inDoubleQuote = false;
      for (size_t i = 0; i < trimmed.size(); i++)
      {
         char ch = trimmed[i];
         if (ch == '\'' && !inDoubleQuote)
            inSingleQuote = !inSingleQuote;
         else if (ch == '"' && !inSingleQuote)
            inDoubleQuote = !inDoubleQuote;
         else if (ch == '#' && !inSingleQuote && !inDoubleQuote)
         {
            trimmed = string_utils::trimWhitespace(trimmed.substr(0, i));
            break;
         }
      }

      if (!trimmed.empty())
         codeLines.push_back(trimmed);
   }

   // should be exactly one line of code
   if (codeLines.size() != 1)
      return false;

   // check for source("renv/activate.R") or source('renv/activate.R')
   const std::string& code = codeLines[0];
   return code == "source(\"renv/activate.R\")" ||
          code == "source('renv/activate.R')";
}

std::vector<std::string> findRiskyFiles(const FilePath& dir)
{
   std::vector<std::string> result;
   for (const char* filename : kRiskyFiles)
   {
      FilePath filePath = dir.completePath(filename);
      if (!filePath.exists())
         continue;

      // carve-out: .Rprofile containing only source("renv/activate.R")
      if (std::string(filename) == ".Rprofile" && isRprofileSafe(filePath))
         continue;

      // .RData is only risky if workspace restore is enabled
      if (std::string(filename) == ".RData" && !module_context::restoreWorkspaceEnabled())
         continue;

      result.push_back(filename);
   }
   return result;
}

bool hasRiskyFiles(const FilePath& dir)
{
   return !findRiskyFiles(dir).empty();
}

Error addToTrustList(const FilePath& directory, bool trusted)
{
   std::string directoryPath = directory.getCanonicalPath();

   json::Array trustedDirs, untrustedDirs;
   Error error = readTrustFile(&trustedDirs, &untrustedDirs);
   if (error)
      LOG_ERROR(error);

   json::Array& targetList = trusted ? trustedDirs : untrustedDirs;
   json::Array& otherList = trusted ? untrustedDirs : trustedDirs;

   // Remove from the other list if present
   json::Array filteredOther;
   for (const json::Value& val : otherList)
   {
      if (json::isType<std::string>(val) && val.getString() != directoryPath)
         filteredOther.push_back(val);
   }
   otherList = filteredOther;

   // Add to target list if not already present
   bool found = false;
   for (const json::Value& val : targetList)
   {
      if (json::isType<std::string>(val) && val.getString() == directoryPath)
      {
         found = true;
         break;
      }
   }
   if (!found)
      targetList.push_back(directoryPath);

   return writeTrustFile(trustedDirs, untrustedDirs);
}

Error removeFromBothLists(const FilePath& directory)
{
   std::string directoryPath = directory.getCanonicalPath();

   json::Array trustedDirs, untrustedDirs;
   Error error = readTrustFile(&trustedDirs, &untrustedDirs);
   if (error)
      LOG_ERROR(error);

   json::Array filteredTrusted;
   for (const json::Value& val : trustedDirs)
   {
      if (json::isType<std::string>(val) && val.getString() != directoryPath)
         filteredTrusted.push_back(val);
   }

   json::Array filteredUntrusted;
   for (const json::Value& val : untrustedDirs)
   {
      if (json::isType<std::string>(val) && val.getString() != directoryPath)
         filteredUntrusted.push_back(val);
   }

   return writeTrustFile(filteredTrusted, filteredUntrusted);
}

Error grantTrust(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   std::string directory;
   Error error = json::readParams(request.params, &directory);
   if (error)
      return error;

   return addToTrustList(FilePath(directory), true);
}

Error revokeTrust(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   std::string directory;
   Error error = json::readParams(request.params, &directory);
   if (error)
      return error;

   return addToTrustList(FilePath(directory), false);
}

Error resetTrustRpc(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string directory;
   Error error = json::readParams(request.params, &directory);
   if (error)
      return error;

   return removeFromBothLists(FilePath(directory));
}

SEXP rs_trustGrant(SEXP directorySEXP)
{
   std::string directory = r::sexp::asString(directorySEXP);
   Error error = addToTrustList(FilePath(directory), true);
   if (error)
      LOG_ERROR(error);
   return R_NilValue;
}

SEXP rs_trustRevoke(SEXP directorySEXP)
{
   std::string directory = r::sexp::asString(directorySEXP);
   Error error = addToTrustList(FilePath(directory), false);
   if (error)
      LOG_ERROR(error);
   return R_NilValue;
}

SEXP rs_trustReset(SEXP directorySEXP)
{
   std::string directory = r::sexp::asString(directorySEXP);
   Error error = removeFromBothLists(FilePath(directory));
   if (error)
      LOG_ERROR(error);
   return R_NilValue;
}

SEXP rs_trustStatus(SEXP directorySEXP)
{
   std::string directory = r::sexp::asString(directorySEXP);
   FilePath dir(directory);

   json::Array trustedDirs, untrustedDirs;
   Error error = readTrustFile(&trustedDirs, &untrustedDirs);
   if (error)
   {
      LOG_ERROR(error);
      return Rf_mkString("unknown");
   }

   if (matchesDirectoryList(dir, trustedDirs))
      return Rf_mkString("trusted");

   if (matchesDirectoryList(dir, untrustedDirs))
      return Rf_mkString("untrusted");

   return Rf_mkString("default");
}

} // anonymous namespace

void checkTrust(const FilePath& projectDir,
                const FilePath& userHomePath)
{
   s_projectDir = projectDir;

   // Check if trust checking is enabled.
   // Explicit setting (0 or 1) takes precedence; if unset (-1),
   // fall back to the edition-specific overlay default.
   int trustSetting = options().trustEnabled();
   bool trustEnabled = (trustSetting == -1)
      ? overlay::trustEnabledByDefault()
      : (trustSetting != 0);

   if (!trustEnabled)
   {
      s_trustStatus = TrustStatus::Trusted;
      return;
   }

   // Skip trust check if project dir is the user's home directory
   if (projectDir == userHomePath)
   {
      s_trustStatus = TrustStatus::Trusted;
      return;
   }

   // Check for risky files
   if (!hasRiskyFiles(projectDir))
   {
      s_trustStatus = TrustStatus::NotRequired;
      return;
   }

   // Read trust.json and check
   json::Array trustedDirs, untrustedDirs;
   Error error = readTrustFile(&trustedDirs, &untrustedDirs);
   if (error)
   {
      LOG_ERROR(error);
      s_trustStatus = TrustStatus::Unknown;
      return;
   }

   if (matchesDirectoryList(projectDir, trustedDirs))
   {
      s_trustStatus = TrustStatus::Trusted;
      return;
   }

   if (matchesDirectoryList(projectDir, untrustedDirs))
   {
      s_trustStatus = TrustStatus::Untrusted;
      return;
   }

   s_trustStatus = TrustStatus::Unknown;
}

bool shouldSuppressStartupFiles()
{
   return s_trustStatus == TrustStatus::Untrusted ||
          s_trustStatus == TrustStatus::Unknown;
}

bool shouldSuppressWorkspaceRestore()
{
   return s_trustStatus == TrustStatus::Untrusted ||
          s_trustStatus == TrustStatus::Unknown;
}

Error setTrust(const FilePath& directory, bool trusted)
{
   return addToTrustList(directory, trusted);
}

std::string explicitTrustSetting()
{
   json::Array trustedDirs, untrustedDirs;
   Error error = readTrustFile(&trustedDirs, &untrustedDirs);
   if (error)
   {
      LOG_ERROR(error);
      return "default";
   }

   if (matchesDirectoryList(s_projectDir, trustedDirs))
      return "trusted";

   if (matchesDirectoryList(s_projectDir, untrustedDirs))
      return "untrusted";

   return "default";
}

Error resetTrust()
{
   return removeFromBothLists(s_projectDir);
}

json::Object trustRequestData()
{
   json::Object data;
   if (s_trustStatus == TrustStatus::Unknown)
   {
      data["directory"] = s_projectDir.getAbsolutePath();

      json::Array riskyFiles;
      for (const std::string& filename : findRiskyFiles(s_projectDir))
         riskyFiles.push_back(filename);
      data["risky_files"] = riskyFiles;
   }
   return data;
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_trustGrant);
   RS_REGISTER_CALL_METHOD(rs_trustRevoke);
   RS_REGISTER_CALL_METHOD(rs_trustReset);
   RS_REGISTER_CALL_METHOD(rs_trustStatus);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "grant_trust", grantTrust))
      (bind(registerRpcMethod, "revoke_trust", revokeTrust))
      (bind(registerRpcMethod, "reset_trust", resetTrustRpc))
      (bind(sourceModuleRFile, "SessionTrust.R"));
   return initBlock.execute();
}

} // namespace trust
} // namespace modules
} // namespace session
} // namespace rstudio
