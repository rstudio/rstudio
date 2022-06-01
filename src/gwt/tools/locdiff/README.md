# locdiff

Locdiff is a command-line utility to generate a CSV showing all English UI strings that
have been added or modified between two commit hashes of the RStudio IDE repository.

The tool compares two cloned instances of the RStudio repo; one named "old" and one "new".
You must create these yourself and checkout the desired commit hashes in each.

Some useful commit hashes:

9f796939 - Juliet Rose initial release (2022.02.0+443)
8aaa5d47 - Juliet Rose patch 1 (2022.02.1+461)
8acbd38b - Juliet Rose patch 2 (2022.02.2+485)

## Usage

The tool requires `node.js` and works on Mac, Windows, or Linux. Node 16 was used
during development.

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
node locdiff
```

Results are written to `locdiff.csv`, which can be loaded into Excel, etc.