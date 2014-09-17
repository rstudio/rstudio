#include "Clang.hpp"

#include <iostream>

#ifdef _WIN32
#include <windows.h>
#else
#include <dlfcn.h>
#endif

namespace {


#ifdef _WIN32

std::string getLastErrorMessage(const std::string& context)
{
   LPVOID lpMsgBuf;
   DWORD dw = ::GetLastError();

   DWORD length = ::FormatMessage(
       FORMAT_MESSAGE_ALLOCATE_BUFFER |
       FORMAT_MESSAGE_FROM_SYSTEM |
       FORMAT_MESSAGE_IGNORE_INSERTS,
       NULL,
       dw,
       MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
       (LPTSTR) &lpMsgBuf,
       0, NULL );

   if (length != 0)
   {
      std::string msg((LPTSTR)lpMsgBuf);
      msg.append(" (" + context + ")");
      LocalFree(lpMsgBuf);
      return msg;
   }
   else
   {
     return "Unknown error " + context;
   }
}

bool loadLibrary(const std::string& libPath, void** ppLib, std::string* pError)
{
   *ppLib = NULL;
   *ppLib = (void*)::LoadLibraryEx(libPath.c_str(), NULL, 0);
   if (*ppLib == NULL)
   {
      *pError = getLastErrorMessage("loading library " + libPath);
      return false;
   }
   else
   {
      return true;
   }
}

bool loadSymbol(void* pLib,
                const std::string& name,
                void** ppSymbol,
                std::string* pError)
{
   *ppSymbol = NULL;
   *ppSymbol = (void*)::GetProcAddress((HINSTANCE)pLib, name.c_str());
   if (*ppSymbol == NULL)
   {
      *pError = getLastErrorMessage("loading symbol " + name);
      return false;
   }
   else
   {
      return true;
   }
}

bool closeLibrary(void* pLib, std::string* pError)
{
   if (!::FreeLibrary((HMODULE)pLib))
   {
      *pError = getLastErrorMessage("closing library");
      return false;
   }
   else
   {
      return true;
   }
}

#else

std::string getLastDlerror(const std::string& context)
{
   const char* msg = ::dlerror();
   if (msg != NULL)
      return std::string(msg);
   else
      return "Unknown error " + context;
}

bool loadLibrary(const std::string& libPath, void** ppLib, std::string* pError)
{
   *ppLib = NULL;
   *ppLib = ::dlopen(libPath.c_str(), RTLD_NOW);
   if (*ppLib == NULL)
   {
      *pError = getLastDlerror("loading library " + libPath);
      return false;
   }
   else
    {
      return true;
   }
}

bool loadSymbol(void* pLib,
                const std::string& name,
                void** ppSymbol,
                std::string* pError)
{
   *ppSymbol = NULL;
   *ppSymbol = ::dlsym(pLib, name.c_str());
   if (*ppSymbol == NULL)
   {
      *pError = getLastDlerror("loading symbol " + name);
      return false;
   }
   else
   {
      return true;
   }
}

bool closeLibrary(void* pLib, std::string* pError)
{
   if (::dlclose(pLib) != 0)
   {
      *pError = getLastDlerror("closing library");
      return false;
   }
   else
   {
      return true;
   }
}

#endif


} // anonymous namespace

#define LOAD_CLANG_SYMBOL(name) \
   if (!loadSymbol(pLib_, "clang_" #name, (void**)&name, &initError_)) \
       return;

LibClang::LibClang(const std::string& libraryPath)
   : pLib_(NULL)
{
   if (!loadLibrary(libraryPath, (void**)&pLib_, &initError_))
       return;

   LOAD_CLANG_SYMBOL(getCString)
   LOAD_CLANG_SYMBOL(disposeString)

   LOAD_CLANG_SYMBOL(createIndex)
   LOAD_CLANG_SYMBOL(disposeIndex)

   LOAD_CLANG_SYMBOL(getFileName)
   LOAD_CLANG_SYMBOL(getFileTime)
   LOAD_CLANG_SYMBOL(isFileMultipleIncludeGuarded)
   LOAD_CLANG_SYMBOL(getFile)

   LOAD_CLANG_SYMBOL(getNullLocation)
   LOAD_CLANG_SYMBOL(equalLocations)
   LOAD_CLANG_SYMBOL(getLocation)
   LOAD_CLANG_SYMBOL(getLocationForOffset)
   LOAD_CLANG_SYMBOL(getNullRange)
   LOAD_CLANG_SYMBOL(getRange)
   LOAD_CLANG_SYMBOL(equalRanges)
   LOAD_CLANG_SYMBOL(Range_isNull)

   // not a fatal error if clang_getExpansionLocation isn't found
   // (it isn't exported from libclang.so in Ubuntu 12.04)
   std::string ignoredError;
   if (!loadSymbol(pLib_,
                   "clang_getExpansionLocation",
                   (void**)&getExpansionLocation,
                    &ignoredError))
   {
      getExpansionLocation = NULL;
   }

   LOAD_CLANG_SYMBOL(getPresumedLocation)
   LOAD_CLANG_SYMBOL(getInstantiationLocation)
   LOAD_CLANG_SYMBOL(getSpellingLocation)
   LOAD_CLANG_SYMBOL(getRangeStart)
   LOAD_CLANG_SYMBOL(getRangeEnd)

   LOAD_CLANG_SYMBOL(getNumDiagnostics)
   LOAD_CLANG_SYMBOL(getDiagnostic)
   LOAD_CLANG_SYMBOL(disposeDiagnostic)
   LOAD_CLANG_SYMBOL(formatDiagnostic)
   LOAD_CLANG_SYMBOL(defaultDiagnosticDisplayOptions)
   LOAD_CLANG_SYMBOL(getDiagnosticSeverity)
   LOAD_CLANG_SYMBOL(getDiagnosticLocation)
   LOAD_CLANG_SYMBOL(getDiagnosticSpelling)
   LOAD_CLANG_SYMBOL(getDiagnosticOption)
   LOAD_CLANG_SYMBOL(getDiagnosticCategory)
   LOAD_CLANG_SYMBOL(getDiagnosticCategoryName)
   LOAD_CLANG_SYMBOL(getDiagnosticNumRanges)
   LOAD_CLANG_SYMBOL(getDiagnosticRange)
   LOAD_CLANG_SYMBOL(getDiagnosticNumFixIts)
   LOAD_CLANG_SYMBOL(getDiagnosticFixIt)

   LOAD_CLANG_SYMBOL(getTranslationUnitSpelling)
   LOAD_CLANG_SYMBOL(createTranslationUnitFromSourceFile)
   LOAD_CLANG_SYMBOL(createTranslationUnit)
   LOAD_CLANG_SYMBOL(defaultEditingTranslationUnitOptions)
   LOAD_CLANG_SYMBOL(parseTranslationUnit)
   LOAD_CLANG_SYMBOL(defaultSaveOptions)
   LOAD_CLANG_SYMBOL(saveTranslationUnit)
   LOAD_CLANG_SYMBOL(disposeTranslationUnit)
   LOAD_CLANG_SYMBOL(defaultReparseOptions)
   LOAD_CLANG_SYMBOL(reparseTranslationUnit)
   LOAD_CLANG_SYMBOL(getTUResourceUsageName)
   LOAD_CLANG_SYMBOL(getCXTUResourceUsage)
   LOAD_CLANG_SYMBOL(disposeCXTUResourceUsage)

   LOAD_CLANG_SYMBOL(getNullCursor)
   LOAD_CLANG_SYMBOL(getTranslationUnitCursor)
   LOAD_CLANG_SYMBOL(equalCursors)
   LOAD_CLANG_SYMBOL(Cursor_isNull)
   LOAD_CLANG_SYMBOL(hashCursor)
   LOAD_CLANG_SYMBOL(getCursorKind)
   LOAD_CLANG_SYMBOL(isDeclaration)
   LOAD_CLANG_SYMBOL(isReference)
   LOAD_CLANG_SYMBOL(isExpression)
   LOAD_CLANG_SYMBOL(isStatement)
   LOAD_CLANG_SYMBOL(isAttribute)
   LOAD_CLANG_SYMBOL(isInvalid)
   LOAD_CLANG_SYMBOL(isTranslationUnit)
   LOAD_CLANG_SYMBOL(isPreprocessing)
   LOAD_CLANG_SYMBOL(isUnexposed)

   LOAD_CLANG_SYMBOL(getCursorAvailability)
   LOAD_CLANG_SYMBOL(getCursorLanguage)
   LOAD_CLANG_SYMBOL(Cursor_getTranslationUnit)
   LOAD_CLANG_SYMBOL(createCXCursorSet)
   LOAD_CLANG_SYMBOL(disposeCXCursorSet)
   LOAD_CLANG_SYMBOL(CXCursorSet_contains)
   LOAD_CLANG_SYMBOL(CXCursorSet_insert)
   LOAD_CLANG_SYMBOL(getCursorSemanticParent)
   LOAD_CLANG_SYMBOL(getCursorLexicalParent)
   LOAD_CLANG_SYMBOL(getOverriddenCursors)
   LOAD_CLANG_SYMBOL(disposeOverriddenCursors)
   LOAD_CLANG_SYMBOL(getIncludedFile)

   LOAD_CLANG_SYMBOL(getCursor)
   LOAD_CLANG_SYMBOL(getCursorLocation)
   LOAD_CLANG_SYMBOL(getCursorExtent)

   LOAD_CLANG_SYMBOL(getCursorType)
   LOAD_CLANG_SYMBOL(equalTypes)
   LOAD_CLANG_SYMBOL(getCanonicalType)
   LOAD_CLANG_SYMBOL(isConstQualifiedType)
   LOAD_CLANG_SYMBOL(isVolatileQualifiedType)
   LOAD_CLANG_SYMBOL(isRestrictQualifiedType)
   LOAD_CLANG_SYMBOL(getPointeeType)
   LOAD_CLANG_SYMBOL(getTypeDeclaration)
   LOAD_CLANG_SYMBOL(getDeclObjCTypeEncoding)
   LOAD_CLANG_SYMBOL(getTypeKindSpelling)
   LOAD_CLANG_SYMBOL(getResultType)
   LOAD_CLANG_SYMBOL(getCursorResultType)
   LOAD_CLANG_SYMBOL(isPODType)
   LOAD_CLANG_SYMBOL(getArrayElementType)
   LOAD_CLANG_SYMBOL(getArraySize)
   LOAD_CLANG_SYMBOL(isVirtualBase)

   LOAD_CLANG_SYMBOL(getCXXAccessSpecifier)
   LOAD_CLANG_SYMBOL(getNumOverloadedDecls)
   LOAD_CLANG_SYMBOL(getOverloadedDecl)

   LOAD_CLANG_SYMBOL(getIBOutletCollectionType)

   LOAD_CLANG_SYMBOL(visitChildren)

   LOAD_CLANG_SYMBOL(getCursorUSR)
   LOAD_CLANG_SYMBOL(constructUSR_ObjCClass)
   LOAD_CLANG_SYMBOL(constructUSR_ObjCCategory)
   LOAD_CLANG_SYMBOL(constructUSR_ObjCProtocol)
   LOAD_CLANG_SYMBOL(constructUSR_ObjCIvar)
   LOAD_CLANG_SYMBOL(constructUSR_ObjCMethod)
   LOAD_CLANG_SYMBOL(constructUSR_ObjCProperty)
   LOAD_CLANG_SYMBOL(getCursorSpelling)
   LOAD_CLANG_SYMBOL(getCursorDisplayName)
   LOAD_CLANG_SYMBOL(getCursorReferenced)
   LOAD_CLANG_SYMBOL(getCursorDefinition)
   LOAD_CLANG_SYMBOL(isCursorDefinition)
   LOAD_CLANG_SYMBOL(getCanonicalCursor)

   LOAD_CLANG_SYMBOL(CXXMethod_isStatic)
   LOAD_CLANG_SYMBOL(CXXMethod_isVirtual)
   LOAD_CLANG_SYMBOL(getTemplateCursorKind)
   LOAD_CLANG_SYMBOL(getSpecializedCursorTemplate)
   LOAD_CLANG_SYMBOL(getCursorReferenceNameRange)

   LOAD_CLANG_SYMBOL(getTokenKind)
   LOAD_CLANG_SYMBOL(getTokenSpelling)
   LOAD_CLANG_SYMBOL(getTokenLocation)
   LOAD_CLANG_SYMBOL(getTokenExtent)
   LOAD_CLANG_SYMBOL(tokenize)
   LOAD_CLANG_SYMBOL(annotateTokens)
   LOAD_CLANG_SYMBOL(disposeTokens)

   LOAD_CLANG_SYMBOL(getCursorKindSpelling)
   LOAD_CLANG_SYMBOL(getDefinitionSpellingAndExtent)
   LOAD_CLANG_SYMBOL(enableStackTraces)
   LOAD_CLANG_SYMBOL(executeOnThread)

   LOAD_CLANG_SYMBOL(getCompletionChunkKind)
   LOAD_CLANG_SYMBOL(getCompletionChunkText)
   LOAD_CLANG_SYMBOL(getCompletionChunkCompletionString)
   LOAD_CLANG_SYMBOL(getNumCompletionChunks)
   LOAD_CLANG_SYMBOL(getCompletionPriority)
   LOAD_CLANG_SYMBOL(getCompletionAvailability)
   LOAD_CLANG_SYMBOL(getCompletionNumAnnotations)
   LOAD_CLANG_SYMBOL(getCompletionAnnotation)
   LOAD_CLANG_SYMBOL(getCursorCompletionString)

   LOAD_CLANG_SYMBOL(defaultCodeCompleteOptions)
   LOAD_CLANG_SYMBOL(codeCompleteAt)
   LOAD_CLANG_SYMBOL(sortCodeCompletionResults)
   LOAD_CLANG_SYMBOL(disposeCodeCompleteResults)
   LOAD_CLANG_SYMBOL(codeCompleteGetNumDiagnostics)
   LOAD_CLANG_SYMBOL(codeCompleteGetDiagnostic)
   LOAD_CLANG_SYMBOL(codeCompleteGetContexts)
   LOAD_CLANG_SYMBOL(codeCompleteGetContainerKind)
   LOAD_CLANG_SYMBOL(codeCompleteGetContainerUSR)
   LOAD_CLANG_SYMBOL(codeCompleteGetObjCSelector)

   LOAD_CLANG_SYMBOL(getClangVersion)
   LOAD_CLANG_SYMBOL(toggleCrashRecovery)
   LOAD_CLANG_SYMBOL(getInclusions)

   LOAD_CLANG_SYMBOL(getRemappings)
   LOAD_CLANG_SYMBOL(remap_getNumFiles)
   LOAD_CLANG_SYMBOL(remap_getFilenames)
   LOAD_CLANG_SYMBOL(remap_dispose)

   LOAD_CLANG_SYMBOL(findReferencesInFile)
}

bool LibClang::isLoaded(std::string* pError)
{
   if (initError_.empty())
   {
      return true;
   }
   else
   {
      *pError = initError_;
      return false;
   }
}

LibClang::~LibClang()
{
   try
   {
      if (pLib_ != NULL)
      {
         std::string errorMessage;
         if (!closeLibrary(pLib_, &errorMessage))
            std::cerr << errorMessage << std::endl;
      }
   }
   catch(...)
   {
   }
}
