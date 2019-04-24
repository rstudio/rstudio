#!/usr/bin/env Rscript

require(jsonlite)

# Extract prefs from JSON schema
schema <- jsonlite::read_json("../../cpp/session/resources/prefs/user-prefs-schema.json")
prefs <- schema$properties

for (pref in names(prefs)) {
   # Convert the preference name from camel case to snake case
   camel <- gsub("_(.)", "\\U\\1\\E", pref, perl = TRUE)
   def <- prefs[[pref]]
   cat(paste0(
      "/**\n",
      " * ", def[["description"]], "\n",
      " */\n",
      "public PrefValue<", def[["type"]], "> ", camel, "()\n",
      "{\n",
      "   return ", def[["type"]], "(\"", pref, "\");\n",
      "}\n\n"))
   
   # Emit convenient enum constants if supplied
}