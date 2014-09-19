
#include <libclang/libclang.hpp>

#include <iostream>

#ifdef _WIN32
#include <windows.h>
#else
#include <dlfcn.h>
#endif

namespace rsclang {

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

bool libclang::isLoadable(const std::string& libraryPath, std::string* pError)
{
   void* pLib;
   if (loadLibrary(libraryPath, (void**)&pLib, pError))
   {
      std::string error;
      closeLibrary(pLib, &error);
      return true;
   }
   else
   {
      return false;
   }
}

libclang::libclang(const std::string& libraryPath)
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

   LOAD_CLANG_SYMBOL(getExpansionLocation)
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

   LOAD_CLANG_SYMBOL(CXIndex_setGlobalOptions)
   LOAD_CLANG_SYMBOL(CXIndex_getGlobalOptions)
   LOAD_CLANG_SYMBOL(getFileUniqueID)
   LOAD_CLANG_SYMBOL(Location_isInSystemHeader)
   LOAD_CLANG_SYMBOL(Location_isFromMainFile)
   LOAD_CLANG_SYMBOL(getFileLocation)

   LOAD_CLANG_SYMBOL(getNumDiagnosticsInSet)
   LOAD_CLANG_SYMBOL(getDiagnosticInSet)
   LOAD_CLANG_SYMBOL(loadDiagnostics)
   LOAD_CLANG_SYMBOL(disposeDiagnosticSet)
   LOAD_CLANG_SYMBOL(getChildDiagnostics)
   LOAD_CLANG_SYMBOL(getDiagnosticSetFromTU)
   LOAD_CLANG_SYMBOL(getDiagnosticCategoryText)

   LOAD_CLANG_SYMBOL(getCursorLinkage)
   LOAD_CLANG_SYMBOL(getCursorPlatformAvailability)
   LOAD_CLANG_SYMBOL(disposeCXPlatformAvailability)
   LOAD_CLANG_SYMBOL(getTypeSpelling)
   LOAD_CLANG_SYMBOL(getTypedefDeclUnderlyingType)
   LOAD_CLANG_SYMBOL(getEnumDeclIntegerType)
   LOAD_CLANG_SYMBOL(getEnumConstantDeclValue)
   LOAD_CLANG_SYMBOL(getEnumConstantDeclUnsignedValue)
   LOAD_CLANG_SYMBOL(getFieldDeclBitWidth)
   LOAD_CLANG_SYMBOL(Cursor_getNumArguments)
   LOAD_CLANG_SYMBOL(Cursor_getArgument)
   LOAD_CLANG_SYMBOL(getFunctionTypeCallingConv)
   LOAD_CLANG_SYMBOL(getNumArgTypes)
   LOAD_CLANG_SYMBOL(getArgType)
   LOAD_CLANG_SYMBOL(isFunctionTypeVariadic)
   LOAD_CLANG_SYMBOL(getElementType)
   LOAD_CLANG_SYMBOL(getNumElements)
   LOAD_CLANG_SYMBOL(Type_getAlignOf)

   LOAD_CLANG_SYMBOL(Type_getSizeOf)
   LOAD_CLANG_SYMBOL(Type_getOffsetOf)

   LOAD_CLANG_SYMBOL(Cursor_isBitField)
   LOAD_CLANG_SYMBOL(Cursor_getSpellingNameRange)
   LOAD_CLANG_SYMBOL(Cursor_getObjCSelectorIndex)
   LOAD_CLANG_SYMBOL(Cursor_isDynamicCall)
   LOAD_CLANG_SYMBOL(Cursor_getReceiverType)
   LOAD_CLANG_SYMBOL(Cursor_getObjCPropertyAttributes)
   LOAD_CLANG_SYMBOL(Cursor_getObjCDeclQualifiers)
   LOAD_CLANG_SYMBOL(Cursor_isObjCOptional)
   LOAD_CLANG_SYMBOL(Cursor_isVariadic)
   LOAD_CLANG_SYMBOL(Cursor_getCommentRange)
   LOAD_CLANG_SYMBOL(Cursor_getRawCommentText)
   LOAD_CLANG_SYMBOL(Cursor_getBriefCommentText)
   LOAD_CLANG_SYMBOL(Cursor_getModule)

   LOAD_CLANG_SYMBOL(Module_getASTFile)
   LOAD_CLANG_SYMBOL(Module_getParent)
   LOAD_CLANG_SYMBOL(Module_getName)
   LOAD_CLANG_SYMBOL(Module_getFullName)

   LOAD_CLANG_SYMBOL(Module_getNumTopLevelHeaders)
   LOAD_CLANG_SYMBOL(Module_getTopLevelHeader)
   LOAD_CLANG_SYMBOL(CXXMethod_isPureVirtual)

   LOAD_CLANG_SYMBOL(getCompletionParent)
   LOAD_CLANG_SYMBOL(getCompletionBriefComment)
   LOAD_CLANG_SYMBOL(findIncludesInFile)
   LOAD_CLANG_SYMBOL(index_isEntityObjCContainerKind)
   LOAD_CLANG_SYMBOL(index_getObjCContainerDeclInfo)
   LOAD_CLANG_SYMBOL(index_getObjCInterfaceDeclInfo)
   LOAD_CLANG_SYMBOL(index_getObjCCategoryDeclInfo)
   LOAD_CLANG_SYMBOL(index_getObjCProtocolRefListInfo)
   LOAD_CLANG_SYMBOL(index_getObjCPropertyDeclInfo)
   LOAD_CLANG_SYMBOL(index_getIBOutletCollectionAttrInfo)
   LOAD_CLANG_SYMBOL(index_getCXXClassDeclInfo)
   LOAD_CLANG_SYMBOL(index_getClientContainer)
   LOAD_CLANG_SYMBOL(index_setClientContainer)
   LOAD_CLANG_SYMBOL(index_getClientEntity)
   LOAD_CLANG_SYMBOL(index_setClientEntity)
   LOAD_CLANG_SYMBOL(IndexAction_create)
   LOAD_CLANG_SYMBOL(IndexAction_dispose)
   LOAD_CLANG_SYMBOL(indexSourceFile)
   LOAD_CLANG_SYMBOL(indexTranslationUnit)
   LOAD_CLANG_SYMBOL(indexLoc_getFileLocation)
   LOAD_CLANG_SYMBOL(indexLoc_getCXSourceLocation)
   LOAD_CLANG_SYMBOL(Cursor_getParsedComment)
   LOAD_CLANG_SYMBOL(Comment_getKind)
   LOAD_CLANG_SYMBOL(Comment_getNumChildren)
   LOAD_CLANG_SYMBOL(Comment_getChild)
   LOAD_CLANG_SYMBOL(Comment_isWhitespace)
   LOAD_CLANG_SYMBOL(InlineContentComment_hasTrailingNewline)
   LOAD_CLANG_SYMBOL(TextComment_getText)
   LOAD_CLANG_SYMBOL(InlineCommandComment_getCommandName)
   LOAD_CLANG_SYMBOL(InlineCommandComment_getRenderKind)
   LOAD_CLANG_SYMBOL(InlineCommandComment_getNumArgs)
   LOAD_CLANG_SYMBOL(InlineCommandComment_getArgText)
   LOAD_CLANG_SYMBOL(HTMLTagComment_getTagName)
   LOAD_CLANG_SYMBOL(HTMLStartTagComment_isSelfClosing)
   LOAD_CLANG_SYMBOL(HTMLStartTag_getNumAttrs)
   LOAD_CLANG_SYMBOL(HTMLStartTag_getAttrName)
   LOAD_CLANG_SYMBOL(HTMLStartTag_getAttrValue)
   LOAD_CLANG_SYMBOL(BlockCommandComment_getCommandName)
   LOAD_CLANG_SYMBOL(BlockCommandComment_getNumArgs)
   LOAD_CLANG_SYMBOL(BlockCommandComment_getArgText)
   LOAD_CLANG_SYMBOL(BlockCommandComment_getParagraph)
   LOAD_CLANG_SYMBOL(ParamCommandComment_getParamName)
   LOAD_CLANG_SYMBOL(ParamCommandComment_isParamIndexValid)
   LOAD_CLANG_SYMBOL(ParamCommandComment_getParamIndex)
   LOAD_CLANG_SYMBOL(ParamCommandComment_isDirectionExplicit)
   LOAD_CLANG_SYMBOL(ParamCommandComment_getDirection)
   LOAD_CLANG_SYMBOL(TParamCommandComment_getParamName)
   LOAD_CLANG_SYMBOL(TParamCommandComment_isParamPositionValid)
   LOAD_CLANG_SYMBOL(TParamCommandComment_getDepth)
   LOAD_CLANG_SYMBOL(TParamCommandComment_getIndex)
   LOAD_CLANG_SYMBOL(VerbatimBlockLineComment_getText)
   LOAD_CLANG_SYMBOL(VerbatimLineComment_getText)
   LOAD_CLANG_SYMBOL(HTMLTagComment_getAsString)
   LOAD_CLANG_SYMBOL(FullComment_getAsHTML)
   LOAD_CLANG_SYMBOL(FullComment_getAsXML)
   LOAD_CLANG_SYMBOL(CompilationDatabase_fromDirectory)
   LOAD_CLANG_SYMBOL(CompilationDatabase_dispose)
   LOAD_CLANG_SYMBOL(CompilationDatabase_getCompileCommands)
   LOAD_CLANG_SYMBOL(CompilationDatabase_getAllCompileCommands)
   LOAD_CLANG_SYMBOL(CompileCommands_dispose)
   LOAD_CLANG_SYMBOL(CompileCommands_getSize)
   LOAD_CLANG_SYMBOL(CompileCommands_getCommand)
   LOAD_CLANG_SYMBOL(CompileCommand_getDirectory)
   LOAD_CLANG_SYMBOL(CompileCommand_getNumArgs)
   LOAD_CLANG_SYMBOL(CompileCommand_getArg)
}

bool libclang::isLoaded(std::string* pError)
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

libclang::~libclang()
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

} // namespace rsclang
