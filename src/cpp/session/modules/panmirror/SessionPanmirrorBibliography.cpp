/*
 * SessionPanmirrorBibliography.cpp
 *
 * Copyright (C) 2022 by RStudio, Inc.
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

#include "SessionPanmirrorBibliography.hpp"

#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/Hash.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/StringUtils.hpp>

#include <core/system/Process.hpp>

#include <session/SessionQuarto.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace bibliography {

namespace {

std::vector<FileInfo> projectBibliographies()
{
   std::vector<FilePath> projectBibs;
   auto config = quarto::quartoConfig();
   if (config.is_project)
   {
      FilePath projDir = module_context::resolveAliasedPath(config.project_dir);
      auto biblios = config.project_bibliographies;
      std::transform(biblios.begin(), biblios.end(), std::back_inserter(projectBibs),
                     [&projDir](const std::string& biblio) {
         return projDir.completeChildPath(biblio);
      });

   }
   else
   {
      projectBibs = module_context::bookdownBibliographies();
   }

   // convert to FileInfo
   std::vector<FileInfo> biblioFiles;
   std::transform(projectBibs.begin(),
                  projectBibs.end(),
                  std::back_inserter(biblioFiles),
                  toFileInfo);
   return biblioFiles;
}

const char * const kBiblioJson = "biblio.json";
const char * const kBiblioFiles = "biblio-files";
const char * const kBiblioRefBlock = "biblio-refblock";

Error writeString(const FilePath& filePath, const std::string& str)
{
   return writeStringToFile(filePath, str);
}

Error readString(const FilePath& filePath, std::string* pStr)
{
   return readStringFromFile(filePath, pStr);
}

bool isYAMLBibliography(const FilePath& biblioPath)
{
   return biblioPath.getExtensionLowerCase() == ".yaml" || biblioPath.getExtensionLowerCase() == ".yml";
}

bool isJSONBibliography(const FilePath& biblioPath)
{
   return biblioPath.getExtensionLowerCase() == ".json";
}



// using a pointer for the *in* argument so we can include this in an exec block
Error parseBiblio(const std::string* pBiblio, json::Value* pBiblioJson)
{
   if (pBiblioJson->isObject())
      pBiblioJson->getObject().clear();
   else if (pBiblioJson->isArray())
      pBiblioJson->getArray().clear();

   if (pBiblio->length() > 0)
      return pBiblioJson->parse(*pBiblio);
   else
      return Success();
}


// cache the last bibliography we returned (along w/ enough info to construct an etag for the cache)
class BiblioCache
{
public:
   static std::string etag(const std::vector<core::FileInfo>& biblioFiles,
                           const std::string& refBlock)
   {
      std::ostringstream ostr;
      for (auto file : biblioFiles)
          ostr << file.absolutePath() << ":" << file.lastWriteTime();
      if (refBlock.length() > 0)
         ostr << hash::crc32HexHash(refBlock);
      return ostr.str();
   }

public:
   void update(const json::Object& biblioJson,
               const std::vector<core::FileInfo>& biblioFiles,
               const std::string& refBlock)
   {
      // update members
      biblioJson_ = biblioJson;
      biblioFiles_ = biblioFiles;
      refBlock_ = refBlock;

      // attempt to write to storage
      Error error = writeToStorage();
      if (error)
         LOG_ERROR(error);
   }

   void clear()
   {
      biblioJson_.clear();
      biblioFiles_.clear();
      refBlock_.clear();
   }

   std::string etag()
   {
      return etag(biblioFiles_, refBlock_);
   } 

   bool isFor(const std::vector<core::FileInfo>& biblioFiles,
              const std::string& refBlock)
   {
      if (refBlock == refBlock_ && biblioFiles.size() == biblioFiles_.size())
      {
         return std::equal(biblioFiles.begin(), biblioFiles.end(), biblioFiles_.begin(),
           [](const FileInfo& a, const FileInfo& b)
            {
               return a.absolutePath() == b.absolutePath();
            }
         );
      }
      else
      {
         return false;
      }
   }

   void setResponse(json::JsonRpcResponse* pResponse)
   {
      json::Object result;
      result["etag"] = etag();
      result["bibliography"] = biblioJson_;
      pResponse->setResult(result);
   }

   Error readFromStorage()
   {
      clear();

      FilePath storageDir = storagePath();
      FilePath biblioJsonPath = storageDir.completeChildPath(kBiblioJson);
      FilePath biblioFilesPath = storageDir.completeChildPath(kBiblioFiles);
      FilePath refBlockPath = storageDir.completeChildPath(kBiblioRefBlock);
      if (biblioJsonPath.exists() && biblioFilesPath.exists() && refBlockPath.exists())
      {
         std::string biblio;
         std::vector<std::string> files;
         ExecBlock readBlock;
         readBlock.addFunctions()
            (boost::bind(readString, biblioJsonPath, &biblio))
            (boost::bind(parseBiblio, &biblio, &biblioJson_))
            (boost::bind(readStringVectorFromFile, biblioFilesPath, &files, true))
            (boost::bind(readBiblioFiles, &files, &biblioFiles_))
            (boost::bind(readString, refBlockPath, &refBlock_))
         ;
         Error error = readBlock.execute();
         if (error)
         {
            clear();
            return error;
         }
         else
         {
            return Success();
         }
      }
      else
      {
         return Success();
      }
   }

   Error writeToStorage()
   {
      FilePath storageDir = storagePath();
      FilePath biblioJsonPath = storageDir.completeChildPath(kBiblioJson);
      FilePath biblioFilesPath = storageDir.completeChildPath(kBiblioFiles);
      FilePath refBlockPath = storageDir.completeChildPath(kBiblioRefBlock);

      std::vector<std::string> files;
      std::transform(biblioFiles_.begin(), biblioFiles_.end(),
                     std::back_inserter(files),
                     boost::bind(&FileInfo::absolutePath, _1));

      ExecBlock writeBlock;
      writeBlock.addFunctions()
         (boost::bind(&FilePath::removeIfExists, &biblioJsonPath))
         (boost::bind(&FilePath::removeIfExists, &biblioFilesPath))
         (boost::bind(&FilePath::removeIfExists, &refBlockPath))
         (boost::bind(writeString, biblioJsonPath, biblioJson_.writeFormatted()))
         (boost::bind(writeBiblioFiles, biblioFilesPath, files))
         (boost::bind(writeString, refBlockPath, refBlock_))
      ;
      return writeBlock.execute();
   }


private:
   FilePath storagePath()
   {
      FilePath path = module_context::scopedScratchPath().completeChildPath("bibliography-index");
      Error error = path.ensureDirectory();
      if (error)
         LOG_ERROR(error);
      return path;
   }

   static Error writeBiblioFiles(const FilePath& filePath, const std::vector<std::string>& files)
   {
      std::vector<std::string> records;
      std::transform(files.begin(), files.end(), std::back_inserter(records),
         [](const std::string& file) {
            std::ostringstream ostr;
            ostr << FilePath(file).getLastWriteTime()
                 << ":"
                 << file;
            return ostr.str();
         }
      );
      return writeStringVectorToFile(filePath, records);
   }

    // using a pointer for the *in* argument so we can include this in an exec block
   static Error readBiblioFiles(const std::vector<std::string>* pRecords, std::vector<core::FileInfo>* pFileInfos)
   {
      pFileInfos->clear();
      for (auto record : *pRecords)
      {
         // parse record
         std::istringstream istr(record);
         std::time_t lastWriteTime;
         char colon;
         std::string file;
         istr >> lastWriteTime >> colon >> file;

         // create file info using recorded write time
         FilePath filePath(file);
         FileInfo fileInfo(file, filePath.isDirectory(), filePath.getSize(), lastWriteTime);
         pFileInfos->push_back(fileInfo);
      }

      return Success();
   }


private:
   json::Object biblioJson_;
   std::vector<core::FileInfo> biblioFiles_;
   std::string refBlock_;
};
BiblioCache s_biblioCache;


// global logging helper
void logBiblioStatus(const std::string& str)
{
   str.length(); // silience compiler on unused var
   // std::cerr << str << std::endl;
}

json::Object createBiblioJson(const json::Array& jsonCitations, bool project)
{
   // citations
   json::Object biblioJson;
   biblioJson["sources"] = jsonCitations;


   // project biblios
   json::Array projectBiblios;

   // quarto config
   auto quartoConfig = quarto::quartoConfig();
   if (quartoConfig.is_project)
   {
      projectBiblios = json::toJsonArray(quartoConfig.project_bibliographies);
   }
   else if (project)
   {
      projectBiblios = json::toJsonArray(module_context::bookdownBibliographiesRelative());
   }

   biblioJson["project_biblios"] = projectBiblios;

   return biblioJson;
}

void indexProjectCompleted(const std::vector<core::FileInfo>& biblioFiles,
                           const core::system::ProcessResult& result)
{
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Array jsonCitations;
      Error error = jsonCitations.parse(result.stdOut);
      if (!error)
      {
         // create bibliography
         json::Object biblioJson = createBiblioJson(jsonCitations, true);

         // cache it
         s_biblioCache.update(biblioJson, biblioFiles, "");

         // status
         logBiblioStatus("Indexed and updated project bibliography");
      }
      else
      {
         LOG_ERROR(error);
      }
   }
   else
   {
      Error error = systemError(boost::system::errc::state_not_recoverable, ERROR_LOCATION);
      error.addProperty("stderr", result.stdErr);
      LOG_ERROR(error);
   }
}

void bibliographiesToCslJson(
  bool isProjectFile,
  const std::vector<core::FileInfo>& biblioFiles,
  const std::string& refBlock,
  std::vector<core::FileInfo> biblioQueue,
  json::Array cslJson,
  const json::JsonRpcFunctionContinuation& cont,
  const boost::optional<core::system::ProcessResult> result = boost::none
) {

   // process any result we receive
   if (result.has_value())
   {
      json::JsonRpcResponse response;
      if (result.value().exitStatus == EXIT_SUCCESS)
      {
         json::Array jsonCitations;
         if (json::parseJsonForResponse(result.value().stdOut, &jsonCitations, &response))
            std::copy(jsonCitations.begin(), jsonCitations.end(), std::back_inserter(cslJson));
      }
      else
      {
         LOG_ERROR_MESSAGE("Error converting to csjson: " + result.value().stdErr);
         json::setProcessErrorResponse(result.value(), ERROR_LOCATION, &response);
         cont(Success(), &response);
         return;
      }
   }

   // get next target
   if (biblioQueue.size() > 0)
   {
      FileInfo biblioFile = biblioQueue.front();
      biblioQueue.erase(biblioQueue.begin());
      if (FilePath::exists(biblioFile.absolutePath()))
      {
         std::vector<std::string> args;
         args.push_back(string_utils::utf8ToSystem(biblioFile.absolutePath()));
         args.push_back("--standalone");
         if (isYAMLBibliography(FilePath(biblioFile.absolutePath())))
         {
            args.push_back("--from");
            args.push_back("markdown");
         }
         else if (isJSONBibliography(FilePath(biblioFile.absolutePath())))
         {
            args.push_back("--from");
            args.push_back("csljson");
         }
         args.push_back("--to");
         args.push_back("csljson");

         // run pandoc and call ourselves back when done
         Error error = module_context::runPandocAsync(
            args, "", boost::bind(bibliographiesToCslJson, isProjectFile, biblioFiles, refBlock, biblioQueue, cslJson, cont, _1)
         );
         if (error)
         {
            json::JsonRpcResponse response;
            json::setErrorResponse(error, &response);
            cont(Success(), &response);
            return;
         }
      }
      else
      {
         // bibliography didn't exist, call ourselves back w/ updated queue
         bibliographiesToCslJson(
            isProjectFile,
            biblioFiles,
            refBlock,
            biblioQueue,
            cslJson,
            cont
         );
      }
   }

   // queue is empty so we are done, cache and return the csl json bibliography
   else
   {
      // create bibliography
      json::Object biblioJson = createBiblioJson(cslJson, isProjectFile);

      // cache last successful bibliograpy
      s_biblioCache.update(biblioJson, biblioFiles, refBlock);

      // status
      logBiblioStatus("Cached getBibliography response");

      // set response
      json::JsonRpcResponse response;
      s_biblioCache.setResponse(&response);
      cont(Success(), &response);
   }

}


void pandocGetBibliography(const json::JsonRpcRequest& request,
                           const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract params
   std::string file, refBlock, etag;
   json::Array bibliographiesJson;
   Error error = json::readParams(request.params, &file, &bibliographiesJson, &refBlock, &etag);
   if (error)
   {
      json::setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
   }

   // determine biblio files
   std::vector<FileInfo> biblioFiles;

   // determine whether the file the bibliography is requested for is part of the current project
   bool isProjectFile = false;
   if (!file.empty())
   {
      FilePath filePath = module_context::resolveAliasedPath(file);
      if (module_context::isBookdownProject())
      {
         isProjectFile = filePath.isWithin(projects::projectContext().buildTargetPath());
      }
      else
      {
         isProjectFile = quarto::isFileInSessionQuartoProject(filePath);
      }
   }

   // if there are bibliographies passed from the client then use those in preference to the
   // project bibliographies (b/c they will appear after the project bibliographies)
   if (bibliographiesJson.getSize() > 0)
   {
      std::vector<std::string> biblios;
      bibliographiesJson.toVectorString(biblios);
      for (auto biblio : biblios)
      {
         FilePath biblioPath = module_context::resolveAliasedPath(biblio);
         biblioFiles.push_back(FileInfo(biblioPath));
      }
   }
   // is this file part of the current project? if so then use the project bibliographies as the default
   else if (isProjectFile)
   {
      biblioFiles = projectBibliographies();
   }


   // filter biblio files on existence
   algorithm::expel_if(biblioFiles, [](const FileInfo& file) { return !FilePath::exists(file.absolutePath()); });

   // if the filesystem and the cache agree on the etag then we can serve from cache
   if (s_biblioCache.etag() == BiblioCache::etag(biblioFiles, refBlock))
   {
      // if the client side cache agrees w/ s_biblioCache then return just the etag
      // (indicating that the client already has up to date data)
      if (etag.length() > 0 && etag == s_biblioCache.etag())
      {
         // set result to just the etag
         json::Object result;
         result["etag"] = etag;
         response.setResult(result);

         // status
         logBiblioStatus("Resolved getBibliography from cache (CLIENT)");
      }
      else
      {
         // set result from cache
         s_biblioCache.setResponse(&response);

         // status
         logBiblioStatus("Resolved getBibliography from cache");
      }


      cont(Success(), &response);
      return;
   }

   // create queue from biblio files and refBlock
   std::vector<FileInfo> biblioQueue = biblioFiles;
   if (!refBlock.empty())
   {
      FilePath refBlockYaml = module_context::tempFile("biblio", "yaml");
      Error error = writeStringToFile(refBlockYaml, refBlock);
      if (error)
      {
         LOG_ERROR(error);
      }
      else
      {
         FileInfo targetBiblio = FileInfo(refBlockYaml);
         biblioQueue.push_back(targetBiblio);
      }
   }

   // process the queue
   if (biblioQueue.size() > 0)
   {
      bibliographiesToCslJson(
         isProjectFile,
         biblioFiles,
         refBlock,
         biblioQueue,
         json::Array(),
         cont
      );
   }
   else
   {
      s_biblioCache.update(createBiblioJson(json::Array(), isProjectFile), biblioFiles, refBlock);
      s_biblioCache.setResponse(&response);
      cont(Success(), &response);
   }
}

Error pandocGenerateBibliography(const std::string& biblioJson,
                                 const std::vector<std::string>& formatArgs,
                                 std::string* pBiblio)
{
   // write the json to a temp file
   FilePath jsonBiblioPath = module_context::tempFile("biblio", "json");
   Error error = core::writeStringToFile(jsonBiblioPath, biblioJson);
   if (error)
      return error;

   // run pandoc
   std::vector<std::string> args;
   args.push_back(string_utils::utf8ToSystem(jsonBiblioPath.getAbsolutePath()));
   args.push_back("--from");
   args.push_back("csljson");
   std::copy(formatArgs.begin(), formatArgs.end(), std::back_inserter(args));
   core::system::ProcessResult result;
   error = module_context::runPandoc(args, "", &result);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      Error error = systemError(boost::system::errc::state_not_recoverable, result.stdErr, ERROR_LOCATION);
      error.addProperty("biblio-json", biblioJson);
      LOG_ERROR(error);
      return error;
   }
   else
   {
      *pBiblio = result.stdOut;
      return Success();
   }
}

Error pandocGenerateBibliography(const std::string& biblioJson,
                                 const FilePath& cslPath,
                                 const std::vector<std::string>& extraArgs,
                                 std::string* pBiblio)
{
   // write the json to a temp file
   FilePath jsonBiblioPath = module_context::tempFile("biblio", "json");
   Error error = core::writeStringToFile(jsonBiblioPath, biblioJson);
   if (error)
      return error;

   // optional csl
   std::string csl;
   if (!cslPath.isEmpty())
   {
      boost::format fmt("\ncsl: \"%1%\"");
      csl = boost::str(fmt % string_utils::utf8ToSystem(cslPath.getAbsolutePath()));
   }

   // create a document
   boost::format fmt("---\nbibliography: \"%1%\"%2%\nnocite: |\n  @*\n---\n");
   std::string doc = boost::str(fmt %
     string_utils::utf8ToSystem(jsonBiblioPath.getAbsolutePath()) %
     csl
   );

   // run pandoc with citeproc
   std::vector<std::string> args;
   args.push_back("--from");
   args.push_back("markdown");
   args.push_back("--citeproc");
   std::copy(extraArgs.begin(), extraArgs.end(), std::back_inserter(args));

   core::system::ProcessResult result;
   error = module_context::runPandoc(args, doc, &result);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      Error error = systemError(boost::system::errc::state_not_recoverable, result.stdErr, ERROR_LOCATION);
      error.addProperty("biblio-json", biblioJson);
      LOG_ERROR(error);
      return error;
   }
   else
   {
      *pBiblio = result.stdOut;
      return Success();
   }
}


std::string stripYAMLEnvelope(const std::string& yaml)
{
   boost::regex regex("^---.*?\nreferences:([\\S\\s]+)(\\.\\.\\.|---)\\s*$");
   boost::smatch match;
   if (boost::regex_search(yaml, match, regex))
   {
      return boost::algorithm::trim_copy(std::string(match[1]));
   }
   else
   {
      return yaml;
   }
}

Error appendToYAMLBibliography(const FilePath& bibliographyFile, const std::string& id, const std::string& biblio)
{
   // read the existing bibio (if it exists)
   std::string biblioFileContents;
   if (bibliographyFile.exists())
   {
      Error error = readString(bibliographyFile, &biblioFileContents);
      if (error)
         return error;
   }

   // strip the yaml envelope from both biblios
   std::string biblioFile = stripYAMLEnvelope(biblioFileContents);
   if (!biblioFile.empty())
      biblioFile += "\n";
   std::string biblioAppend = stripYAMLEnvelope(biblio);

   // add the id to the biblio
   biblioAppend = boost::algorithm::replace_first_copy(biblioAppend, "- ", "- id: " + id + "\n  ");

   // write the biblio
   boost::format fmt("---\nreferences:\n%1%%2%\n...\n");
   return core::writeStringToFile(bibliographyFile, boost::str(fmt % biblioFile % biblioAppend), string_utils::LineEndingPosix);
}

Error appendToJSONBibliography(const FilePath& bibliographyFile, const std::string& id, const std::string& biblio)
{
   // read the existing bibio (if it exists)
   std::string biblioFileContents;
   json::Array biblioFileJson;
   if (bibliographyFile.exists())
   {
      Error error = readString(bibliographyFile, &biblioFileContents);
      if (error)
         return error;
      error = parseBiblio(&biblioFileContents, &biblioFileJson);
      if (error)
         return error;
   }

   // parse the passed biblio and apply the id
   json::Array biblioJson;
   Error error = parseBiblio(&biblio, &biblioJson);
   if (error)
      return error;
   if (biblioJson.getSize() > 0)
   {
      json::Object entryJson;
      for (json::Object::Member member : biblioJson[0].getObject())
         entryJson[member.getName()] = member.getValue();
      entryJson["id"] = id;
      biblioJson[0] = entryJson;
   }

   // append
   std::copy(biblioJson.begin(), biblioJson.end(), std::back_inserter(biblioFileJson));

   // write
   std::string newBiblio = biblioFileJson.writeFormatted();
   return writeStringToFile(bibliographyFile, newBiblio, string_utils::LineEndingPosix);
}

Error pandocAddToBibliography(const json::JsonRpcRequest& request, json::JsonRpcResponse* pResponse)
{
   // extract params
   std::string bibliography, id, sourceAsJson, sourceAsBibTeX;
   bool project;
   Error error = json::readParams(request.params,
                                  &bibliography,
                                  &project,
                                  &id,
                                  &sourceAsJson,
                                  &sourceAsBibTeX);
   if (error)
      return error;

   // resolve the bibliography path
   FilePath bibliographyPath;
   if (project && projects::projectContext().hasProject())
   {
      auto quartoConfig = quarto::quartoConfig();
      if (quartoConfig.is_project)
      {
         FilePath projDir = module_context::resolveAliasedPath(quartoConfig.project_dir);
         bibliographyPath = projDir.completeChildPath(bibliography);
      }
      else
      {
         bibliographyPath = projects::projectContext().buildTargetPath().completeChildPath(bibliography);
      }
   }
   else
   {
      bibliographyPath = module_context::resolveAliasedPath(bibliography);
   }


   // yaml or json target
   bool isYAML = isYAMLBibliography(bibliographyPath);
   bool isJSON = isJSONBibliography(bibliographyPath);
   if (isYAML || isJSON)
   {
      std::vector<std::string> formatArgs;
      formatArgs.push_back("--standalone");
      formatArgs.push_back("--to");
      if (isYAML)
         formatArgs.push_back("markdown");
      else
        formatArgs.push_back("csljson");
      std::string biblio;
      Error error = pandocGenerateBibliography(sourceAsJson, formatArgs, &biblio);
      if (error)
         return error;

      error = isYAML
         ? appendToYAMLBibliography(bibliographyPath, id, biblio)
         : appendToJSONBibliography(bibliographyPath, id, biblio);
      if (error)
         return error;
   }
   else
   {
      if (sourceAsBibTeX.length() > 0)
      {
         error = core::writeStringToFile(bibliographyPath, "\n" + sourceAsBibTeX + "\n",
                                         string_utils::LineEndingPosix, false);
         if (error)
            return error;
      }
      else
      {
         // get the path to the bibtex csl
         // Summary of bibtex types, fields, and so on
         // http://texdoc.net/texmf-dist/doc/bibtex/tamethebeast/ttb_en.pdf
         FilePath cslPath =
             session::options().rResourcesPath().completePath("bibtex.csl");

         std::vector<std::string> args;
         args.push_back("--to");
         args.push_back("plain");
         args.push_back("--wrap");
         args.push_back("none");
         std::string biblio;
         error =
             pandocGenerateBibliography(sourceAsJson, cslPath, args, &biblio);
         if (error)
            return error;

         // substitute the id
         const char* const kIdToken = "F3CCCD24-5C50-412A-AE47-549C9D147498";
         std::string entry = biblio;
         boost::algorithm::trim(entry);
         boost::replace_all(entry, kIdToken, id);

         // apply indentation
         std::vector<std::string> lines;
         boost::algorithm::split(lines, entry,
                                 boost::algorithm::is_any_of("\n"));
         std::vector<std::string> indentedLines;
         for (std::size_t i = 0; i < lines.size(); i++)
         {
            bool firstLine = i == 0;
            bool lastLine = i == lines.size() - 1;
            if (firstLine || lastLine)
               indentedLines.push_back(lines[i]);
            else
               indentedLines.push_back("  " + lines[i]);
         }
         entry = boost::algorithm::join(indentedLines, "\n");

         // append the bibliography to the file
         error = core::writeStringToFile(bibliographyPath, "\n" + entry + "\n",
                                         string_utils::LineEndingPosix, false);
         if (error)
            return error;
      }
   }

   pResponse->setResult(true);
   return Success();
}


Error pandocCitationHTML(const json::JsonRpcRequest& request, json::JsonRpcResponse* pResponse)
{
   // extract params
   std::string file, sourceAsJson, csl;
   Error error = json::readParams(request.params, &file, &sourceAsJson, &csl);
   if (error)
      return error;

   // resolve the file and csl paths (if any)
   FilePath filePath = !file.empty() ? module_context::resolveAliasedPath(file): FilePath();
   FilePath cslPath = (!csl.empty() && !filePath.isEmpty()) ? filePath.getParent().completePath(csl) : FilePath();

   // if there is no csl specified and a file is specified, see if we can resolve csl from the project
   if (cslPath.isEmpty() && !filePath.isEmpty() &&
       projects::projectContext().hasProject() &&
       filePath.isWithin(projects::projectContext().buildTargetPath()))
   {
      cslPath = module_context::bookdownCSL();
   }

   std::vector<std::string> args;
   args.push_back("--to");
   args.push_back("html");
   std::string biblio;
   error = pandocGenerateBibliography(sourceAsJson, cslPath, args, &biblio);
   if (error)
      return error;

   pResponse->setResult(biblio);

   return Success();
}



void updateProjectBibliography()
{
   std::vector<FileInfo> biblioFiles = projectBibliographies();
   std::vector<std::string> args;
   for (auto biblioFile : biblioFiles)
   {
      if (FilePath::exists(biblioFile.absolutePath()))
         args.push_back(string_utils::utf8ToSystem(biblioFile.absolutePath()));
   }
   args.push_back("--standalone");
   args.push_back("--to");
   args.push_back("csljson");
   Error error = module_context::runPandocAsync(args, "", boost::bind(indexProjectCompleted, biblioFiles, _1));
   if (error)
      LOG_ERROR(error);
}

void onCheckForBiblioChange(const std::vector<FileInfo>& biblioFiles,
                            const std::vector<core::system::FileChangeEvent>& changes)
{
   for (const core::system::FileChangeEvent& fileChange : changes)
   {
      bool detectedChange = false;
      for (auto biblioFile : biblioFiles)
      {
         if (biblioFile.absolutePath() == fileChange.fileInfo().absolutePath())
         {
            detectedChange = true;
            if (s_biblioCache.isFor(biblioFiles, ""))
            {
               logBiblioStatus("Updating biblio for file change event");
               updateProjectBibliography();
            }
            break;
         }
      }
      if (detectedChange)
         break;
   }
}

void onDeferredInit(bool)
{
   // read index from storage
   Error error = s_biblioCache.readFromStorage();
   if (error)
      LOG_ERROR(error);

   // if we have a project level bibliography then index it proactively (if we haven't already)
   if (projects::projectContext().hasProject())
   {
      std::vector<FileInfo> biblioFiles = projectBibliographies();
      if (biblioFiles.size() > 0)
      {
         // update the project bibliography cache if we need to
         if (s_biblioCache.etag() != BiblioCache::etag(biblioFiles, ""))
            updateProjectBibliography();

         // monitor the filesystem to do further updates
         session::projects::FileMonitorCallbacks cb;
         cb.onFilesChanged = boost::bind(onCheckForBiblioChange, biblioFiles, _1);
         projects::projectContext().subscribeToFileMonitor("Bibliography", cb);
      }
   }
}

} // end anonymous namespace

Error initialize()
{   
   module_context::events().onDeferredInit.connect(onDeferredInit);

   ExecBlock initBlock;
   initBlock.addFunctions()
        (boost::bind(module_context::registerAsyncRpcMethod, "pandoc_get_bibliography", pandocGetBibliography))
        (boost::bind(module_context::registerRpcMethod, "pandoc_add_to_bibliography", pandocAddToBibliography))
        (boost::bind(module_context::registerRpcMethod, "pandoc_citation_html", pandocCitationHTML))
   ;
   return initBlock.execute();
}


} // end namespace bibliography
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
