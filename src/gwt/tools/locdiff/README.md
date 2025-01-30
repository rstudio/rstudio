# locdiff

Locdiff is a command-line utility to generate a CSV showing all English UI strings that
have been added or modified between two commit hashes of the RStudio IDE repository.

The tool compares two cloned instances of the RStudio repo; one named "old" and one "new".
You must create these yourself and checkout the desired commit hashes in each.

It also shows the most recent French translation (if any) for each string to make it easier to
identify what work is needed.

## Usage

The tool requires `node.js` on the path and works on Mac, Windows, or Linux. Recommend
node.js 22.13.1 or newer but any recent node.js should work.

```bash
cd rstudio/src/gwt/tools/locdiff
git clone git@github.com:rstudio/rstudio
mv rstudio old
git clone git@github.com:rstudio/rstudio
mv rstudio new
cd old
git checkout 9f796939
cd ../new
git checkout main
cd ..
npm i
npm start
```

Results are written to `locdiff.csv`, which can be loaded into Excel, etc.
To view the French strings in Excel, you will need to import (rather than open) the csv and
specify that it is comma-separated and stored as Unicode / UTF-8, otherwise the French strings
may be garbled.

The file will load directly into Apple Numbers if you have that handy.

### Arguments

--only-changed - writes only strings that have changed or been added

### Other Checks

Do a check of all the French property files to ensure there are no
unescaped single quote characters. Search for the regex (?<!')'(?!')
in all *._fr.properties files.
