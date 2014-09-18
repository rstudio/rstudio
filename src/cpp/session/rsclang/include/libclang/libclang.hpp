#ifndef __RSCLANG_LIBCLANG_HPP__
#define __RSCLANG_LIBCLANG_HPP__

#include <string>

#include <boost/noncopyable.hpp>

#include "clang-c/Index.h"
#include "clang-c/CXCompilationDatabase.h"

namespace rsclang {

class libclang : boost::noncopyable
{
public:
   libclang(const std::string& libraryPath);
   bool isLoaded(std::string* pError);
   ~libclang();

public:

   // strings
   const char * (*getCString)(CXString string);
   void (*disposeString)(CXString string);

   // indexes
   CXIndex (*createIndex)(int excludeDeclarationsFromPCH,
                          int displayDiagnostics);
   void (*disposeIndex)(CXIndex index);

   // file manipulation routines
   CXString (*getFileName)(CXFile SFile);
   time_t (*getFileTime)(CXFile SFile);
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
   CXSourceRange (*getNullRange)(void);
   CXSourceRange (*getRange)(CXSourceLocation begin, CXSourceLocation end);
   unsigned (*equalRanges)(CXSourceRange range1, CXSourceRange range2);
   int (*Range_isNull)(CXSourceRange range);

   // Not available in Ubuntu 12.04 libclang.so. Therefore:
   //   - It's not a fatal error if a lookup for this symbol fails
   //   - Call hasGetExpansionLocation to check for availability before using
   bool hasGetExpansionLocation() const { return getExpansionLocation != NULL; }
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
   CXSourceLocation (*getRangeStart)(CXSourceRange range);
   CXSourceLocation (*getRangeEnd)(CXSourceRange range);

   // diagnostics
   unsigned (*getNumDiagnostics)(CXTranslationUnit Unit);
   CXDiagnostic (*getDiagnostic)(CXTranslationUnit Unit,
                                                   unsigned Index);
   void (*disposeDiagnostic)(CXDiagnostic Diagnostic);
   CXString (*formatDiagnostic)(CXDiagnostic Diagnostic, unsigned Options);
   unsigned (*defaultDiagnosticDisplayOptions)(void);
   enum CXDiagnosticSeverity (*getDiagnosticSeverity)(CXDiagnostic);
   CXSourceLocation (*getDiagnosticLocation)(CXDiagnostic);
   CXString (*getDiagnosticSpelling)(CXDiagnostic);
   CXString (*getDiagnosticOption)(CXDiagnostic Diag, CXString *Disable);
   unsigned (*getDiagnosticCategory)(CXDiagnostic);
   CXString (*getDiagnosticCategoryName)(unsigned Category);
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
   enum CXAvailabilityKind (*getCursorAvailability)(CXCursor cursor);
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
   unsigned (*equalTypes)(CXType A, CXType B);
   CXType (*getCanonicalType)(CXType T);
   unsigned (*isConstQualifiedType)(CXType T);
   unsigned (*isVolatileQualifiedType)(CXType T);
   unsigned (*isRestrictQualifiedType)(CXType T);
   CXType (*getPointeeType)(CXType T);
   CXCursor (*getTypeDeclaration)(CXType T);
   CXString (*getDeclObjCTypeEncoding)(CXCursor C);
   CXString (*getTypeKindSpelling)(enum CXTypeKind K);
   CXType (*getResultType)(CXType T);
   CXType (*getCursorResultType)(CXCursor C);
   unsigned (*isPODType)(CXType T);
   CXType (*getArrayElementType)(CXType T);
   long long (*getArraySize)(CXType T);
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
   CXString (*getCursorDisplayName)(CXCursor);
   CXCursor (*getCursorReferenced)(CXCursor);
   CXCursor (*getCursorDefinition)(CXCursor);
   unsigned (*isCursorDefinition)(CXCursor);
   CXCursor (*getCanonicalCursor)(CXCursor);

   // C++ AST instrospection
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
   void (*findReferencesInFile)(CXCursor cursor,
                                CXFile file,
                                CXCursorAndRangeVisitor visitor);

private:
   void* pLib_;
   std::string initError_;
};

} // namespace rsclang

#endif // __RSCLANG_LIBCLANG_HPP__
