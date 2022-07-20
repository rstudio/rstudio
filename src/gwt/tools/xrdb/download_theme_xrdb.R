#!/usr/bin/env Rscript

themes <- read.delim("theme_urls.tsv", sep = "\t")
themes <- themes[!is.na(themes$url), ]

for (i in seq_len(nrow(themes))) {
    download.file(themes$url[i], themes$filename[i], quiet = TRUE)
}

# fix some invisible colors
patches <- list(
    cobalt = c(Ansi_0_Color = "#4f4f4f"), 
    dracula = c(Ansi_0_Color = "#4f4f4f"),
    # just here for the record, but material needs manual updating
    material = c(Ansi_0_Color = "#4f4f4f"), 
    monokai = c(Ansi_0_Color = "#4f4f4f"), 
    pastel_on_dark = c(Ansi_8_Color = "#7c7c7c"),
    solarized_dark = c(Ansi_0_Color = "#4f4f4f", Ansi_8_Color = "#7c7c7c"), 
    solarized_light = c(Ansi_7_Color = "#d3d7cf", Ansi_15_Color = "#d3d7cf"),
    tomorrow = c(Ansi_7_Color = "#d3d7cf", Ansi_15_Color = "#eeeeec"),
    tomorrow_night = c(Ansi_0_Color = "#4f4f4f", Ansi_8_Color = "#7c7c7c"), 
    tomorrow_night_blue = c(Ansi_0_Color = "#4f4f4f"), 
    tomorrow_night_bright = c(Ansi_0_Color = "#4f4f4f", Ansi_8_Color = "#7c7c7c"),
    tomorrow_night_eighties = c(Ansi_0_Color = "#4f4f4f"),
    twilight = c(Ansi_0_Color = "#4f4f4f", Ansi_8_Color = "#7c7c7c"), 
    vibrant_ink = c(Ansi_8_Color = "#7c7c7c")
)

for (i in seq_along(patches)) {
    filename <- paste0(names(patches)[i], ".xrdb")
    txt <- readLines(filename)

    patch <- patches[[i]]
    for (j in seq_along(patch)) {
        name <- names(patch)[j]
        color <- patch[j]

        line <- grep(paste("#define", name), txt)
        txt[line] <- paste("#define", name, color)
    }
    writeLines(txt, filename)
}


