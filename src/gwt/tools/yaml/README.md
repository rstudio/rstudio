# YAML package

Use this to create a minified version of the `yaml` package (from `npm`).

The `yaml` package version is pinned in `package.json` so should only need to re-run this if
taking a new version.

```
npm i
npm run package
```

This will create/update `src/gwt/www/js/yaml.min.js`.
