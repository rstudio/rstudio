/*
 * SessionEnvironment.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionEnvironment.hpp"
#include "EnvironmentMonitor.hpp"

#include <algorithm>

#include <core/Exec.hpp>

#define INTERNAL_R_FUNCTIONS
#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/session/RSession.hpp>
#include <r/RInterface.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <core/FileSerializer.hpp>
#include <boost/foreach.hpp>

#include "EnvironmentUtils.hpp"

#define TOP_FUNCTION 1

using namespace core ;

namespace session {
namespace modules { 
namespace environment {

EnvironmentMonitor s_environmentMonitor;

namespace {

bool handleRBrowseEnv(const core::FilePath& filePath)
{
   if (filePath.filename() == "wsbrowser.html")
   {
      module_context::showContent("R objects", filePath);
      return true;
   }
   else
   {
      return false;
   }
}

// given a context from the context stack, indicate whether it is executing a
// user-defined function
bool isUserFunctionContext(RCNTXT *pContext)
{
   return (pContext->callflag & CTXT_FUNCTION) &&
         pContext->srcref;
}

// return the function context at the given depth
RCNTXT* getFunctionContext(const int depth,
                           int* pFoundDepth = NULL,
                           SEXP* pEnvironment = NULL)
{
   RCNTXT* pRContext = r::getGlobalContext();
   int currentDepth = 0;
   while (pRContext->callflag)
   {
      if (isUserFunctionContext(pRContext))
      {
         if (++currentDepth == depth)
         {
            break;
         }
      }
      pRContext = pRContext->nextcontext;
   }

   // indicate the depth at which we stopped and the environment we found at
   // that depth, if requested
   if (pFoundDepth)
   {
      *pFoundDepth = currentDepth;
   }
   if (pEnvironment)
   {
      *pEnvironment = currentDepth == 0 ? R_GlobalEnv : pRContext->cloenv;
   }
   return pRContext;
}

SEXP getEnvironment(const int depth)
{
   return depth == 0 ? R_GlobalEnv : getFunctionContext(depth)->cloenv;
}

// return whether the context stack contains a pure (interactive) browser
bool inBrowseContext()
{
   RCNTXT* pRContext = r::getGlobalContext();
   while (pRContext->callflag)
   {
      if ((pRContext->callflag & CTXT_BROWSER) &&
          !(pRContext->callflag & CTXT_FUNCTION))
      {
         return true;
      }
      pRContext = pRContext->nextcontext;
   }
   return false;
}

std::string functionNameFromContext(RCNTXT* pContext)
{
   return r::sexp::asString(PRINTNAME(CAR(pContext->call)));
}

void getSourceRefFromContext(const RCNTXT* pContext,
                             std::string* pFileName,
                             int* pLineNumber)
{
   SEXP srcref = pContext->srcref;
   *pLineNumber = r::sexp::asInteger(srcref);
   Error error = r::exec::RFunction(".rs.sourceFileFromRef", srcref)
                 .call(pFileName);
   if (error)
   {
      LOG_ERROR(error);
   }
   return;
}

SEXP getFunctionSourceRefFromContext(const RCNTXT* pContext)
{
   return r::sexp::getAttrib(pContext->callfun, "srcref");
}

json::Array callFramesAsJson()
{
   RCNTXT* pRContext = r::getGlobalContext();
   RCNTXT* pSrcContext = pRContext;
   json::Array listFrames;
   int contextDepth = 0;

   while (pRContext->callflag)
   {
      if (isUserFunctionContext(pRContext))
      {
         json::Object varFrame;
         varFrame["context_depth"] = ++contextDepth;
         varFrame["function_name"] = functionNameFromContext(pRContext);

         // in the linked list of R contexts, the srcref associated with each
         // context points to the place from which the context was invoked.
         // however, for traditional debugging, we want the call frame to show
         // where control *left* the frame to go to the next frame. pSrcContext
         // keeps track of the previous invocation.
         std::string filename;
         int lineNumber = 0;
         getSourceRefFromContext(pSrcContext, &filename, &lineNumber);
         varFrame["file_name"] = filename;
         varFrame["line_number"] = lineNumber;
         pSrcContext = pRContext;

         // extract the first line of the function. the client can optionally
         // use this to compute the source location as an offset into the
         // function rather than as an absolute file position (useful when
         // we need to debug a copy of the function rather than the real deal).
         SEXP srcRef = getFunctionSourceRefFromContext(pSrcContext);
         if (srcRef && TYPEOF(srcRef) != NILSXP)
         {
            varFrame["function_line_number"] = INTEGER(srcRef)[0];
         }

         std::string argList;
         Error error = r::exec::RFunction(".rs.argumentListSummary",
                                    CDR(pRContext->call)).call(&argList);
         if (error)
         {
            LOG_ERROR(error);
         }
         varFrame["argument_list"] = error ? "" : argList;

         listFrames.push_back(varFrame);
      }
      pRContext = pRContext->nextcontext;
   }
   return listFrames;
}

json::Array environmentListAsJson(int depth)
{
    using namespace r::sexp;
    Protect rProtect;
    std::vector<Variable> vars;
    listEnvironment(getEnvironment(depth), false, &rProtect, &vars);

    // get object details and transform to json
    json::Array listJson;
    std::transform(vars.begin(),
                   vars.end(),
                   std::back_inserter(listJson),
                   varToJson);
    return listJson;
}

Error listEnvironment(boost::shared_ptr<int> pContextDepth,
                      const json::JsonRpcRequest&,
                      json::JsonRpcResponse* pResponse)
{
   // return list
   pResponse->setResult(environmentListAsJson(*pContextDepth));
   return Success();
}

// given a function context, indicate whether the copy of the source code
// for the function is different than the source code on disk.
bool functionIsOutOfSync(const RCNTXT *pContext,
                         std::string *pFunctionCode)
{
   std::string fileName;
   std::string fileContent;
   Error error;

   // start by extracting the source code from the call site
   error = r::exec::RFunction(".rs.sourceCodeFromCall", pContext->callfun)
         .call(pFunctionCode);
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }

   // next, look up the name of the source file that contains the file in
   // question
   SEXP srcRef = getFunctionSourceRefFromContext(pContext);
   if (srcRef == NULL || TYPEOF(srcRef) == NILSXP)
   {
      return true;
   }
   error = r::exec::RFunction(".rs.sourceFileFromRef", srcRef)
                 .call(&fileName);
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }

   // check for ~/.active-rstudio-document -- we never want to match sources
   // in this file, as it's used to source unsaved changes from RStudio
   // editor buffers
   boost::algorithm::trim(fileName);
   if (fileName == "~/.active-rstudio-document")
   {
      return true;
   }

   // make sure the file exists
   FilePath sourceFilePath = module_context::resolveAliasedPath(fileName);
   if (!sourceFilePath.exists())
   {
      return true;
   }

   // check the list of source documents in the working set; if this source
   // document has unsaved changes, don't try to match the lines
   std::vector<boost::shared_ptr<source_database::SourceDocument> > docs ;
   error = source_database::list(&docs);
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }

   BOOST_FOREACH(boost::shared_ptr<source_database::SourceDocument> doc, docs)
   {
      if (doc->path() == fileName &&
          doc->dirty())
      {
         return true;
      }
   }

   // read the portion of the file pointed to by the source refs from disk
   // the sourceref structure (including the array offsets used below)
   // is documented here:
   // http://journal.r-project.org/archive/2010-2/RJournal_2010-2_Murdoch.pdf
   error = readStringFromFile(
         sourceFilePath,
         &fileContent,
         string_utils::LineEndingPosix,
         INTEGER(srcRef)[0],  // the first line
         INTEGER(srcRef)[2],  // the last line
         INTEGER(srcRef)[4],  // character position on the first line
         INTEGER(srcRef)[5]   // character position on the last line
         );
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }
   return *pFunctionCode != fileContent;
}

// create a JSON object that contains information about the current environment;
// used both to initialize the environment state on first load and to send
// information about the new environment on a context change
json::Value commonEnvironmentStateData(int depth)
{
   json::Object varJson;
   bool useProvidedSource = false;
   std::string functionCode;

   varJson["context_depth"] = depth;
   varJson["environment_list"] = environmentListAsJson(depth);
   varJson["call_frames"] = callFramesAsJson();

   // if we're in a debug context, add information about the function currently
   // being debugged
   if (depth > 0)
   {
      RCNTXT* pContext = getFunctionContext(depth);
      varJson["function_name"] = functionNameFromContext(pContext);

      // see if the function to be debugged is out of sync with its saved
      // sources (if available)--if it is, emit its code so the client can
      // display it
      if (isUserFunctionContext(pContext))
      {
         useProvidedSource = functionIsOutOfSync(pContext, &functionCode);
      }
   }
   else
   {
      varJson["function_name"] = "";
   }

   varJson["use_provided_source"] = useProvidedSource;
   varJson["function_code"] = useProvidedSource ? functionCode : "";

   return varJson;
}

void enqueContextDepthChangedEvent(int depth)
{
   // emit an event to the client indicating the new call frame and the
   // current state of the environment
   ClientEvent event (client_events::kContextDepthChanged,
                      commonEnvironmentStateData(depth));
   module_context::enqueClientEvent(event);
}

void enqueBrowserLineChangedEvent(int newLineNumber)
{
   json::Object varJson;
   varJson["line_number"] = newLineNumber;
   ClientEvent event (client_events::kBrowserLineChanged, varJson);
   module_context::enqueClientEvent(event);
}

Error setContextDepth(boost::shared_ptr<int> pContextDepth,
                      const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   // get the requested depth
   int requestedDepth;
   Error error = json::readParam(request.params, 0, &requestedDepth);
   if (error)
      return error;

   // set state for the new depth
   *pContextDepth = requestedDepth;

   // populate the new state on the client
   enqueContextDepthChangedEvent(*pContextDepth);

   return Success();
}


void onDetectChanges(module_context::ChangeSource source)
{
   s_environmentMonitor.checkForChanges();
}

void onConsolePrompt(boost::shared_ptr<int> pContextDepth)
{
   int depth = 0;
   SEXP environmentTop = NULL;
   getFunctionContext(TOP_FUNCTION, &depth, &environmentTop);

   // we entered (or left) a call frame
   if (environmentTop != s_environmentMonitor.getMonitoredEnvironment() ||
       depth != *pContextDepth)
   {
      json::Object varJson;

      // if we appear to be switching into debug mode, make sure there's a
      // browser call somewhere on the stack. if there isn't, then we're
      // probably just waiting for user input inside a function (e.g. scan());
      // assume the user isn't interested in seeing the function's internals.
      if (*pContextDepth == 0 &&
          !inBrowseContext())
      {
         return;
      }

      // start monitoring the enviroment at the new depth
      s_environmentMonitor.setMonitoredEnvironment(environmentTop);
      *pContextDepth = depth;
      enqueContextDepthChangedEvent(depth);
   }
   // if we're debugging and stayed in the same frame, update the line number
   else if (depth > 0)
   {
      int lineNumber = r::sexp::asInteger(r::getGlobalContext()->srcref);
      enqueBrowserLineChangedEvent(lineNumber);
   }
}


} // anonymous namespace

json::Value environmentStateAsJson()
{
   int contextDepth = 0;
   getFunctionContext(TOP_FUNCTION, &contextDepth);
   return commonEnvironmentStateData(contextDepth);
}

Error initialize()
{
   boost::shared_ptr<int> pContextDepth = boost::make_shared<int>(0);

   // begin monitoring the environment
   SEXP environmentTop = NULL;
   getFunctionContext(TOP_FUNCTION, pContextDepth.get(), &environmentTop);
   s_environmentMonitor.setMonitoredEnvironment(environmentTop);

   // subscribe to events
   using boost::bind;
   using namespace session::module_context;
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   events().onConsolePrompt.connect(bind(onConsolePrompt, pContextDepth));

   json::JsonRpcFunction listEnv =
         boost::bind(listEnvironment, pContextDepth, _1, _2);
   json::JsonRpcFunction setCtxDepth =
         boost::bind(setContextDepth, pContextDepth, _1, _2);

   // source R functions
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRBrowseFileHandler, handleRBrowseEnv))
      (bind(registerRpcMethod, "list_environment", listEnv))
      (bind(registerRpcMethod, "set_context_depth", setCtxDepth))
      (bind(sourceModuleRFile, "SessionEnvironment.R"));

   return initBlock.execute();
}
   
} // namespace environment
} // namespace modules
} // namesapce session

