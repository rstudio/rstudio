/*
 * SessionQuarto.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#ifndef SESSION_QUARTO_HPP
#define SESSION_QUARTO_HPP

#include <string>
#include <vector>

#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
class Error;
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace session {
namespace quarto {

extern const char* const kQuartoCrossrefScope;
extern const char* const kQuartoProjectDefault;
extern const char* const kQuartoProjectWebsite;
extern const char* const kQuartoProjectSite;
extern const char* const kQuartoProjectBook;
extern const char* const kQuartoExecuteDirProject;
extern const char* const kQuartoExecuteDirFile;

struct QuartoConfig
{
   QuartoConfig() : enabled(false), is_project(false) {}

   // is quarto enabled?
   bool enabled;

   // is there a user installed version?
   core::FilePath userInstalled;

   // active version info
   std::string version;
   std::string bin_path;
   std::string resources_path;

   // project info
   bool is_project;
   std::string project_type;
   std::string project_dir;
   std::string project_output_dir;
   std::string project_execute_dir;
   std::vector<std::string> project_formats;
   std::vector<std::string> project_bibliographies;
   core::json::Object project_editor;
};

QuartoConfig quartoConfig(bool refresh = false);

core::Error quartoInspect(const std::string& path,
                          core::json::Object *pResultObject);

core::json::Object quartoConfigJSON(bool refresh = false);

core::json::Value quartoCapabilities();

// see if quarto wants to handle the preview
bool handleQuartoPreview(const core::FilePath& sourceFile,
                         const core::FilePath& outputFile,
                         const std::string& renderOutput,
                         bool validateExtendedType);

std::string quartoDefaultFormat(const core::FilePath& sourceFile);

bool isFileInSessionQuartoProject(const core::FilePath& file);
std::string urlPathForQuartoProjectOutputFile(const core::FilePath& outputFile);

// NOTE: Prefer using 'quartoExecutablePath()' when running quarto
// as a program / command, as this will use a Windows-safe short
// path and so avoid issues with paths containing spaces in some contexts
// https://github.com/rstudio/rstudio/issues/11779
core::FilePath quartoBinary();
std::string quartoExecutablePath();

bool projectIsQuarto();

bool docIsQuarto(const std::string& docId);

core::FilePath quartoProjectConfigFile(const core::FilePath& filePath);

void readQuartoProjectConfig(const core::FilePath& configFile,
                             std::string* pType,
                             std::string* pOutputDir = nullptr,
                             std::string* pExecuteDir = nullptr,
                             std::vector<std::string>* pFormats = nullptr,
                             std::vector<std::string>* pBibliographies = nullptr,
                             core::json::Object* pEditor = nullptr);

core::json::Value quartoXRefIndex();

core::FilePath getQuartoExecutionDir(const std::string& docPath);

} // namespace quarto
} // namespace session
} // namespace rstudio

#endif /* SESSION_QUARTO_HPP */
