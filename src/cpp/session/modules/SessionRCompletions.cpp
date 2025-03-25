/*
 * SessionRCompletions.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include "SessionRCompletions.hpp"

#include <gsl/gsl-lite.hpp>

#include <core/Exec.hpp>

#include <boost/range/adaptors.hpp>

#include <r/RSexp.hpp>
#include <r/RInternal.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/RRoutines.hpp>
#include <r/ROptions.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RSessionUtils.hpp>

#include <core/StringUtils.hpp>
#include <core/system/FileScanner.hpp>
#include <core/text/TextCursor.hpp>

#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RSourceIndex.hpp>
#include <core/r_util/RPackageInfo.hpp>
#include <core/r_util/RSourceIndex.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionCodeSearch.hpp"
#include "SessionLibPathsIndexer.hpp"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace r_packages {

namespace {

char ends(char begins)
{
   switch (begins) {
   case '(': return ')';
   case '[': return ']';
   case '{': return '}';
   }
   return '\0';
}

bool isOperator(char character)
{
   switch (character)
   {
   case '~':
   case '!':
   case '@':
   case '$':
   case '%':
   case '^':
   case '&':
   case '+':
   case '-':
   case '*':
   case '/':
   case '=':
   case '|':
   case '<':
   case '>':
   case '?':
      return true;
   default:
      return false;
   }
}

} // end anonymous namespace

std::wstring finishExpression(const std::wstring& expression)
{
   using namespace string_utils;
   
   std::wstring result;
   std::vector<wchar_t> terminators;

   auto top = L'\0';
   terminators.push_back(top);

   bool inString = false;
   bool inEscape = false;
   bool sawOperator = false;
   bool sawIdentifier = false;

   for (std::size_t i = 0, n = expression.size(); i < n; i++)
   {
      wchar_t ch = expression[i];
      
      // skip over whitespace
      if (iswspace(ch))
      {
         // if we see a newline, assume it ends the current expression, so it's okay
         // to have consecutive identifiers if a newline separates them
         if (sawIdentifier && ch == L'\n')
            sawIdentifier = false;
         
         // only add whitespace if we didn't previously see an identifier character
         // this has the effect of concatenating adjacent identifiers; e.g.
         //
         //     foo(a b c)  =>  foo(abc)
         //
         // which helps ensure the statement is parsable by R
         if (!sawIdentifier)
            result.push_back(ch);
         
         // keep going
         continue;
      }
      
      // if we saw an operator previously, check for closing parens
      // this allows us to support common code structure like
      //
      //    foo(a + )
      //
      // where the user hasn't yet "filled in" the rhs of an operator
      if (sawOperator)
      {
         if (ch == L')' || ch == L'}' || ch == L']')
         {
            result.push_back(L'.');
         }
      }

      // push back the current character
      result.push_back(ch);
      
      // handle strings
      if (inString)
      {
         if (inEscape)
         {
            inEscape = false;
            continue;
         }

         if (ch == L'\\')
         {
            inEscape = true;
            continue;
         }

         if (ch != top)
         {
            continue;
         }

         inString = false;
         terminators.pop_back();
         top = terminators.back();
         continue;
      }
      
      // check for operators
      sawOperator = isOperator(ch);
      
      // check for identifiers
      //
      // TODO: This is wrong for multibyte characters; we should really start
      // using a proper library for handling UTF-8 inputs...
      sawIdentifier = iswalnum(ch) || ch == L'.' || ch == L'_';

      if (ch == top)
      {
         terminators.pop_back();
         top = terminators.back();
      }
      else if (ch == L'(' || ch == L'{' || ch == L'[')
      {
         top = ends(ch);
         terminators.push_back(top);
      }
      else if (ch == L'"' || ch == L'`' || ch == L'\'')
      {
         top = ch;
         inString = true;
         terminators.push_back(top);
      }
   }
   
   // finish an operator if necessary
   if (sawOperator)
      result.push_back(L'.');

   // append to the output
   for (std::size_t i = terminators.size() - 1; i > 0; --i)
      result.push_back(terminators[i]);

   return result;
}

std::string finishExpression(const std::string& expression)
{
   return string_utils::wideToUtf8(finishExpression(string_utils::utf8ToWide(expression)));
}

namespace {

SEXP rs_finishExpression(SEXP stringSEXP)
{
   r::sexp::Protect rProtect;
   int n = r::sexp::length(stringSEXP);

   std::vector<std::string> output;
   output.reserve(n);

   void* vmax = vmaxget();
   for (int i = 0; i < n; ++i)
   {
      SEXP charSEXP = STRING_ELT(stringSEXP, i);
      const char* inputText = Rf_translateCharUTF8(charSEXP);
      std::wstring inputTextWide = string_utils::utf8ToWide(inputText);
      std::wstring finishedExprWide = finishExpression(inputTextWide);
      output.push_back(string_utils::wideToUtf8(finishedExprWide));
   }
   vmaxset(vmax);

   return r::sexp::create(output, &rProtect);
}

struct SourceIndexCompletions {
   std::vector<std::string> completions;
   std::vector<bool> isFunction;
   bool moreAvailable;
};

SourceIndexCompletions getSourceIndexCompletions(const std::string& token)
{
   // get functions from the source index
   std::vector<core::r_util::RSourceItem> items;
   bool moreAvailable = false;

   // TODO: wire up 'moreAvailable'
   modules::code_search::searchSource(token,
                                      1000,
                                      true,
                                      &items,
                                      &moreAvailable);

   SourceIndexCompletions srcCompletions;
   for (const core::r_util::RSourceItem& item : items)
   {
      if (item.braceLevel() == 0)
      {
         srcCompletions.completions.push_back(item.name());
         srcCompletions.isFunction.push_back(item.isFunction() || item.isMethod());
      }
   }

   srcCompletions.moreAvailable = moreAvailable;
   return srcCompletions;
}

SEXP rs_getSourceIndexCompletions(SEXP tokenSEXP)
{
   r::sexp::Protect protect;
   std::string token = r::sexp::asString(tokenSEXP);
   SourceIndexCompletions srcCompletions = getSourceIndexCompletions(token);

   std::vector<std::string> names;
   names.push_back("completions");
   names.push_back("isFunction");
   names.push_back("moreAvailable");

   SEXP resultSEXP = r::sexp::createList(names, &protect);
   r::sexp::setNamedListElement(resultSEXP, "completions", srcCompletions.completions);
   r::sexp::setNamedListElement(resultSEXP, "isFunction", srcCompletions.isFunction);
   r::sexp::setNamedListElement(resultSEXP, "moreAvailable", srcCompletions.moreAvailable);

   return resultSEXP;
}

bool subsequenceFilter(const FileInfo& fileInfo,
                       const std::string& pattern,
                       int parentPathLength,
                       int maxCount,
                       std::vector<std::string>* pPaths,
                       int* pCount,
                       bool* pMoreAvailable)
{
   if (*pCount >= maxCount)
   {
      *pMoreAvailable = true;
      return false;
   }
   
   bool isSubsequence = string_utils::isSubsequence(
            fileInfo.absolutePath().substr(parentPathLength + 2),
            pattern,
            true);
   
   if (isSubsequence)
   {
      ++*pCount;
      pPaths->push_back(fileInfo.absolutePath());
   }

   // Always add subdirectories
   if (fileInfo.isDirectory())
   {
      return true;
   }
   
   return false;
}

SEXP rs_scanFiles(SEXP pathSEXP,
                  SEXP patternSEXP,
                  SEXP maxCountSEXP)
{
   std::string path = r::sexp::asString(pathSEXP);
   std::string pattern = r::sexp::asString(patternSEXP);
   int maxCount = r::sexp::asInteger(maxCountSEXP);

   FilePath filePath(path);
   FileInfo fileInfo(filePath);
   tree<FileInfo> tree;

   core::system::FileScannerOptions options;
   options.recursive = true;
   options.yield = true;

   // Use a subsequence filter, and bail after too many files
   std::vector<std::string> paths;
   
   int count = 0;
   bool moreAvailable = false;
   options.filter = boost::bind(subsequenceFilter,
                                _1,
                                pattern,
                                gsl::narrow_cast<int>(path.length()),
                                maxCount,
                                &paths,
                                &count,
                                &moreAvailable);

   Error error = scanFiles(fileInfo, options, &tree);
   if (error)
      return R_NilValue;

   r::sexp::Protect protect;
   r::sexp::ListBuilder builder(&protect);

   builder.add("paths", paths);
   builder.add("more_available", moreAvailable);

   return r::sexp::create(builder, &protect);
}

SEXP rs_isSubsequence(SEXP stringsSEXP, SEXP querySEXP)
{
   std::vector<std::string> strings;
   if (!r::sexp::fillVectorString(stringsSEXP, &strings))
      return R_NilValue;

   std::string query = r::sexp::asString(querySEXP);

   std::vector<bool> result(strings.size());

   std::transform(strings.begin(),
                  strings.end(),
                  result.begin(),
                  boost::bind(
                     string_utils::isSubsequence,
                     _1,
                     query));

   r::sexp::Protect protect;
   return r::sexp::create(result, &protect);

}

SEXP rs_getNAMESPACEImportedSymbols(SEXP documentIdSEXP)
{
   std::string documentId = r::sexp::asString(documentIdSEXP);
   boost::shared_ptr<core::r_util::RSourceIndex> index =
         code_search::rSourceIndex().get(documentId);
   
   using namespace core::r_util;
   std::vector<std::string> pkgs;
   
   for (const std::string& pkg : RSourceIndex::getImportedPackages())
   {
      pkgs.push_back(pkg);
   }
   
   for (const std::string& pkg :
                 RSourceIndex::getImportFromDirectives() | boost::adaptors::map_keys)
   {
      pkgs.push_back(pkg);
   }
   
   using namespace r::sexp;
   Protect protect;
   r::sexp::ListBuilder builder(&protect);
   
   for (const std::string& pkg : pkgs)
   {
      const PackageInformation& completions = RSourceIndex::getPackageInformation(pkg);
      r::sexp::ListBuilder child(&protect);
      
      // If the inferred package is listed _only_ in the NAMESPACE 'importFrom',
      // then we want to selectively return symbols.
      if ((index && core::algorithm::contains(index->getInferredPackages(), pkg)) &&
          RSourceIndex::getImportedPackages().count(pkg) == 0 &&
          RSourceIndex::getImportFromDirectives().count(pkg) != 0)
      {
         std::vector<std::string> exports;
         std::vector<int> types;
         
         std::set<std::string>& directives =
               RSourceIndex::getImportFromDirectives()[pkg];
         
         for (const std::string& item : directives)
         {
            std::vector<std::string>::const_iterator it =
                  std::find(completions.exports.begin(),
                            completions.exports.end(),
                            item);
            
            if (it == completions.exports.end())
               continue;
            
            std::size_t index = it - completions.exports.begin();
            
            exports.push_back(completions.exports[index]);
            types.push_back(completions.types[index]);
         }
         
         child.add("exports", exports);
         child.add("types", types);
         
      }
      else
      {
         child.add("exports", completions.exports);
         child.add("types", completions.types);
      }
      
      builder.add(pkg, child);
   }
   
   return r::sexp::create(builder, &protect);
}

SEXP rs_getInferredCompletions(SEXP packagesSEXP)
{
   using namespace rstudio::core::r_util;

   std::vector<std::string> packages;
   if (!r::sexp::fillVectorString(packagesSEXP, &packages))
      return R_NilValue;

   r::sexp::Protect protect;
   r::sexp::ListBuilder parent(&protect);
   
   for (std::vector<std::string>::const_iterator it = packages.begin();
        it != packages.end();
        ++it)
   {
      DEBUG("Adding entry for '" << *it << "'");
      PackageInformation pkgInfo = RSourceIndex::getPackageInformation(*it);
      
      r::sexp::ListBuilder builder(&protect);
      builder.add("exports", pkgInfo.exports);
      builder.add("types", pkgInfo.types);
      builder.add("functions", core::r_util::infoToFormalMap(pkgInfo.functionInfo));
      builder.add("datasets", pkgInfo.datasets);
      parent.add(*it, builder);
   }
   
   return r::sexp::create(parent, &protect);
}

SEXP rs_listInferredPackages(SEXP documentIdSEXP)
{
   std::string documentId = r::sexp::asString(documentIdSEXP);
   boost::shared_ptr<core::r_util::RSourceIndex> index =
         code_search::rSourceIndex().get(documentId);

   // NOTE: can occur when user edits file not in source index
   if (index == nullptr)
      return R_NilValue;
   
   std::vector<std::string> pkgs = index->getInferredPackages();
   pkgs.insert(
            pkgs.end(),
            index->getImportedPackages().begin(),
            index->getImportedPackages().end());
   
   r::sexp::Protect protect;
   return r::sexp::create(pkgs, &protect);
   
}

SEXP rs_getKnitParamsForDocument(SEXP documentIdSEXP)
{
   using namespace source_database;
   
   std::string documentId = r::sexp::asString(documentIdSEXP);
   
   // Check for empty doc ID (can happen if this is requested e.g. from console)
   if (documentId.empty())
      return R_NilValue;
   
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument());
   
   Error error = get(documentId, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   
   if (!pDoc->isRMarkdownDocument())
      return R_NilValue;
   
   r::exec::RFunction knitParams(".rs.knitParams");
   knitParams.addParam(pDoc->contents());
   
   r::sexp::Protect protect;
   SEXP resultSEXP;
   error = knitParams.call(&resultSEXP, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   
   return resultSEXP;
}

SEXP rs_listIndexedPackages()
{
   std::vector<std::string> pkgNames;
   
   const std::vector<FilePath>& pkgPaths = libpaths::getInstalledPackages();
   pkgNames.reserve(pkgPaths.size());
   for (const FilePath& pkgPath : pkgPaths)
   {
      pkgNames.push_back(pkgPath.getAbsolutePath());
   }
   
   r::sexp::Protect protect;
   return r::sexp::create(pkgNames, &protect);
}

} // end anonymous namespace

Error initialize() {

   RS_REGISTER_CALL_METHOD(rs_finishExpression, 1);
   RS_REGISTER_CALL_METHOD(rs_getSourceIndexCompletions, 1);
   RS_REGISTER_CALL_METHOD(rs_scanFiles, 4);
   RS_REGISTER_CALL_METHOD(rs_isSubsequence, 2);
   RS_REGISTER_CALL_METHOD(rs_listInferredPackages, 1);
   RS_REGISTER_CALL_METHOD(rs_getInferredCompletions, 1);
   RS_REGISTER_CALL_METHOD(rs_getNAMESPACEImportedSymbols, 1);
   RS_REGISTER_CALL_METHOD(rs_getKnitParamsForDocument, 1);
   RS_REGISTER_CALL_METHOD(rs_listIndexedPackages, 0);
   
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionRCompletions.R"));
   return initBlock.execute();
}

} // namespace r_completions
} // namespace modules
} // namespace session
} // namespace rstudio
