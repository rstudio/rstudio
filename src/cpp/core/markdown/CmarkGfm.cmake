# Build the pinned, unmodified cmark-gfm sources as a private static dependency.
# Do not use upstream's top-level CMake project: it installs a CLI and libraries
# and changes flags / cache options belonging to the enclosing RStudio build.
function(add_rstudio_cmark_gfm)
   set(CMARK_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/markdown/cmark-gfm")
   set(CMARK_BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/cmark-gfm")
   file(MAKE_DIRECTORY "${CMARK_BINARY_DIR}")

   set(CMARK_SOURCES
      arena.c blocks.c buffer.c cmark.c cmark_ctype.c commonmark.c footnotes.c
      houdini_href_e.c houdini_html_e.c houdini_html_u.c html.c inlines.c
      iterator.c latex.c linked_list.c man.c map.c node.c plaintext.c plugin.c
      references.c registry.c render.c scanners.c syntax_extension.c utf8.c xml.c
   )
   list(TRANSFORM CMARK_SOURCES PREPEND "${CMARK_SOURCE_DIR}/src/")
   set(CMARK_EXTENSION_SOURCES
      autolink.c core-extensions.c ext_scanners.c strikethrough.c table.c
      tagfilter.c tasklist.c
   )
   list(TRANSFORM CMARK_EXTENSION_SOURCES PREPEND "${CMARK_SOURCE_DIR}/extensions/")

   add_library(rstudio-cmark-gfm STATIC ${CMARK_SOURCES} ${CMARK_EXTENSION_SOURCES})
   set_target_properties(rstudio-cmark-gfm PROPERTIES
      C_STANDARD 99
      C_STANDARD_REQUIRED ON
      POSITION_INDEPENDENT_CODE ON
   )
   target_compile_definitions(rstudio-cmark-gfm PRIVATE
      CMARK_GFM_STATIC_DEFINE CMARK_GFM_EXTENSIONS_STATIC_DEFINE)
   target_include_directories(rstudio-cmark-gfm BEFORE PRIVATE
      "${CMARK_BINARY_DIR}"
      "${CMARK_SOURCE_DIR}/src"
      "${CMARK_SOURCE_DIR}/extensions"
   )
   # Only the adapter needs these headers. In particular, upstream utf8.h must
   # not shadow utf8cpp's header in session sources or other core consumers.
   set_property(SOURCE markdown/CmarkGfm.cpp APPEND PROPERTY INCLUDE_DIRECTORIES
      "${CMARK_BINARY_DIR}"
      "${CMARK_SOURCE_DIR}/src"
      "${CMARK_SOURCE_DIR}/extensions")
   set_property(SOURCE markdown/CmarkGfm.cpp APPEND PROPERTY COMPILE_DEFINITIONS
      CMARK_GFM_STATIC_DEFINE CMARK_GFM_EXTENSIONS_STATIC_DEFINE)

   include(GenerateExportHeader)
   generate_export_header(rstudio-cmark-gfm
      BASE_NAME cmark-gfm
      EXPORT_FILE_NAME "${CMARK_BINARY_DIR}/cmark-gfm_export.h")

   # Match the version declared in the pinned upstream CMakeLists.txt.
   set(PROJECT_VERSION_MAJOR 0)
   set(PROJECT_VERSION_MINOR 29)
   set(PROJECT_VERSION_PATCH 0)
   set(PROJECT_VERSION_GFM 13)
   configure_file("${CMARK_SOURCE_DIR}/src/cmark-gfm_version.h.in"
      "${CMARK_BINARY_DIR}/cmark-gfm_version.h" @ONLY)

   include(CheckIncludeFile)
   include(CheckCSourceCompiles)
   check_include_file(stdbool.h HAVE_STDBOOL_H)
   check_c_source_compiles("int main(void) { return __builtin_expect(0, 0); }"
      HAVE___BUILTIN_EXPECT)
   check_c_source_compiles("int f(void) __attribute__((unused)); int main(void) { return 0; }"
      HAVE___ATTRIBUTE__)
   configure_file("${CMARK_SOURCE_DIR}/src/config.h.in"
      "${CMARK_BINARY_DIR}/config.h")
endfunction()
add_rstudio_cmark_gfm()
