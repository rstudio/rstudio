/*
 * SessionObjectExplorer.cpp
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

#define R_INTERNAL_FUNCTIONS

#include "SessionObjectExplorer.hpp"

#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <core/Algorithm.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {      
namespace explorer {

namespace {

const char * const kTagAttributes = "attributes";
const char * const kTagVirtual    = "virtual";

const char * const kExplorerCacheDir = "explorer-cache";

// forward-declare some classes + functions
struct Context;
typedef json::Object InspectionResult;
InspectionResult inspectObject(SEXP, const Context&);

struct Context
{
   std::string name;
   std::string access;
   std::vector<std::string> tags;
   int recursive;
};

Context createContext(const std::string& name,
                      const std::string& access,
                      const std::vector<std::string>& tags,
                      int recursive)
{
   Context context;
   
   context.name      = name;
   context.access    = access;
   context.tags      = tags;
   context.recursive = recursive;
   
   return context;
}

Context createChildContext(const Context& context,
                           const std::string& name,
                           const std::string& access,
                           const std::vector<std::string>& tags)
{
   Context childContext(context);
   
   childContext.name      = name;
   childContext.access    = access;
   childContext.tags      = tags;
   
   childContext.recursive -= 1;
   
   return childContext;
}

FilePath explorerCacheDir() 
{
   return module_context::sessionScratchPath()
         .childPath(kExplorerCacheDir);
}

std::string explorerCacheDirSystem()
{
   return string_utils::utf8ToSystem(explorerCacheDir().absolutePath());
}

void removeOrphanedCacheItems()
{
   Error error;
   
   // if we don't have a cache, nothing to do
   if (!explorerCacheDir().exists())
      return;
   
   // list source documents
   std::vector<FilePath> docPaths;
   error = source_database::list(&docPaths);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // read their properties
   typedef source_database::SourceDocument SourceDocument;
   typedef boost::shared_ptr<SourceDocument> Document;
   
   std::vector<Document> documents;
   BOOST_FOREACH(const FilePath& docPath, docPaths)
   {
      Document pDoc(new SourceDocument());
      Error error = source_database::get(docPath.filename(), false, pDoc);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      documents.push_back(pDoc);
   }
   
   // list objects in explorer cache
   std::vector<FilePath> cachedFiles;
   error = explorerCacheDir().children(&cachedFiles);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // remove any objects for which we don't have an associated
   // source document available
   BOOST_FOREACH(const FilePath& cacheFile, cachedFiles)
   {
      std::string id = cacheFile.filename();
      
      bool foundId = false;
      BOOST_FOREACH(Document pDoc, documents)
      {
         if (id == pDoc->getProperty("id"))
         {
            foundId = true;
            break;
         }
      }
      
      if (!foundId)
      {
         error = cacheFile.remove();
         if (error)
            LOG_ERROR(error);
      }
   }
}

void onShutdown(bool terminatedNormally)
{
   if (!terminatedNormally)
      return;
   
   using namespace r::exec;
   Error error = RFunction(".rs.explorer.saveCache")
         .addParam(explorerCacheDirSystem())
         .call();
   
   if (error)
      LOG_ERROR(error);
}

void onSuspend(const r::session::RSuspendOptions&,
               core::Settings*)
{
   onShutdown(true);
}

void onResume(const Settings&)
{
   
}

void onDocPendingRemove(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   Error error;
   
   // if we have a cache item associated with this document, remove it
   std::string id = pDoc->getProperty("id");
   if (id.empty())
      return;
   
   FilePath cachePath = explorerCacheDir().childPath(id);
   error = cachePath.removeIfExists();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // also attempt to remove from R cache
   using namespace r::exec;
   error = RFunction(".rs.explorer.removeCachedObject")
         .addParam(id)
         .call();
   
   if (error)
      LOG_ERROR(error);
}

void onDeferredInit(bool)
{
   Error error;
   
   error = explorerCacheDir().ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   removeOrphanedCacheItems();
   
   using namespace r::exec;
   error = RFunction(".rs.explorer.restoreCache")
         .addParam(explorerCacheDirSystem())
         .call();
   
   if (error)
      LOG_ERROR(error);
}

Error explorerInspectObject(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   Error error;
   
   std::string id, extractingCode, name, access;
   json::Array tagsJson;
   int recursive;
   error = json::readParams(request.params,
                            &id,
                            &extractingCode,
                            &name,
                            &access,
                            &tagsJson,
                            &recursive);
   if (error)
      LOG_ERROR(error);
   
   std::vector<std::string> tags;
   json::fillVectorString(tagsJson, &tags);
   
   // retrieve cached object
   r::sexp::Protect protect;
   SEXP objectSEXP = R_NilValue;
   error = r::exec::RFunction(".rs.explorer.getCachedObject")
         .addParam(id)
         .addParam(extractingCode)
         .call(&objectSEXP, &protect);
   if (error)
      LOG_ERROR(error);
   
   Context context = createContext(name, access, tags, recursive);
   InspectionResult result = inspectObject(objectSEXP, context);
   pResponse->setResult(result);
   return Success();
}

Error explorerBeginInspect(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   Error error;
   
   std::string id, name;
   error = json::readParams(request.params, &id, &name);
   if (error)
      LOG_ERROR(error);
   
   // retrieve cached object
   r::sexp::Protect protect;
   SEXP objectSEXP = R_NilValue;
   error = r::exec::RFunction(".rs.explorer.getCachedObject")
         .addParam(id)
         .call(&objectSEXP, &protect);
   if (error)
      LOG_ERROR(error);
   
   Context context = createContext(
            name,
            "",
            std::vector<std::string>(),
            1);
   
   InspectionResult result = inspectObject(objectSEXP, context);
   pResponse->setResult(result);
   return Success();
}

std::string describeWith(SEXP objectSEXP, const char* function)
{
   using namespace r::exec;
   
   std::string name;
   Error error = RFunction(function)
         .addParam(objectSEXP)
         .call(&name);
   if (error)
      LOG_ERROR(error);
   return name;
}

std::string objectName(SEXP objectSEXP)
{
   return describeWith(objectSEXP, ".rs.explorer.objectName");
}

std::string objectType(SEXP objectSEXP)
{
   // shortcut for symbols
   if (TYPEOF(objectSEXP) == SYMSXP)
      return "symbol";
   
   // delegate to R function
   return describeWith(objectSEXP, ".rs.explorer.objectType");
}

std::string objectDesc(SEXP objectSEXP)
{
   // shortcut for symbols
   if (TYPEOF(objectSEXP) == SYMSXP)
   {
      const char* fmt = "`%s`";
      return string_utils::format(fmt, CHAR(PRINTNAME(objectSEXP)));
   }
   
   return describeWith(objectSEXP, ".rs.explorer.objectDesc");
}

bool isExpandable(SEXP objectSEXP)
{
   // is this an S4 object with one or more slots?
   // TODO: check number of slots!
   if (IS_S4_OBJECT(objectSEXP))
      return true;
   
   // do we have relevant attributes?
   SEXP attribSEXP = ATTRIB(objectSEXP);
   if (attribSEXP != R_NilValue)
      return true;
   
   // is this an R list / environment with children?
   int n = ::Rf_length(objectSEXP);
   int type = TYPEOF(objectSEXP);
   if (type == ENVSXP || type == VECSXP)
      return n > 0;
   
   // is this a named atomic vector?
   if (r::sexp::isAtomic(objectSEXP))
   {
      SEXP namesSEXP = ::Rf_getAttrib(objectSEXP, R_NamesSymbol);
      return namesSEXP != R_NilValue && n > 0;
   }
   
   // failed expansion checks; return false
   return false;
}

std::string objectAddress(SEXP objectSEXP)
{
   std::stringstream ss;
   ss << std::hex << (void*) objectSEXP;
   return ss.str();
}

std::vector<std::string> objectClass(SEXP objectSEXP)
{
   std::vector<std::string> classes;
   
   SEXP classSEXP = ::Rf_getAttrib(objectSEXP, R_ClassSymbol);
   if (TYPEOF(classSEXP) != STRSXP)
      return classes;
   
   for (R_len_t i = 0, n = ::Rf_length(classSEXP); i < n; i++)
      classes.push_back(CHAR(STRING_ELT(classSEXP, i)));
   
   return classes;
}

SEXP rs_objectClass(SEXP objectSEXP)
{
   SEXP attribSEXP = ATTRIB(objectSEXP);
   if (attribSEXP == R_NilValue)
      return R_NilValue;
   
   while (attribSEXP != R_NilValue)
   {
      SEXP tagSEXP = TAG(attribSEXP);
      if (TYPEOF(tagSEXP) == SYMSXP)
      {
         const char* tag = CHAR(PRINTNAME(tagSEXP));
         if (::strcmp(tag, "class") == 0)
            return CAR(attribSEXP);
      }
      
      attribSEXP = CDR(attribSEXP);
   }
   
   return R_NilValue;
}

SEXP rs_objectAddress(SEXP objectSEXP)
{
   std::string address = objectAddress(objectSEXP);
   r::sexp::Protect protect;
   return r::sexp::create(address, &protect);
}

SEXP rs_objectAttributes(SEXP objectSEXP)
{
   return ATTRIB(objectSEXP);
}

SEXP rs_explorerCacheDir()
{
   r::sexp::Protect protect;
   return r::sexp::create(explorerCacheDirSystem(), &protect);
}

SEXP rs_inspectObject(SEXP objectSEXP, SEXP contextSEXP)
{
   Error error;
   std::string name, access;
   std::vector<std::string> tags;
   int recursive = 1;
   
   error = r::sexp::getNamedListElement(contextSEXP, "name",      &name);
   if (error)
      LOG_ERROR(error);
   
   error = r::sexp::getNamedListElement(contextSEXP, "access",    &access);
   if (error)
      LOG_ERROR(error);
   
   error = r::sexp::getNamedListElement(contextSEXP, "tags",      &tags);
   if (error)
      LOG_ERROR(error);
   
   error = r::sexp::getNamedListElement(contextSEXP, "recursive", &recursive);
   if (error)
      LOG_ERROR(error);
   
   Context context = createContext(name, access, tags, recursive);
   InspectionResult result = inspectObject(objectSEXP, context);
   r::sexp::Protect protect;
   return r::sexp::create(result, &protect);
}

// Inspection Routines ----

InspectionResult createInspectionResult(
      SEXP objectSEXP,
      const Context& context,
      std::vector<InspectionResult>& children)
{
   Error error;
   
   bool s4 = IS_S4_OBJECT(objectSEXP);
   bool expandable = isExpandable(objectSEXP);
   
   // extract attributes when relevant
   if (context.recursive)
   {
      std::vector<std::string> tags;
      tags.push_back(kTagAttributes);
      tags.push_back(kTagVirtual);
      
      Context childContext = createChildContext(
               context,
               "(attributes)",
               "attributes(#)",
               tags);
      
      InspectionResult childResult = inspectObject(
               ATTRIB(objectSEXP),
               childContext);
      
      children.push_back(childResult);
   }
   
   // construct UI display attributes
   InspectionResult display;
   display["name"] = context.name;
   display["type"] = objectType(objectSEXP);
   display["desc"] = objectDesc(objectSEXP);
   
   // attach index to children when present
   for (std::size_t i = 0, n = children.size(); i < n; i++)
   {
      InspectionResult& child = children[i];
      child["index"] = (double) i;
   }
   
   // create inspection result
   SEXP namesSEXP = ::Rf_getAttrib(objectSEXP, R_NamesSymbol);
   bool named = TYPEOF(namesSEXP) == STRSXP == ::Rf_length(namesSEXP) != 0;
   
   InspectionResult result;
   result["address"]    = objectAddress(objectSEXP);
   result["type"]       = ::Rf_type2char(TYPEOF(objectSEXP));
   result["class"]      = json::toJsonArray(objectClass(objectSEXP));
   result["length"]     = ::Rf_length(objectSEXP);
   result["access"]     = context.access.empty() ? json::Value() : context.access;
   result["atomic"]     = r::sexp::isAtomic(objectSEXP);
   result["recursive"]  = !r::sexp::isAtomic(objectSEXP);
   result["expandable"] = expandable;
   result["named"]      = named;
   result["s4"]         = s4;
   result["tags"]       = json::toJsonArray(context.tags);
   result["display"]    = display;
   result["children"]   = context.recursive ? json::toJsonArray(children) : json::Value();
   
   return result;
}

InspectionResult inspectXmlNode(
      SEXP objectSEXP,
      const Context& context)
{
   // TODO
   return InspectionResult();
}

InspectionResult inspectList(
      SEXP objectSEXP,
      const Context& context)
{
   std::vector<InspectionResult> children;
   if (context.recursive)
   {
      // retrieve object length
      std::size_t n = ::Rf_length(objectSEXP);
      
      // retrieve names (NOTE: can be null / empty)
      std::vector<std::string> names;
      Error error = r::sexp::getNames(objectSEXP, &names);
      if (error)
         LOG_ERROR(error);
      
      for (std::size_t i = 0; i < n; i++)
      {
         std::string name, access;
         std::vector<std::string> tags;
         
         if (names.size() < n || names[i].empty())
         {
            // no names; generate virtual names
            name = string_utils::format("[[%i]]", (int) i);
            access = "#" + name;
            tags.push_back(kTagVirtual);
         }
         else
         {
            // use provided names for access
            name = names[i];
            access = string_utils::format("#[[\"%s\"]]", name.c_str());
         }
         
         Context childContext = createChildContext(context, name, access, tags);
         InspectionResult childResult = inspectObject(
                  VECTOR_ELT(objectSEXP, i),
                  childContext);
         children.push_back(childResult);
      }
   }
   
   return createInspectionResult(objectSEXP, context, children);
}

InspectionResult inspectEnvironment(
      SEXP objectSEXP,
      const Context& context)
{
   std::cout << "Hello environment!" << std::endl;
   std::vector<InspectionResult> children;
   if (context.recursive)
   {
      // ensure we're working with primitive environment
      SEXP envirSEXP = R_NilValue;
      r::sexp::Protect protect;
      Error error;
      
      error = r::sexp::asPrimitiveEnvironment(
               objectSEXP,
               &envirSEXP,
               &protect);
      if (error)
         LOG_ERROR(error);
      
      // list keys
      SEXP namesSEXP = ::R_lsInternal(envirSEXP, TRUE);
      std::cout << "Found " << Rf_length(namesSEXP) << " keys" << std::endl;
      r::sexp::printValue(namesSEXP);
      
      // produce child context for each
      for (std::size_t i = 0, n = ::Rf_length(namesSEXP); i < n; i++)
      {
         std::string name = CHAR(STRING_ELT(namesSEXP, i));
         std::string access = string_utils::format("#[[\"%s\"]]", name.c_str());
         std::vector<std::string> tags;
         
         std::cout << "Environment key: '" << name << "'" << std::endl;
         
         Context childContext = createChildContext(context, name, access, tags);
         InspectionResult childResult = inspectObject(
                  ::Rf_findVarInFrame(envirSEXP, ::Rf_install(name.c_str())),
                  childContext);
         children.push_back(childResult);
      }
   }
   
   return createInspectionResult(objectSEXP, context, children);
}

InspectionResult inspectS4(
      SEXP objectSEXP,
      const Context& context)
{
   std::vector<InspectionResult> children;
   if (context.recursive)
   {
      // TODO
   }
   
   return createInspectionResult(objectSEXP, context, children);
}

InspectionResult inspectPairList(
      SEXP objectSEXP,
      const Context& context)
{
   std::vector<InspectionResult> children;
   
   // TODO
   return createInspectionResult(objectSEXP, context, children);
}

InspectionResult inspectFunction(
      SEXP objectSEXP,
      const Context& context)
{
   // TODO
   std::vector<InspectionResult> children;
   return createInspectionResult(objectSEXP, context, children);
}

InspectionResult inspectPrimitive(
      SEXP objectSEXP,
      const Context& context)
{
   // TODO
   std::vector<InspectionResult> children;
   return createInspectionResult(objectSEXP, context, children);
}

InspectionResult inspectDefault(
      SEXP objectSEXP,
      const Context& context)
{
   std::vector<InspectionResult> children;
   
   // allow children when this is a named atomic vector
   // be lazy and convert it to a list then inspect that
   SEXP namesSEXP = ::Rf_getAttrib(objectSEXP, R_NamesSymbol);
   int n = ::Rf_length(objectSEXP);
   if (context.recursive && n > 0 && n == ::Rf_length(namesSEXP))
   {
      SEXP listSEXP = R_NilValue;
      r::sexp::Protect protect;
      Error error = r::exec::RFunction("as.list")
            .addParam(objectSEXP)
            .call(&listSEXP, &protect);
      if (error)
         LOG_ERROR(error);
      
      if (listSEXP != R_NilValue)
      {
         InspectionResult result = inspectList(listSEXP, context);
         json::Value childrenJson = result["children"];
         if (json::isType<json::Array>(childrenJson))
         {
            BOOST_FOREACH(const json::Value& jsonValue, childrenJson.get_array())
            {
               if (json::isType<json::Object>(jsonValue))
                  children.push_back(jsonValue.get_obj());
            }
         }
      }
   }
   
   return createInspectionResult(objectSEXP, context, children);
}

InspectionResult inspectObject(SEXP objectSEXP,
                               const Context& context)
{
   // class-based dispatch
   if (::Rf_inherits(objectSEXP, "xml_node"))
      return inspectXmlNode(objectSEXP, context);
   else if (r::sexp::isEnvironment(objectSEXP))
      return inspectEnvironment(objectSEXP, context);
   else if (IS_S4_OBJECT(objectSEXP))
      return inspectS4(objectSEXP, context);
   
   // type-based dispatch
   switch (TYPEOF(objectSEXP))
   {
   case VECSXP:
   case EXPRSXP:
      return inspectList(objectSEXP, context);
   case LANGSXP:
   case LISTSXP:
      return inspectPairList(objectSEXP, context);
   case CLOSXP:
      return inspectFunction(objectSEXP, context);
   case SPECIALSXP:
   case BUILTINSXP:
      return inspectPrimitive(objectSEXP, context);
   default:
      return inspectDefault(objectSEXP, context);
   }
}

} // end anonymous namespace

core::Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   module_context::events().onDeferredInit.connect(onDeferredInit);
   module_context::events().onShutdown.connect(onShutdown);
   addSuspendHandler(SuspendHandler(onSuspend, onResume));
   
   source_database::events().onDocPendingRemove.connect(onDocPendingRemove);
   
   RS_REGISTER_CALL_METHOD(rs_objectAddress, 1);
   RS_REGISTER_CALL_METHOD(rs_objectClass, 1);
   RS_REGISTER_CALL_METHOD(rs_objectAttributes, 1);
   RS_REGISTER_CALL_METHOD(rs_explorerCacheDir, 0);
   RS_REGISTER_CALL_METHOD(rs_inspectObject, 2);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionObjectExplorer.R"))
         (bind(registerRpcMethod, "explorer_inspect_object", explorerInspectObject))
         (bind(registerRpcMethod, "explorer_begin_inspect", explorerBeginInspect));
   
   return initBlock.execute();
}
   
} // namespace explorer
} // namespace modules
} // namespace session
} // namespace rstudio
