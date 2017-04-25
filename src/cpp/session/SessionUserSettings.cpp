/*
 * SessionUserSettings.cpp
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

#include <session/SessionUserSettings.hpp>

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include <core/r_util/RSessionContext.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionConstants.hpp>
#include <session/projects/ProjectsSettings.hpp>
#include <session/projects/SessionProjects.hpp>
#include "modules/SessionErrors.hpp"
#include "modules/SessionShinyViewer.hpp"

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RConsoleHistory.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace session {  
   
#define kAgreementPrefix "agreement."
   
namespace {
const char * const kContextId = kContextIdentifier;
const char * const kAgreementHash = kAgreementPrefix "agreedToHash";
const char * const kAutoCreatedProfile = "autoCreatedProfile";
const char * const kUiPrefs = "uiPrefs";
const char * const kRProfileOnResume = "rprofileOnResume";
const char * const kSaveAction = "saveAction";
const char * const kLoadRData = "loadRData";
const char * const kInitialWorkingDirectory = "initialWorkingDirectory";
const char * const kCRANMirrorName = "cranMirrorName";
const char * const kCRANMirrorHost = "cranMirrorHost";
const char * const kCRANMirrorUrl = "cranMirrorUrl";
const char * const kCRANMirrorCountry = "cranMirrorCountry";
const char * const kBioconductorMirrorName = "bioconductorMirrorName";
const char * const kBioconductorMirrorUrl = "bioconductorMirrorUrl";
const char * const kAlwaysSaveHistory = "alwaysSaveHistory";
const char * const kRemoveHistoryDuplicates = "removeHistoryDuplicates";
const char * const kLineEndings = "lineEndingConversion";
const char * const kUseNewlineInMakefiles = "newlineInMakefiles";
const char * const kDefaultTerminalShell = "defaultTerminalShell";

template <typename T>
T readPref(const json::Object& prefs,
           const std::string& name,
           const T& defaultValue)
{
   T value = defaultValue;
   Error error = json::readObject(prefs,
                                  name,
                                  defaultValue,
                                  &value);
   if (error)
   {
      value = defaultValue;
      error.addProperty("pref", name);
      LOG_ERROR(error);
   }

   return value;
}

void setCRANReposOption(const std::string& url)
{
   if (!url.empty())
   {
      Error error = r::exec::RFunction(".rs.setCRANReposFromSettings",
                                       url).call();
      if (error)
         LOG_ERROR(error);
   }
}

void setBioconductorReposOption(const std::string& mirror)
{
   if (!mirror.empty())
   {
      Error error = r::options::setOption("BioC_mirror", mirror);
      if (error)
         LOG_ERROR(error);
   }
}


SEXP rs_readUiPref(SEXP prefName)
{
   r::sexp::Protect protect;

   // extract name of preference to read
   std::string pref = r::sexp::safeAsString(prefName, "");
   if (pref.empty())
      return R_NilValue;

   json::Value prefValue = json::Value();

   // try project overrides first
   if (projects::projectContext().hasProject())
   {
      json::Object uiPrefs = projects::projectContext().uiPrefs();
      json::Object::iterator it = uiPrefs.find(pref);
      if (it != uiPrefs.end())
         prefValue = it->second;
   }

   // then try global UI prefs
   if (prefValue.is_null())
   {
      json::Object uiPrefs = userSettings().uiPrefs();
      json::Object::iterator it = uiPrefs.find(pref);
      if (it != uiPrefs.end())
         prefValue = it->second;
   }

   // convert to SEXP and return
   return r::sexp::create(prefValue, &protect);
}

SEXP rs_writeUiPref(SEXP prefName, SEXP value)
{
   json::Value prefValue = json::Value();

   // extract name of preference to write
   std::string pref = r::sexp::safeAsString(prefName, "");
   if (pref.empty())
      return R_NilValue;

   // extract value to write
   Error error = r::json::jsonValueFromObject(value, &prefValue);
   if (error)
   {
      r::exec::error("Unexpected value: " + error.summary());
      return R_NilValue;
   }

   // if this corresponds to an existing preference, ensure that we're not 
   // changing its data type
   json::Object uiPrefs = userSettings().uiPrefs();
   json::Object::iterator it = uiPrefs.find(pref);
   if (it != uiPrefs.end())
   {
      if (it->second.type() != prefValue.type())
      {
         r::exec::error("Type mismatch: expected " + 
                  json::typeAsString(it->second.type()) + "; got " + 
                  json::typeAsString(prefValue.type()));
         return R_NilValue;
      }
   }
   
   // write new pref value
   uiPrefs[pref] = prefValue;
   userSettings().setUiPrefs(uiPrefs);

   return R_NilValue;
}

} // anonymous namespace
   
UserSettings& userSettings()
{
   static UserSettings instance ;
   return instance;
}
   
Error UserSettings::initialize()
{
   // calculate settings file path
   FilePath settingsDir = module_context::registerMonitoredUserScratchDir(
              kUserSettingsDir,
              boost::bind(&UserSettings::onSettingsFileChanged, this, _1));
   settingsFilePath_ = settingsDir.complete(kUserSettingsFile);

   // if it doesn't exist see if we can migrate an old user settings
   if (!settingsFilePath_.exists())
   {
      FilePath oldSettingsPath =
            module_context::userScratchPath().complete("user-settings");
      if (oldSettingsPath.exists())
         oldSettingsPath.move(settingsFilePath_);
   }

   // read the settings
   Error error = settings_.initialize(settingsFilePath_);
   if (error)
      return error;

   // register routines for reading/writing UI prefs from R code
   R_CallMethodDef readUiPrefMethodDef ;
   readUiPrefMethodDef.name = "rs_readUiPref";
   readUiPrefMethodDef.fun = (DL_FUNC) rs_readUiPref;
   readUiPrefMethodDef.numArgs = 1;
   r::routines::addCallMethod(readUiPrefMethodDef);

   R_CallMethodDef writeUiPrefMethodDef ;
   writeUiPrefMethodDef.name = "rs_writeUiPref";
   writeUiPrefMethodDef.fun = (DL_FUNC) rs_writeUiPref;
   writeUiPrefMethodDef.numArgs = 2;
   r::routines::addCallMethod(writeUiPrefMethodDef);

   // make sure we have a context id
   if (contextId().empty())
      setContextId(core::system::generateShortenedUuid());

   return Success();
}

void UserSettings::onSettingsFileChanged(
                     const core::system::FileChangeEvent& changeEvent)
{
   // ensure this is for our target file
   if (settingsFilePath_.absolutePath() !=
       changeEvent.fileInfo().absolutePath())
   {
      return;
   }

   // re-read the settings from disk
   Error error = settings_.initialize(settingsFilePath_);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // update prefs cache
   updatePrefsCache(uiPrefs());

   // set underlying R repos options
   std::string cranMirrorURL = cranMirror().url;
   if (!cranMirrorURL.empty())
      setCRANReposOption(cranMirrorURL);
   std::string bioconductorMirrorURL = settings_.get(kBioconductorMirrorUrl);
   if (!bioconductorMirrorURL.empty())
      setBioconductorReposOption(bioconductorMirrorURL);

   // update remove dups in underlying R session
   using namespace rstudio::r::session;
   consoleHistory().setRemoveDuplicates(removeHistoryDuplicates());

   // fire event so others can react appropriately
   onChanged();
}


std::string UserSettings::contextId() const
{
   return settings_.get(kContextId);
}

std::string UserSettings::oldContextId() const
{
   return settings_.get("contextId");
}

void UserSettings::setContextId(const std::string& contextId)
{
   settings_.set(kContextId, contextId);
}

std::string UserSettings::agreementHash() const
{
   return settings_.get(kAgreementHash, "");
}
   
void UserSettings::setAgreementHash(const std::string& hash)
{
   settings_.set(kAgreementHash, hash);
}

  
bool UserSettings::autoCreatedProfile() const
{
   return settings_.getBool(kAutoCreatedProfile, false);
}
   
void UserSettings::setAutoCreatedProfile(bool autoCreated)
{
   settings_.set(kAutoCreatedProfile, autoCreated);
}

core::json::Object UserSettings::uiPrefs() const
{
   std::string value = settings_.get(kUiPrefs, "{}");
   json::Value jsonValue;
   bool success = core::json::parse(value, &jsonValue);
   if (success)
      return jsonValue.get_obj();
   else
      return json::Object();
}

void UserSettings::setUiPrefs(const core::json::Object& prefsObject)
{
   std::ostringstream output;
   json::writeFormatted(prefsObject, output);
   settings_.set(kUiPrefs, output.str());

   updatePrefsCache(prefsObject);
}

void UserSettings::updatePrefsCache(const json::Object& prefs) const
{ 
   bool useSpacesForTab = readPref<bool>(prefs, "use_spaces_for_tab", true);
   pUseSpacesForTab_.reset(new bool(useSpacesForTab));

   int numSpacesForTab = readPref<int>(prefs, "num_spaces_for_tab", 2);
   pNumSpacesForTab_.reset(new int(numSpacesForTab));

   bool autoAppendNewline = readPref<bool>(prefs, "auto_append_newline", false);
   pAutoAppendNewline_.reset(new bool(autoAppendNewline));

   bool stripTrailingWhitespace = readPref<bool>(prefs, "strip_trailing_whitespace", false);
   pStripTrailingWhitespace_.reset(new bool(stripTrailingWhitespace));

   std::string enc = readPref<std::string>(prefs, "default_encoding", "");
   pDefaultEncoding_.reset(new std::string(enc));

   std::string sweave = readPref<std::string>(prefs, "default_sweave_engine", "Sweave");
   pDefaultSweaveEngine_.reset(new std::string(sweave));

   std::string latex = readPref<std::string>(prefs, "default_latex_program", "pdfLaTeX");
   pDefaultLatexProgram_.reset(new std::string(latex));

   bool alwaysEnableRnwConcordance = readPref<bool>(prefs, "always_enable_concordance", true);
   pAlwaysEnableRnwConcordance_.reset(new bool(alwaysEnableRnwConcordance));

   std::string spellingLanguage = readPref<std::string>(prefs, "spelling_dictionary_language", "en_US");
   pSpellingLanguage_.reset(new std::string(spellingLanguage));

   json::Array spellingCustomDicts = readPref<core::json::Array>(prefs, "spelling_custom_dictionaries", core::json::Array());
   pSpellingCustomDicts_.reset(new json::Array(spellingCustomDicts));

   bool handleErrorsInUserCodeOnly = readPref<bool>(prefs, "handle_errors_in_user_code_only", true);
   pHandleErrorsInUserCodeOnly_.reset(new bool(handleErrorsInUserCodeOnly));

   int shinyViewerType = readPref<int>(prefs, "shiny_viewer_type", modules::shiny_viewer::SHINY_VIEWER_WINDOW);
   pShinyViewerType_.reset(new int(shinyViewerType));

   bool enableRSConnectUI = readPref<bool>(prefs, "enable_rstudio_connect", false);
   pEnableRSConnectUI_.reset(new bool(enableRSConnectUI));

   bool lintRFunctionCalls = readPref<bool>(prefs, "diagnostics_in_function_calls", true);
   pLintRFunctionCalls_.reset(new bool(lintRFunctionCalls));
   
   bool checkArgumentsToRFunctionCalls = readPref<bool>(prefs, "check_arguments_to_r_function_calls", false);
   pCheckArgumentsToRFunctionCalls_.reset(new bool(checkArgumentsToRFunctionCalls));

   bool warnIfNoSuchVariableInScope = readPref<bool>(prefs, "warn_if_no_such_variable_in_scope", false);
   pWarnIfNoSuchVariableInScope_.reset(new bool(warnIfNoSuchVariableInScope));

   bool warnIfVariableDefinedButNotUsed = readPref<bool>(prefs, "warn_if_variable_defined_but_not_used", false);
   pWarnIfVariableDefinedButNotUsed_.reset(new bool(warnIfVariableDefinedButNotUsed));

   bool enableStyleDiagnostics = readPref<bool>(prefs, "enable_style_diagnostics", false);
   pEnableStyleDiagnostics_.reset(new bool(enableStyleDiagnostics));

   int ansiConsoleMode = readPref<int>(prefs, "ansi_console_mode", core::text::AnsiColorOn);
   pAnsiConsoleMode_.reset(new int(ansiConsoleMode));
}


// readers for ui prefs

bool UserSettings::useSpacesForTab() const
{
   return readUiPref<bool>(pUseSpacesForTab_);
}

int UserSettings::numSpacesForTab() const
{
   return readUiPref<int>(pNumSpacesForTab_);
}

bool UserSettings::autoAppendNewline() const
{
   return readUiPref<bool>(pAutoAppendNewline_);
}

bool UserSettings::stripTrailingWhitespace() const
{
   return readUiPref<bool>(pStripTrailingWhitespace_);
}

std::string UserSettings::defaultEncoding() const
{
   return readUiPref<std::string>(pDefaultEncoding_);
}

std::string UserSettings::defaultSweaveEngine() const
{
   return readUiPref<std::string>(pDefaultSweaveEngine_);
}

std::string UserSettings::defaultLatexProgram() const
{
   return readUiPref<std::string>(pDefaultLatexProgram_);
}

bool UserSettings::alwaysEnableRnwCorcordance() const
{
   return readUiPref<bool>(pAlwaysEnableRnwConcordance_);
}

std::string UserSettings::spellingLanguage() const
{
   return readUiPref<std::string>(pSpellingLanguage_);
}

bool UserSettings::handleErrorsInUserCodeOnly() const
{
   return readUiPref<bool>(pHandleErrorsInUserCodeOnly_);
}

int UserSettings::shinyViewerType() const
{
   return readUiPref<int>(pShinyViewerType_);
}

bool UserSettings::enableRSConnectUI() const
{
   return readUiPref<bool>(pEnableRSConnectUI_);
}

bool UserSettings::lintRFunctionCalls() const
{
   return readUiPref<bool>(pLintRFunctionCalls_);
}

bool UserSettings::checkArgumentsToRFunctionCalls() const
{
   return readUiPref<bool>(pCheckArgumentsToRFunctionCalls_);
}

bool UserSettings::warnIfNoSuchVariableInScope() const
{
   return readUiPref<bool>(pWarnIfNoSuchVariableInScope_);
}

bool UserSettings::warnIfVariableDefinedButNotUsed() const
{
   return readUiPref<bool>(pWarnIfVariableDefinedButNotUsed_);
}

bool UserSettings::enableStyleDiagnostics() const
{
   return readUiPref<bool>(pEnableStyleDiagnostics_);
}

std::vector<std::string> UserSettings::spellingCustomDictionaries() const
{
   json::Array dictsJson = readUiPref<json::Array>(pSpellingCustomDicts_);
   std::vector<std::string> dicts;
   BOOST_FOREACH(const json::Value& dictJson, dictsJson)
   {
      if (json::isType<std::string>(dictJson))
         dicts.push_back(dictJson.get_str());
   }
   return dicts;
}


bool UserSettings::alwaysRestoreLastProject() const
{
   return settings_.getBool(kAlwaysRestoreLastProject, true);
}

void UserSettings::setAlwaysRestoreLastProject(bool alwaysRestore)
{
   settings_.set(kAlwaysRestoreLastProject, alwaysRestore);
}

bool UserSettings::rProfileOnResume() const
{
   return settings_.getBool(kRProfileOnResume,
                            session::options().rProfileOnResumeDefault());
}

void UserSettings::setRprofileOnResume(bool rProfileOnResume)
{
   settings_.set(kRProfileOnResume, rProfileOnResume);
}

int UserSettings::saveAction() const
{   
   return settings_.getInt(kSaveAction,
                           session::options().saveActionDefault());
}

void UserSettings::setSaveAction(int saveAction)
{
   settings_.set(kSaveAction, saveAction);
}


bool UserSettings::loadRData() const
{
   return settings_.getBool(kLoadRData, true);
}

void UserSettings::setLoadRData(bool loadRData)
{
   settings_.set(kLoadRData, loadRData);
}

bool UserSettings::showLastDotValue() const
{
   return settings_.getBool("showLastDotValue", false);
}

void UserSettings::setShowLastDotValue(bool show)
{
   settings_.set("showLastDotValue", show);
}

console_process::TerminalShell::TerminalShellType UserSettings::defaultTerminalShellValue() const
{
   return static_cast<console_process::TerminalShell::TerminalShellType>(
            settings_.getInt(kDefaultTerminalShell,
               static_cast<int>(console_process::TerminalShell::DefaultShell)));
}

void UserSettings::setDefaultTerminalShellValue(
      console_process::TerminalShell::TerminalShellType shell)
{
   settings_.set(kDefaultTerminalShell, static_cast<int>(shell));
}

core::FilePath UserSettings::initialWorkingDirectory() const
{
   return getWorkingDirectoryValue(kInitialWorkingDirectory);
}

core::text::AnsiCodeMode UserSettings::ansiConsoleMode() const
{
   return static_cast<core::text::AnsiCodeMode>(readUiPref<int>(pAnsiConsoleMode_));
}

CRANMirror UserSettings::cranMirror() const
{
   // get the settings
   CRANMirror mirror;
   mirror.name = settings_.get(kCRANMirrorName);
   mirror.host = settings_.get(kCRANMirrorHost);
   mirror.url = settings_.get(kCRANMirrorUrl);
   mirror.country = settings_.get(kCRANMirrorCountry);

   // if there is no URL then return the default RStudio mirror
   // (return the insecure version so we can rely on probing for
   // the secure version). also check for "/" to cleanup from
   // a previous bug/regression
   if (mirror.url.empty() || (mirror.url == "/"))
   {
      mirror.name = "Global (CDN)";
      mirror.host = "RStudio";
      mirror.url = "http://cran.rstudio.com/";
      mirror.country = "us";
   }

   // re-map cran.rstudio.org to cran.rstudio.com
   if (boost::algorithm::starts_with(mirror.url, "http://cran.rstudio.org"))
      mirror.url = "http://cran.rstudio.com/";

   // remap url without trailing slash
   if (!boost::algorithm::ends_with(mirror.url, "/"))
      mirror.url += "/";

   return mirror;
}

void UserSettings::setCRANMirror(const CRANMirror& mirror)
{
   settings_.set(kCRANMirrorName, mirror.name);
   settings_.set(kCRANMirrorHost, mirror.host);
   settings_.set(kCRANMirrorUrl, mirror.url);
   settings_.set(kCRANMirrorCountry, mirror.country);

   // only set the underlying option if it's not empty (some
   // evidence exists that this is possible, it doesn't appear to
   // be possible in the current code however previous releases
   // may have let this in)
   if (!mirror.url.empty())
      setCRANReposOption(mirror.url);
}

BioconductorMirror UserSettings::bioconductorMirror() const
{
   BioconductorMirror mirror ;

   mirror.name = settings_.get(kBioconductorMirrorName);
   if (!mirror.name.empty())
   {
      mirror.url = settings_.get(kBioconductorMirrorUrl);
   }
   else
   {
      mirror.name = "Seattle (USA)";
      mirror.url = "http://www.bioconductor.org";
   }

   return mirror;
}

void UserSettings::setBioconductorMirror(
                        const BioconductorMirror& bioconductorMirror)
{
   settings_.set(kBioconductorMirrorName, bioconductorMirror.name);
   settings_.set(kBioconductorMirrorUrl, bioconductorMirror.url);

   setBioconductorReposOption(bioconductorMirror.url);
}

bool UserSettings::securePackageDownload() const
{
   return settings_.getBool("securePackageDownload", true);
}

void UserSettings::setSecurePackageDownload(bool secureDownload)
{
   settings_.set("securePackageDownload", secureDownload);
}


bool UserSettings::vcsEnabled() const
{
   return settings_.getBool("vcsEnabled", true);
}

void UserSettings::setVcsEnabled(bool enabled)
{
   settings_.set("vcsEnabled", enabled);
}

FilePath UserSettings::gitExePath() const
{
   std::string dir = settings_.get("vcsGitExePath");
   if (!dir.empty())
      return FilePath(dir);
   else
      return FilePath();
}

void UserSettings::setGitExePath(const FilePath& gitExePath)
{
   settings_.set("vcsGitExePath", gitExePath.absolutePath());
}

FilePath UserSettings::svnExePath() const
{
   std::string dir = settings_.get("vcsSvnExePath");
   if (!dir.empty())
      return FilePath(dir);
   else
      return FilePath();
}

void UserSettings::setSvnExePath(const FilePath& svnExePath)
{
   settings_.set("vcsSvnExePath", svnExePath.absolutePath());
}

FilePath UserSettings::vcsTerminalPath() const
{
   std::string dir = settings_.get("vcsTerminalPath");
   if (!dir.empty())
      return FilePath(dir);
   else
      return FilePath();
}

void UserSettings::setVcsTerminalPath(const FilePath& terminalPath)
{
   settings_.set("vcsTerminalPath", terminalPath.absolutePath());
}

bool UserSettings::vcsUseGitBash() const
{
   return settings_.getBool("vcsUseGitBash", true);
}

void UserSettings::setVcsUseGitBash(bool useGitBash)
{
   settings_.set("vcsUseGitBash", useGitBash);
}

bool UserSettings::cleanTexi2DviOutput() const
{
   return settings_.getBool("cleanTexi2DviOutput", true);
}

void UserSettings::setCleanTexi2DviOutput(bool cleanTexi2DviOutput)
{
   settings_.set("cleanTexi2DviOutput", cleanTexi2DviOutput);
}

bool UserSettings::enableLaTeXShellEscape() const
{
   return settings_.getBool("enableLaTeXShellEscape", false);
}

void UserSettings::setEnableLaTeXShellEscape(bool enableShellEscape)
{
   settings_.set("enableLaTeXShellEscape", enableShellEscape);
}

bool UserSettings::useInternet2() const
{
   return settings_.getBool("useInternet2", true);
}

void UserSettings::setUseInternet2(bool useInternet2)
{
   settings_.set("useInternet2", useInternet2);
}

bool UserSettings::cleanupAfterRCmdCheck() const
{
   return settings_.getBool("cleanupAfterRCmdCheck", true);
}

void UserSettings::setCleanupAfterRCmdCheck(bool cleanup)
{
   settings_.set("cleanupAfterRCmdCheck", cleanup);
}

bool UserSettings::viewDirAfterRCmdCheck() const
{
   return settings_.getBool("viewDirAfterRCmdCheck", false);
}

void UserSettings::setViewDirAfterRCmdCheck(bool viewDir)
{
   settings_.set("viewDirAfterRCmdCheck", viewDir);
}

bool UserSettings::hideObjectFiles() const
{
   return settings_.getBool("hideObjectFiles", true);
}

void UserSettings::setHideObjectFiles(bool hide)
{
   settings_.set("hideObjectFiles", hide);
}

bool UserSettings::alwaysSaveHistory() const
{
   return settings_.getBool(kAlwaysSaveHistory, true);
}

void UserSettings::setAlwaysSaveHistory(bool alwaysSave)
{
   settings_.set(kAlwaysSaveHistory, alwaysSave);
}

bool UserSettings::removeHistoryDuplicates() const
{
   return settings_.getBool(kRemoveHistoryDuplicates, false);
}

void UserSettings::setRemoveHistoryDuplicates(bool removeDuplicates)
{
   settings_.set(kRemoveHistoryDuplicates, removeDuplicates);

   // update in underlying R session
   r::session::consoleHistory().setRemoveDuplicates(removeDuplicates);
}

string_utils::LineEnding UserSettings::lineEndings() const
{
   return (string_utils::LineEnding)settings_.getInt(
                                     kLineEndings,
                                     string_utils::LineEndingNative);
}

void UserSettings::setLineEndings(string_utils::LineEnding lineEndings)
{
   settings_.set(kLineEndings, (int)lineEndings);
}


bool UserSettings::useNewlineInMakefiles() const
{
   return settings_.getBool(kUseNewlineInMakefiles, true);
}

void UserSettings::setUseNewlineInMakefiles(bool useNewline)
{
   settings_.set(kUseNewlineInMakefiles, useNewline);
}


void UserSettings::setInitialWorkingDirectory(const FilePath& filePath)
{
   setWorkingDirectoryValue(kInitialWorkingDirectory, filePath);
}

FilePath UserSettings::getWorkingDirectoryValue(const std::string& key) const
{
   return module_context::resolveAliasedPath(
            settings_.get(key, session::options().defaultWorkingDir()));
}


void UserSettings::setWorkingDirectoryValue(const std::string& key,
                                            const FilePath& filePath)
{
   if (module_context::createAliasedPath(filePath) == "~")
      settings_.set(key, std::string("~"));
   else
      settings_.set(key, filePath.absolutePath());
}

int UserSettings::errorHandlerType() const
{
   return settings_.getInt("errorHandlerType",
                           modules::errors::ERRORS_TRACEBACK);
}

void UserSettings::setErrorHandlerType(int type)
{
   settings_.set("errorHandlerType", type);
}

bool UserSettings::useDevtools() const
{
   return settings_.getBool("useDevtools", true);
}

void UserSettings::setUseDevtools(bool useDevtools)
{
   settings_.set("useDevtools", useDevtools);
}

int UserSettings::clangVerbose() const
{
   return settings_.getInt("clangVerbose", 0);
}

void UserSettings::setClangVerbose(int level)
{
   settings_.set("clangVerbose", level);
}

void UserSettings::setEnableStyleDiagnostics(bool enable)
{
   settings_.set("enableStyleDiagnostics", enable);
}

void UserSettings::setLintRFunctionCalls(bool enable)
{
   settings_.set("lintRFunctionCalls", enable);
}

bool  UserSettings::usingMingwGcc49() const
{
   return boost::algorithm::contains(core::system::getenv("R_COMPILED_BY"),
                                     "4.9.3") ||
          settings_.getBool("usingMingwGcc49", false);
}

void  UserSettings::setUsingMingwGcc49(bool usingMingwGcc49)
{
   settings_.set("usingMingwGcc49", usingMingwGcc49);
}

std::string UserSettings::showUserHomePage() const
{
   return settings_.get(kServerHomeSetting, kServerHomeSessions);
}

void UserSettings::setShowUserHomePage(const std::string& value)
{
   settings_.set(kServerHomeSetting, value);
}

bool UserSettings::reuseSessionsForProjectLinks() const
{
   return settings_.getBool(kReuseSessionsForProjectLinksSettings, true);
}

void UserSettings::setReuseSessionsForProjectLinks(bool reuse)
{
   settings_.set(kReuseSessionsForProjectLinksSettings, reuse);
}


} // namespace session
} // namespace rstudio
