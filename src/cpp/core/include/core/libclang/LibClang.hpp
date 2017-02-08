/*
 * LibClang.hpp
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

#ifndef CORE_LIBCLANG_LIBCLANG_HPP
#define CORE_LIBCLANG_LIBCLANG_HPP

#include <string>

#include <boost/noncopyable.hpp>
#include <boost/format.hpp>
#include <boost/function.hpp>

#include <core/Error.hpp>

#include "Diagnostic.hpp"
#include "SourceIndex.hpp"
#include "SourceLocation.hpp"
#include "SourceRange.hpp"
#include "Token.hpp"
#include "TranslationUnit.hpp"
#include "UnsavedFiles.hpp"
#include "Utils.hpp"

#include "clang-c/Index.h"
#include "clang-c/CXCompilationDatabase.h"

namespace rstudio {
namespace core {
namespace libclang {

struct LibraryVersion
{
   LibraryVersion() : versionMajor_(0), versionMinor_(0), versionPatch_(0) {}
   LibraryVersion(int versionMajor, int versionMinor, int versionPatch)
      : versionMajor_(versionMajor), versionMinor_(versionMinor), versionPatch_(versionPatch)
   {
   }


   bool empty() const { return versionMajor_ == 0; }

   int versionMajor() const { return versionMajor_; }
   int versionMinor() const { return versionMinor_; }
   int versionPatch() const { return versionPatch_; }

   bool operator<(const LibraryVersion& other) const
   {
      if (versionMajor_ == other.versionMajor_ && versionMinor_ == other.versionMinor_)
         return versionPatch_ < other.versionPatch_;
      else if (versionMajor_ == other.versionMajor_)
         return versionMinor_ < other.versionMinor_;
      else
         return versionMajor_ < other.versionMajor_;
   }

   bool operator==(const LibraryVersion& other) const
   {
      return versionMajor_ == other.versionMajor_ &&
             versionMinor_ == other.versionMinor_ &&
             versionPatch_ == other.versionPatch_;
   }

   bool operator!=(const LibraryVersion& other) const
   {
      return !(*this == other);
   }

   std::string asString() const
   {
      boost::format fmt("%1%.%2%.%3%");
      return boost::str(fmt % versionMajor_ % versionMinor_ % versionPatch_);
   }

private:
   const int versionMajor_;
   const int versionMinor_;
   const int versionPatch_;
};


struct EmbeddedLibrary
{
   bool empty() const { return ! libraryPath; }
   boost::function<std::string()> libraryPath;
   boost::function<std::vector<std::string>(const LibraryVersion&, bool)>
                                                                compileArgs;
};


class LibClang : boost::noncopyable
{
public:
   // construction/destruction (copying prohibited)
   LibClang() : pLib_(NULL) {}
   virtual ~LibClang();

   // loading
   bool load(EmbeddedLibrary embedded = EmbeddedLibrary(),
             LibraryVersion requiredVersion = LibraryVersion(3,4,0),
             std::string* pDiagnostics = NULL);

   core::Error unload();
   bool isLoaded() const { return pLib_ != NULL; }

   // version
   LibraryVersion version() const;

   // compile args required by this configuration of liblcnag
   std::vector<std::string> compileArgs(bool isCppFile) const;

   // strings
   const char * (*getCString)(CXString string);
   void (*disposeString)(CXString string);

   // indexes
   CXIndex (*createIndex)(int excludeDeclarationsFromPCH,
                          int displayDiagnostics);
   void (*disposeIndex)(CXIndex index);
   void (*CXIndex_setGlobalOptions)(CXIndex, unsigned options);
   unsigned (*CXIndex_getGlobalOptions)(CXIndex);


   // file manipulation routines
   CXString (*getFileName)(CXFile SFile);
   time_t (*getFileTime)(CXFile SFile);
   int (*getFileUniqueID)(CXFile file, CXFileUniqueID *outID);
   unsigned (*isFileMultipleIncludeGuarded)(CXTranslationUnit tu, CXFile file);
   CXFile (*getFile)(CXTranslationUnit tu, const char *file_name);

   // source locations
   CXSourceLocation (*getNullLocation)();
   unsigned (*equalLocations)(CXSourceLocation loc1, CXSourceLocation loc2);
   CXSourceLocation (*getLocation)(CXTranslationUnit tu,
                                   CXFile file,
                                   unsigned line,
                                   unsigned column);
   CXSourceLocation (*getLocationForOffset)(CXTranslationUnit tu,
                                            CXFile file,
                                            unsigned offset);

   int (*Location_isInSystemHeader)(CXSourceLocation location);
   int (*Location_isFromMainFile)(CXSourceLocation location);
   CXSourceRange (*getNullRange)(void);
   CXSourceRange (*getRange)(CXSourceLocation begin, CXSourceLocation end);
   unsigned (*equalRanges)(CXSourceRange range1, CXSourceRange range2);
   int (*Range_isNull)(CXSourceRange range);

   void (*getExpansionLocation)(CXSourceLocation location,
                                CXFile *file,
                                unsigned *line,
                                unsigned *column,
                                unsigned *offset);

   void (*getPresumedLocation)(CXSourceLocation location,
                               CXString *filename,
                               unsigned *line,
                               unsigned *column);
   void (*getInstantiationLocation)(CXSourceLocation location,
                                    CXFile *file,
                                    unsigned *line,
                                    unsigned *column,
                                    unsigned *offset);
   void (*getSpellingLocation)(CXSourceLocation location,
                               CXFile *file,
                               unsigned *line,
                               unsigned *column,
                               unsigned *offset);
   void (*getFileLocation)(CXSourceLocation location,
                           CXFile *file,
                           unsigned *line,
                           unsigned *column,
                           unsigned *offset);
   CXSourceLocation (*getRangeStart)(CXSourceRange range);
   CXSourceLocation (*getRangeEnd)(CXSourceRange range);

   // diagnostics
   unsigned (*getNumDiagnosticsInSet)(CXDiagnosticSet Diags);
   CXDiagnostic (*getDiagnosticInSet)(CXDiagnosticSet Diags,
                                      unsigned Index);
   CXDiagnosticSet (*loadDiagnostics)(const char *file,
                                      enum CXLoadDiag_Error *error,
                                      CXString *errorString);
   void (*disposeDiagnosticSet)(CXDiagnosticSet Diags);
   CXDiagnosticSet (*getChildDiagnostics)(CXDiagnostic D);
   unsigned (*getNumDiagnostics)(CXTranslationUnit Unit);
   CXDiagnostic (*getDiagnostic)(CXTranslationUnit Unit,
                                                   unsigned Index);
   CXDiagnosticSet (*getDiagnosticSetFromTU)(CXTranslationUnit Unit);
   void (*disposeDiagnostic)(CXDiagnostic Diagnostic);
   CXString (*formatDiagnostic)(CXDiagnostic Diagnostic, unsigned Options);
   unsigned (*defaultDiagnosticDisplayOptions)(void);
   enum CXDiagnosticSeverity (*getDiagnosticSeverity)(CXDiagnostic);
   CXSourceLocation (*getDiagnosticLocation)(CXDiagnostic);
   CXString (*getDiagnosticSpelling)(CXDiagnostic);
   CXString (*getDiagnosticOption)(CXDiagnostic Diag, CXString *Disable);
   unsigned (*getDiagnosticCategory)(CXDiagnostic);
   CXString (*getDiagnosticCategoryName)(unsigned Category);
   CXString (*getDiagnosticCategoryText)(CXDiagnostic);
   unsigned (*getDiagnosticNumRanges)(CXDiagnostic);
   CXSourceRange (*getDiagnosticRange)(CXDiagnostic Diagnostic, unsigned Range);
   unsigned (*getDiagnosticNumFixIts)(CXDiagnostic Diagnostic);
   CXString (*getDiagnosticFixIt)(CXDiagnostic Diagnostic,
                                  unsigned FixIt,
                                  CXSourceRange *ReplacementRange);


   // translation units
   CXString (*getTranslationUnitSpelling)(CXTranslationUnit CTUnit);

   CXTranslationUnit (*createTranslationUnitFromSourceFile)(
                                  CXIndex CIdx,
                                  const char *source_filename,
                                  int num_clang_command_line_args,
                                  const char * const *clang_command_line_args,
                                  unsigned num_unsaved_files,
                                  struct CXUnsavedFile *unsaved_files);
   CXTranslationUnit (*createTranslationUnit)(CXIndex,
                                              const char *ast_filename);
   unsigned (*defaultEditingTranslationUnitOptions)(void);
   CXTranslationUnit (*parseTranslationUnit)(
                                       CXIndex CIdx,
                                       const char *source_filename,
                                       const char * const *command_line_args,
                                       int num_command_line_args,
                                       struct CXUnsavedFile *unsaved_files,
                                       unsigned num_unsaved_files,
                                       unsigned options);
   unsigned (*defaultSaveOptions)(CXTranslationUnit TU);
   int (*saveTranslationUnit)(CXTranslationUnit TU,
                              const char *FileName,
                              unsigned options);
   void (*disposeTranslationUnit)(CXTranslationUnit Unit);
   unsigned (*defaultReparseOptions)(CXTranslationUnit TU);
   int (*reparseTranslationUnit)(CXTranslationUnit TU,
                                 unsigned num_unsaved_files,
                                 struct CXUnsavedFile *unsaved_files,
                                 unsigned options);
   const char * (*getTUResourceUsageName)(enum CXTUResourceUsageKind kind);
   CXTUResourceUsage (*getCXTUResourceUsage)(CXTranslationUnit TU);
   void (*disposeCXTUResourceUsage)(CXTUResourceUsage usage);

   // cursors
   CXCursor (*getNullCursor)(void);
   CXCursor (*getTranslationUnitCursor)(CXTranslationUnit);
   unsigned (*equalCursors)(CXCursor, CXCursor);
   int (*Cursor_isNull)(CXCursor);
   unsigned (*hashCursor)(CXCursor);
   enum CXCursorKind (*getCursorKind)(CXCursor);
   unsigned (*isDeclaration)(enum CXCursorKind);
   unsigned (*isReference)(enum CXCursorKind);
   unsigned (*isExpression)(enum CXCursorKind);
   unsigned (*isStatement)(enum CXCursorKind);
   unsigned (*isAttribute)(enum CXCursorKind);
   unsigned (*isInvalid)(enum CXCursorKind);
   unsigned (*isTranslationUnit)(enum CXCursorKind);
   unsigned (*isPreprocessing)(enum CXCursorKind);
   unsigned (*isUnexposed)(enum CXCursorKind);
   enum CXLinkageKind (*getCursorLinkage)(CXCursor cursor);
   enum CXAvailabilityKind (*getCursorAvailability)(CXCursor cursor);
   int (*getCursorPlatformAvailability)(CXCursor cursor,
                                        int *always_deprecated,
                                        CXString *deprecated_message,
                                        int *always_unavailable,
                                        CXString *unavailable_message,
                                        CXPlatformAvailability *availability,
                                        int availability_size);
   void (*disposeCXPlatformAvailability)(CXPlatformAvailability *availability);

   enum CXLanguageKind (*getCursorLanguage)(CXCursor cursor);
   CXTranslationUnit (*Cursor_getTranslationUnit)(CXCursor);
   CXCursorSet (*createCXCursorSet)();
   void (*disposeCXCursorSet)(CXCursorSet cset);
   unsigned (*CXCursorSet_contains)(CXCursorSet cset, CXCursor cursor);
   unsigned (*CXCursorSet_insert)(CXCursorSet cset, CXCursor cursor);
   CXCursor (*getCursorSemanticParent)(CXCursor cursor);
   CXCursor (*getCursorLexicalParent)(CXCursor cursor);
   void (*getOverriddenCursors)(CXCursor cursor,
                                CXCursor **overridden,
                                unsigned *num_overridden);
   void (*disposeOverriddenCursors)(CXCursor *overridden);
   CXFile (*getIncludedFile)(CXCursor cursor);

   // mapping between cursors and source code
   CXCursor (*getCursor)(CXTranslationUnit, CXSourceLocation);
   CXSourceLocation (*getCursorLocation)(CXCursor);
   CXSourceRange (*getCursorExtent)(CXCursor);

   // type information for cursors
   CXType (*getCursorType)(CXCursor C);
   CXString (*getTypeSpelling)(CXType CT);
   CXType (*getTypedefDeclUnderlyingType)(CXCursor C);
   CXType (*getEnumDeclIntegerType)(CXCursor C);
   long long (*getEnumConstantDeclValue)(CXCursor C);
   unsigned long long (*getEnumConstantDeclUnsignedValue)(CXCursor C);
   int (*getFieldDeclBitWidth)(CXCursor C);
   int (*Cursor_getNumArguments)(CXCursor C);
   CXCursor (*Cursor_getArgument)(CXCursor C, unsigned i);
   unsigned (*equalTypes)(CXType A, CXType B);
   CXType (*getCanonicalType)(CXType T);
   unsigned (*isConstQualifiedType)(CXType T);
   unsigned (*isVolatileQualifiedType)(CXType T);
   unsigned (*isRestrictQualifiedType)(CXType T);
   CXType (*getPointeeType)(CXType T);
   CXCursor (*getTypeDeclaration)(CXType T);
   CXString (*getDeclObjCTypeEncoding)(CXCursor C);
   CXString (*getTypeKindSpelling)(enum CXTypeKind K);
   enum CXCallingConv (*getFunctionTypeCallingConv)(CXType T);
   CXType (*getResultType)(CXType T);
   int (*getNumArgTypes)(CXType T);
   CXType (*getArgType)(CXType T, unsigned i);
   unsigned (*isFunctionTypeVariadic)(CXType T);
   CXType (*getCursorResultType)(CXCursor C);
   unsigned (*isPODType)(CXType T);
   CXType (*getElementType)(CXType T);
   long long (*getNumElements)(CXType T);
   CXType (*getArrayElementType)(CXType T);
   long long (*getArraySize)(CXType T);
   long long (*Type_getAlignOf)(CXType T);
   long long (*Type_getSizeOf)(CXType T);
   long long (*Type_getOffsetOf)(CXType T, const char *S);
   unsigned (*Cursor_isBitField)(CXCursor C);
   unsigned (*isVirtualBase)(CXCursor);
   enum CX_CXXAccessSpecifier (*getCXXAccessSpecifier)(CXCursor);
   unsigned (*getNumOverloadedDecls)(CXCursor cursor);
   CXCursor (*getOverloadedDecl)(CXCursor cursor, unsigned index);

   // information for attributes
   CXType (*getIBOutletCollectionType)(CXCursor);

   // traversing the AST with cursors
   unsigned (*visitChildren)(CXCursor parent,
                             CXCursorVisitor visitor,
                             CXClientData client_data);

   // cross referencing in the AST
   CXString (*getCursorUSR)(CXCursor);
   CXString (*constructUSR_ObjCClass)(const char *class_name);
   CXString (*constructUSR_ObjCCategory)(const char *class_name,
                                         const char *category_name);
   CXString (*constructUSR_ObjCProtocol)(const char *protocol_name);
   CXString (*constructUSR_ObjCIvar)(const char *name, CXString classUSR);
   CXString (*constructUSR_ObjCMethod)(const char *name,
                                       unsigned isInstanceMethod,
                                       CXString classUSR);
   CXString (*constructUSR_ObjCProperty)(const char *property,
                                         CXString classUSR);
   CXString (*getCursorSpelling)(CXCursor);
   CXSourceRange (*Cursor_getSpellingNameRange)(CXCursor,
                                                unsigned pieceIndex,
                                                unsigned options);

   CXString (*getCursorDisplayName)(CXCursor);
   CXCursor (*getCursorReferenced)(CXCursor);
   CXCursor (*getCursorDefinition)(CXCursor);
   unsigned (*isCursorDefinition)(CXCursor);
   CXCursor (*getCanonicalCursor)(CXCursor);
   int (*Cursor_getObjCSelectorIndex)(CXCursor);
   int (*Cursor_isDynamicCall)(CXCursor C);
   CXType (*Cursor_getReceiverType)(CXCursor C);
   unsigned (*Cursor_getObjCPropertyAttributes)(CXCursor C,
                                                unsigned reserved);
   unsigned (*Cursor_getObjCDeclQualifiers)(CXCursor C);
   unsigned (*Cursor_isObjCOptional)(CXCursor C);
   unsigned (*Cursor_isVariadic)(CXCursor C);
   CXSourceRange (*Cursor_getCommentRange)(CXCursor C);
   CXString (*Cursor_getRawCommentText)(CXCursor C);
   CXString (*Cursor_getBriefCommentText)(CXCursor C);
   CXModule (*Cursor_getModule)(CXCursor C);

   CXFile (*Module_getASTFile)(CXModule Module);
   CXModule (*Module_getParent)(CXModule Module);
   CXString (*Module_getName)(CXModule Module);
   CXString (*Module_getFullName)(CXModule Module);

   unsigned (*Module_getNumTopLevelHeaders)(CXTranslationUnit, CXModule Module);
   CXFile (*Module_getTopLevelHeader)(CXTranslationUnit,
                                      CXModule Module,
                                      unsigned Index);

   // C++ AST instrospection
   unsigned (*CXXMethod_isPureVirtual)(CXCursor C);
   unsigned (*CXXMethod_isStatic)(CXCursor C);
   unsigned (*CXXMethod_isVirtual)(CXCursor C);
   enum CXCursorKind (*getTemplateCursorKind)(CXCursor C);
   CXCursor (*getSpecializedCursorTemplate)(CXCursor C);
   CXSourceRange (*getCursorReferenceNameRange)(CXCursor C,
                                                unsigned NameFlags,
                                                unsigned PieceIndex);


   // Token extraction and manipulation
   CXTokenKind (*getTokenKind)(CXToken);
   CXString (*getTokenSpelling)(CXTranslationUnit, CXToken);
   CXSourceLocation (*getTokenLocation)(CXTranslationUnit,
                                                          CXToken);
   CXSourceRange (*getTokenExtent)(CXTranslationUnit, CXToken);
   void (*tokenize)(CXTranslationUnit TU,
                    CXSourceRange Range,
                    CXToken **Tokens,
                    unsigned *NumTokens);
   void (*annotateTokens)(CXTranslationUnit TU,
                          CXToken *Tokens,
                          unsigned NumTokens,
                          CXCursor *Cursors);
   void (*disposeTokens)(CXTranslationUnit TU,
                         CXToken *Tokens,
                         unsigned NumTokens);


   // debugging
   CXString (*getCursorKindSpelling)(enum CXCursorKind Kind);
   void (*getDefinitionSpellingAndExtent)(CXCursor,
                                          const char **startBuf,
                                          const char **endBuf,
                                          unsigned *startLine,
                                          unsigned *startColumn,
                                          unsigned *endLine,
                                          unsigned *endColumn);
   void (*enableStackTraces)(void);
   void (*executeOnThread)(void (*fn)(void*),
                           void *user_data,
                           unsigned stack_size);

   // code completion
   enum CXCompletionChunkKind (*getCompletionChunkKind)(
                                       CXCompletionString completion_string,
                                       unsigned chunk_number);
   CXString (*getCompletionChunkText)(CXCompletionString completion_string,
                                      unsigned chunk_number);
   CXCompletionString (*getCompletionChunkCompletionString)(
                                       CXCompletionString completion_string,
                                       unsigned chunk_number);
   unsigned (*getNumCompletionChunks)(CXCompletionString completion_string);
   unsigned (*getCompletionPriority)(CXCompletionString completion_string);
   enum CXAvailabilityKind (*getCompletionAvailability)(CXCompletionString completion_string);
   unsigned (*getCompletionNumAnnotations)(CXCompletionString completion_string);
   CXString (*getCompletionAnnotation)(CXCompletionString completion_string,
                                       unsigned annotation_number);
   CXString (*getCompletionParent)(CXCompletionString completion_string,
                                   enum CXCursorKind *kind);
   CXString (*getCompletionBriefComment)(CXCompletionString completion_string);
   CXCompletionString (*getCursorCompletionString)(CXCursor cursor);
   unsigned (*defaultCodeCompleteOptions)(void);
   CXCodeCompleteResults* (*codeCompleteAt)(
                                          CXTranslationUnit TU,
                                          const char *complete_filename,
                                          unsigned complete_line,
                                          unsigned complete_column,
                                          struct CXUnsavedFile *unsaved_files,
                                          unsigned num_unsaved_files,
                                          unsigned options);
   void (*sortCodeCompletionResults)(CXCompletionResult *Results,
                                     unsigned NumResults);
   void (*disposeCodeCompleteResults)(CXCodeCompleteResults *Results);
   unsigned (*codeCompleteGetNumDiagnostics)(CXCodeCompleteResults *Results);
   CXDiagnostic (*codeCompleteGetDiagnostic)(CXCodeCompleteResults *Results,
                                             unsigned Index);
   unsigned long long (*codeCompleteGetContexts)(CXCodeCompleteResults *Results);
   enum CXCursorKind (*codeCompleteGetContainerKind)(
                                                CXCodeCompleteResults *Results,
                                                unsigned *IsIncomplete);
   CXString (*codeCompleteGetContainerUSR)(CXCodeCompleteResults *Results);
   CXString (*codeCompleteGetObjCSelector)(CXCodeCompleteResults *Results);

   // Miscellaneous utility functions
   CXString (*getClangVersion)(void);
   void (*toggleCrashRecovery)(unsigned isEnabled);
   void (*getInclusions)(CXTranslationUnit tu,
                         CXInclusionVisitor visitor,
                         CXClientData client_data);

   // Remapping functions
   CXRemapping (*getRemappings)(const char *path);
   unsigned (*remap_getNumFiles)(CXRemapping);
   void (*remap_getFilenames)(CXRemapping,
                              unsigned index,
                              CXString *original,
                              CXString *transformed);
   void (*remap_dispose)(CXRemapping);

   // Higher level API functions
   CXResult (*findReferencesInFile)(CXCursor cursor,
                                CXFile file,
                                CXCursorAndRangeVisitor visitor);
   CXResult (*findIncludesInFile)(CXTranslationUnit TU,
                                  CXFile file,
                                  CXCursorAndRangeVisitor visitor);


   int (*index_isEntityObjCContainerKind)(CXIdxEntityKind);
   const CXIdxObjCContainerDeclInfo *
            (*index_getObjCContainerDeclInfo)(const CXIdxDeclInfo *);

   const CXIdxObjCInterfaceDeclInfo *
            (*index_getObjCInterfaceDeclInfo)(const CXIdxDeclInfo *);

   const CXIdxObjCCategoryDeclInfo *
            (*index_getObjCCategoryDeclInfo)(const CXIdxDeclInfo *);

   const CXIdxObjCProtocolRefListInfo *
            (*index_getObjCProtocolRefListInfo)(const CXIdxDeclInfo *);

   const CXIdxObjCPropertyDeclInfo *
            (*index_getObjCPropertyDeclInfo)(const CXIdxDeclInfo *);

   const CXIdxIBOutletCollectionAttrInfo *
            (*index_getIBOutletCollectionAttrInfo)(const CXIdxAttrInfo *);

   const CXIdxCXXClassDeclInfo *
            (*index_getCXXClassDeclInfo)(const CXIdxDeclInfo *);


   CXIdxClientContainer (*index_getClientContainer)(const CXIdxContainerInfo *);
   void (*index_setClientContainer)(const CXIdxContainerInfo *,CXIdxClientContainer);
   CXIdxClientEntity (*index_getClientEntity)(const CXIdxEntityInfo *);
   void (*index_setClientEntity)(const CXIdxEntityInfo *, CXIdxClientEntity);
   CXIndexAction (*IndexAction_create)(CXIndex CIdx);
   void (*IndexAction_dispose)(CXIndexAction);

   int (*indexSourceFile)(CXIndexAction,
                                CXClientData client_data,
                                IndexerCallbacks *index_callbacks,
                                unsigned index_callbacks_size,
                                unsigned index_options,
                                const char *source_filename,
                                const char * const *command_line_args,
                                int num_command_line_args,
                                struct CXUnsavedFile *unsaved_files,
                                unsigned num_unsaved_files,
                                CXTranslationUnit *out_TU,
                                unsigned TU_options);

   int (*indexTranslationUnit)(CXIndexAction,
                               CXClientData client_data,
                               IndexerCallbacks *index_callbacks,
                               unsigned index_callbacks_size,
                               unsigned index_options,
                               CXTranslationUnit);

   void (*indexLoc_getFileLocation)(CXIdxLoc loc,
                                   CXIdxClientFile *indexFile,
                                   CXFile *file,
                                   unsigned *line,
                                   unsigned *column,
                                   unsigned *offset);
   CXSourceLocation (*indexLoc_getCXSourceLocation)(CXIdxLoc loc);

   // documentation
   CXComment (*Cursor_getParsedComment)(CXCursor C);
   enum CXCommentKind (*Comment_getKind)(CXComment Comment);
   unsigned (*Comment_getNumChildren)(CXComment Comment);
   CXComment (*Comment_getChild)(CXComment Comment, unsigned ChildIdx);
   unsigned (*Comment_isWhitespace)(CXComment Comment);
   unsigned (*InlineContentComment_hasTrailingNewline)(CXComment Comment);
   CXString (*TextComment_getText)(CXComment Comment);
   CXString (*InlineCommandComment_getCommandName)(CXComment Comment);
   enum CXCommentInlineCommandRenderKind
            (*InlineCommandComment_getRenderKind)(CXComment Comment);
   unsigned (*InlineCommandComment_getNumArgs)(CXComment Comment);
   CXString (*InlineCommandComment_getArgText)(CXComment Comment,
                                                  unsigned ArgIdx);
   CXString (*HTMLTagComment_getTagName)(CXComment Comment);

   unsigned (*HTMLStartTagComment_isSelfClosing)(CXComment Comment);
   unsigned (*HTMLStartTag_getNumAttrs)(CXComment Comment);

   CXString (*HTMLStartTag_getAttrName)(CXComment Comment, unsigned AttrIdx);

   CXString (*HTMLStartTag_getAttrValue)(CXComment Comment, unsigned AttrIdx);

   CXString (*BlockCommandComment_getCommandName)(CXComment Comment);

   unsigned (*BlockCommandComment_getNumArgs)(CXComment Comment);
   CXString (*BlockCommandComment_getArgText)(CXComment Comment,
                                              unsigned ArgIdx);

   CXComment (*BlockCommandComment_getParagraph)(CXComment Comment);

   CXString (*ParamCommandComment_getParamName)(CXComment Comment);
   unsigned (*ParamCommandComment_isParamIndexValid)(CXComment Comment);
   unsigned (*ParamCommandComment_getParamIndex)(CXComment Comment);
   unsigned (*ParamCommandComment_isDirectionExplicit)(CXComment Comment);
   enum CXCommentParamPassDirection (*ParamCommandComment_getDirection)(
                                                               CXComment Comment);
   CXString (*TParamCommandComment_getParamName)(CXComment Comment);
   unsigned (*TParamCommandComment_isParamPositionValid)(CXComment Comment);
   unsigned (*TParamCommandComment_getDepth)(CXComment Comment);
   unsigned (*TParamCommandComment_getIndex)(CXComment Comment, unsigned Depth);

   CXString (*VerbatimBlockLineComment_getText)(CXComment Comment);
   CXString (*VerbatimLineComment_getText)(CXComment Comment);
   CXString (*HTMLTagComment_getAsString)(CXComment Comment);

   CXString (*FullComment_getAsHTML)(CXComment Comment);
   CXString (*FullComment_getAsXML)(CXComment Comment);


   // compilation database
   CXCompilationDatabase (*CompilationDatabase_fromDirectory)(
                                           const char *BuildDir,
                                           CXCompilationDatabase_Error *ErrorCode);
   void (*CompilationDatabase_dispose)(CXCompilationDatabase);

   CXCompileCommands (*CompilationDatabase_getCompileCommands)(
                             CXCompilationDatabase,
                             const char *CompleteFileName);
   CXCompileCommands (*CompilationDatabase_getAllCompileCommands)(CXCompilationDatabase);
   void (*CompileCommands_dispose)(CXCompileCommands);
   unsigned (*CompileCommands_getSize)(CXCompileCommands);

   CXCompileCommand (*CompileCommands_getCommand)(CXCompileCommands, unsigned I);

   CXString (*CompileCommand_getDirectory)(CXCompileCommand);

   unsigned (*CompileCommand_getNumArgs)(CXCompileCommand);
   CXString (*CompileCommand_getArg)(CXCompileCommand, unsigned I);

private:
   core::Error tryLoad(const std::string& libraryPath,
                       LibraryVersion requiredVersion);

private:
   void* pLib_;
   EmbeddedLibrary embedded_;
};

// shared instance of lib clang
LibClang& clang();

} // namespace libclang
} // namespace core
} // namespace rstudio


#endif // CORE_LIBCLANG_LIBCLANG_HPP
