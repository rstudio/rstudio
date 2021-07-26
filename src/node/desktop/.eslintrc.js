module.exports = {
  
  "root": true,

  "env": {
    "es2020": true,
    "node": true
  },

  "parser": '@typescript-eslint/parser',

  "parserOptions": {
    "project": "./tsconfig.json",
    "ecmaVersion": 11,
    "sourceType": "module",
  },

  "plugins": [
    "@typescript-eslint",
  ],

  "extends": [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
  ],

  "rules": {

    "indent": ["error", 2],
    "quotes": ["error", "single"],
    "semi":   ["error", "always"],

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

