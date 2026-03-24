#!/usr/bin/env bash
#
# r-api-report.sh
#
# Reports which R C API symbols used by RStudio are not part of R's public API.
#
# The public API is determined by @apifun, @eapifun, @apivar, and @eapivar
# annotations in R-exts.texi. The embedding API (@embfun, @embvar) and
# formerly-API symbols (@forfun) are reported separately.
#
# By default, fetches the latest R-exts.texi and R headers from
# https://github.com/r-devel/r-svn (trunk branch).
#
# Usage:
#   ./scripts/r-api-report.sh

set -uo pipefail

RSTUDIO_CPP="$(cd "$(dirname "$0")/.." && pwd)/src/cpp"

if [ ! -d "$RSTUDIO_CPP" ]; then
  echo "Error: $RSTUDIO_CPP not found" >&2
  exit 1
fi

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

R_SVN_BASE="https://raw.githubusercontent.com/r-devel/r-svn/trunk"

# Step 0: Fetch R sources from r-devel/r-svn ----

echo "Fetching R-exts.texi from r-devel/r-svn..." >&2
curl -sfL "$R_SVN_BASE/doc/manual/R-exts.texi" -o "$TMPDIR/R-exts.texi"
if [ ! -s "$TMPDIR/R-exts.texi" ]; then
  echo "Error: failed to download R-exts.texi" >&2
  exit 1
fi
TEXI="$TMPDIR/R-exts.texi"

echo "Fetching R headers from r-devel/r-svn..." >&2
R_INCLUDE="$TMPDIR/r-include"
mkdir -p "$R_INCLUDE"

R_HEADERS=(
  Rinternals.h
  Rinterface.h
  Rembedded.h
  Rdefines.h
  R_ext/Applic.h
  R_ext/Boolean.h
  R_ext/Callbacks.h
  R_ext/Complex.h
  R_ext/Connections.h
  R_ext/Error.h
  R_ext/GraphicsDevice.h
  R_ext/GraphicsEngine.h
  R_ext/Memory.h
  R_ext/Parse.h
  R_ext/Print.h
  R_ext/RS.h
  R_ext/RStartup.h
  R_ext/Random.h
  R_ext/Rdynload.h
  R_ext/Riconv.h
  R_ext/Utils.h
  R_ext/eventloop.h
  R_ext/libextern.h
)

mkdir -p "$R_INCLUDE/R_ext"
for header in "${R_HEADERS[@]}"; do
  curl -sfL "$R_SVN_BASE/src/include/$header" -o "$R_INCLUDE/$header" &
done
wait

# Step 1: Extract API symbols from R-exts.texi ----

grep -oE '@(apifun|eapifun|apivar|eapivar) [A-Za-z_][A-Za-z0-9_]*' "$TEXI" \
  | sed 's/@[a-z]* //' | sort -u > "$TMPDIR/public_api.txt"

# Treat symbols exported by the graphics engine/device headers as public API
for gfx_header in "$R_INCLUDE/R_ext/GraphicsEngine.h" "$R_INCLUDE/R_ext/GraphicsDevice.h"; do
  if [ -f "$gfx_header" ]; then
    grep -oE '\b(R_|Rf_|GE)[A-Za-z_][A-Za-z0-9_]*' "$gfx_header" 2>/dev/null
  fi
done | sort -u >> "$TMPDIR/public_api.txt"
sort -u -o "$TMPDIR/public_api.txt" "$TMPDIR/public_api.txt"

grep -oE '@(embfun|embvar) [A-Za-z_][A-Za-z0-9_]*' "$TEXI" \
  | sed 's/@[a-z]* //' | sort -u > "$TMPDIR/embed_api.txt"

grep -oE '@forfun [A-Za-z_][A-Za-z0-9_]*' "$TEXI" \
  | sed 's/@forfun //' | sort -u > "$TMPDIR/former_api.txt"

# Embedding-related symbols that are documented in the embedding chapter
# but not annotated with @embfun/@embvar
EMBED_EXTRAS=(
  Rf_initialize_R Rf_mainloop R_Suicide R_Consolefile R_Outputfile
  R_CStackStart R_running_as_main_program R_DirtyImage R_HomeDir
  R_Quiet R_NoEcho R_Verbose R_SignalHandlers
  R_SaveGlobalEnvToFile R_RestoreGlobalEnvFromFile
)
for sym in "${EMBED_EXTRAS[@]}"; do
  echo "$sym"
done >> "$TMPDIR/embed_api.txt"
sort -u -o "$TMPDIR/embed_api.txt" "$TMPDIR/embed_api.txt"

# Step 2: Extract R C API symbols used by RStudio ----

# Files to exclude from scanning (e.g. internal API blocklists)
EXCLUDE="--exclude=RInternal.hpp"

# Rf_* and R_* identifiers
grep -rohE $EXCLUDE '\bRf_[a-zA-Z_][a-zA-Z0-9_]*' "$RSTUDIO_CPP" 2>/dev/null | sort -u > "$TMPDIR/rf_calls.txt"
grep -rohE $EXCLUDE '\bR_[a-zA-Z][a-zA-Z0-9_]*' "$RSTUDIO_CPP" 2>/dev/null | sort -u > "$TMPDIR/r_calls.txt"

# R macros (uppercase identifiers from Rinternals.h)
R_MACROS='ALTREP|ATTRIB|BODY|CAAR|CADR|CADDR|CADDDR|CAD4R|CAD5R|CAR|CDAR|CDDDR|CDDR|CDR|CHAR|CLEAR_ATTRIB|CLOENV|COMPLEX|COMPLEX_ELT|DATAPTR|DATAPTR_OR_NULL|DATAPTR_RO|DUPLICATE_ATTRIB|ENCLOS|FORMALS|FRAME|HASHTAB|INTEGER|INTEGER_ELT|INTERNAL|IS_SCALAR|IS_SIMPLE_SCALAR|LENGTH|LEVELS|LOGICAL|LOGICAL_ELT|MARK_NOT_MUTABLE|MISSING|NAMED|OBJECT|PRCODE|PRENV|PRINTNAME|PROTECT|PROTECT_WITH_INDEX|PRVALUE|RAW|RAW_ELT|REAL|REAL_ELT|REAL_RO|INTEGER_RO|LOGICAL_RO|COMPLEX_RO|RAW_RO|SETCAR|SETCDR|SETCADR|SETCADDR|SETCADDDR|SETCAD4R|SETLEVELS|SET_ATTRIB|SET_COMPLEX_ELT|SET_INTEGER_ELT|SET_LOGICAL_ELT|SET_MISSING|SET_NAMED|SET_RAW_ELT|SET_REAL_ELT|SET_S4_OBJECT|SET_STRING_ELT|SET_TAG|SET_TYPEOF|SET_VECTOR_ELT|SHALLOW_DUPLICATE_ATTRIB|STRING_ELT|SYMVALUE|TAG|TYPEOF|UNPROTECT|VECTOR_ELT|XLENGTH'
grep -rohE $EXCLUDE "\b($R_MACROS)[[:space:]]*\(" "$RSTUDIO_CPP" 2>/dev/null | sed -E 's/[[:space:]]*\($//' | sort -u > "$TMPDIR/macros.txt"

# Combine all symbols
cat "$TMPDIR/rf_calls.txt" "$TMPDIR/r_calls.txt" "$TMPDIR/macros.txt" | sort -u > "$TMPDIR/all_symbols_raw.txt"

# Filter R_/Rf_ symbols to only those that appear in R's headers
while read -r sym; do
  if [[ "$sym" == R_* ]] || [[ "$sym" == Rf_* ]]; then
    if grep -rqw "$sym" "$R_INCLUDE" 2>/dev/null; then
      echo "$sym"
    fi
  else
    echo "$sym"
  fi
done < "$TMPDIR/all_symbols_raw.txt" > "$TMPDIR/all_symbols_filtered.txt"

# Remove types, header guards, and preprocessor macros (not functions/variables)
grep -vE '^(R_xlen_t|R_SIZE_T|R_ARCH|R_HOME|R_ext|R_INTERFACE_PTRS|R_INTERNALS_H_|R_NO_REMAP|R_USE_PROTOTYPES|R_USE_SIGNALS|SEXP|R_CFinalizer_t|R_CallMethodDef|R_ObjectTable|R_INCLUDE_|R_LEN_T|R_INLINE)$' \
  "$TMPDIR/all_symbols_filtered.txt" > "$TMPDIR/all_symbols.txt"

# Step 3: Classify each symbol ----

is_in_list() {
  grep -qx "$1" "$2" 2>/dev/null
}

: > "$TMPDIR/result_public.txt"
: > "$TMPDIR/result_embed.txt"
: > "$TMPDIR/result_former.txt"
: > "$TMPDIR/result_nonapi.txt"

while read -r sym; do
  short="${sym#Rf_}"

  # Check public API (exact match or Rf_ prefix stripped)
  if is_in_list "$sym" "$TMPDIR/public_api.txt" || is_in_list "$short" "$TMPDIR/public_api.txt"; then
    echo "$sym" >> "$TMPDIR/result_public.txt"
  # Check embedding API
  elif is_in_list "$sym" "$TMPDIR/embed_api.txt" || is_in_list "$short" "$TMPDIR/embed_api.txt"; then
    echo "$sym" >> "$TMPDIR/result_embed.txt"
  # Check former API
  elif is_in_list "$sym" "$TMPDIR/former_api.txt" || is_in_list "$short" "$TMPDIR/former_api.txt"; then
    echo "$sym" >> "$TMPDIR/result_former.txt"
  else
    echo "$sym" >> "$TMPDIR/result_nonapi.txt"
  fi
done < "$TMPDIR/all_symbols.txt"

# Step 4: Write report ----

OUTFILE="$(cd "$(dirname "$0")" && pwd)/r-api-report.txt"

{

echo "R API Compliance Report"
echo "============================================================"
echo ""
echo "Generated: $(date -u '+%Y-%m-%d')"
echo "Source:    $R_SVN_BASE/doc/manual/R-exts.texi"
echo "Scanned:  src/cpp/ (excluding RInternal.hpp)"
echo ""
echo "Public API symbols used:    $(wc -l < "$TMPDIR/result_public.txt" | tr -d ' ')"
echo "Embedding API symbols used: $(wc -l < "$TMPDIR/result_embed.txt" | tr -d ' ')"
if [ -s "$TMPDIR/result_former.txt" ]; then
  echo "Former API symbols used:    $(wc -l < "$TMPDIR/result_former.txt" | tr -d ' ')"
fi
echo "Non-API symbols used:       $(wc -l < "$TMPDIR/result_nonapi.txt" | tr -d ' ')"
echo ""

if [ -s "$TMPDIR/result_nonapi.txt" ]; then
  echo "Non-Public API Symbols"
  echo "------------------------------------------------------------"
  echo ""
  printf "%-35s %s\n" "Symbol" "Uses"
  printf "%-35s %s\n" "------" "----"
  while read -r sym; do
    count=$(grep -rw $EXCLUDE "$sym" "$RSTUDIO_CPP" 2>/dev/null | grep -vE ':[[:space:]]*//' | grep -vE ':[[:space:]]*\*' | wc -l | tr -d ' ')
    if [ "$count" -gt 0 ]; then
      printf "%-35s %s\n" "$sym" "$count"
    fi
  done < "$TMPDIR/result_nonapi.txt"
  echo ""
fi

if [ -s "$TMPDIR/result_embed.txt" ]; then
  echo "Embedding API Symbols"
  echo "------------------------------------------------------------"
  echo "(Documented in the \"Embedding R\" chapter, separate from public C API)"
  echo ""
  printf "%-35s %s\n" "Symbol" "Uses"
  printf "%-35s %s\n" "------" "----"
  while read -r sym; do
    count=$(grep -rw $EXCLUDE "$sym" "$RSTUDIO_CPP" 2>/dev/null | grep -vE ':[[:space:]]*//' | grep -vE ':[[:space:]]*\*' | wc -l | tr -d ' ')
    if [ "$count" -gt 0 ]; then
      printf "%-35s %s\n" "$sym" "$count"
    fi
  done < "$TMPDIR/result_embed.txt"
  echo ""
fi

if [ -s "$TMPDIR/result_former.txt" ]; then
  echo "Former API Symbols"
  echo "------------------------------------------------------------"
  echo ""
  while read -r sym; do echo "  $sym"; done < "$TMPDIR/result_former.txt"
  echo ""
fi

} > "$OUTFILE"

echo "Report written to $OUTFILE" >&2
