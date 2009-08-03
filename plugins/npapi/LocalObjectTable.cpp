#include "mozincludes.h"
#include "LocalObjectTable.h"

LocalObjectTable::~LocalObjectTable() {
  if (!dontFree) {
    freeAll();
  }
}
