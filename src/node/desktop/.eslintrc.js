module.exports = {
    'env': {
        'node': true,
        'commonjs': true,
        'es2021': true
    },
    'extends': 'eslint:recommended',
    'parser': 'babel-eslint',
    'parserOptions': {
        'ecmaVersion': 12
    },
    'rules': {
      'semi': ['error', 'always'],
      'quotes': ['error', 'single'],
      'no-unused-vars': ['error', { 'args': 'none' }],
    }
};
