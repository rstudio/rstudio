module.exports = {
  
  "root": true,

  "env": {
    "es2020": true,
    "node": true
  },

  "parser": "@typescript-eslint/parser",

  "parserOptions": {
    "project": "./tsconfig.json",
    "ecmaVersion": 11,
    "sourceType": "module",
  },

  "plugins": [
    "@typescript-eslint",
  ],

  "extends": [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
  ],

  "rules": {

    "indent": ["error", 2],
    "quotes": ["error", "single"],
    "semi":   ["error", "always"],

    "@typescript-eslint/await-thenable": ["error"],
    "@typescript-eslint/no-base-to-string": ["error"],
    "@typescript-eslint/no-confusing-non-null-assertion": ["error"],
    "@typescript-eslint/no-floating-promises": ["warn"],
    "@typescript-eslint/no-invalid-void-type": ["error"],
    "@typescript-eslint/no-misused-promises": ["error"],
    "@typescript-eslint/no-throw-literal": ["error"],
    "@typescript-eslint/no-unnecessary-condition": ["error"],
    "@typescript-eslint/promise-function-async": ["warn"],
    "@typescript-eslint/require-array-sort-compare": ["error"],
    "@typescript-eslint/return-await": ["warn"],
    "@typescript-eslint/no-implicit-any-catch": ["error"],

    "@typescript-eslint/strict-boolean-expressions": [
      "error", {
        "allowString": true,
        "allowNumber": true,
        "allowNullableObject": true,
        "allowNullableBoolean": true,
        "allowNullableString": true,
        "allowNullableNumber": true,
      }
    ],

  }
  
};
