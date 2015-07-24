/*
 * stan_highlight_rules.js
 *
 * Copyright (C) 2015 by RStudio, Inc.
 *
 * The Initial Developer of the Original Code is Jeffrey Arnold
 * Portions created by the Initial Developer are Copyright (C) 2014
 * the Initial Developer. All Rights Reserved.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

define("mode/stan_highlight_rules", function(require, exports, module) {

var oop = require("ace/lib/oop");
var lang = require("ace/lib/lang");
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

var StanHighlightRules = function() {

    // For auto-completions. Extracted with:
    // :r !R --vanilla --slave -e 'cat(unique(rstan:::rosetta$Stan))' + some
    // tidying up thereafter
    var functionList = "abs|acosh|acos|append_col|append_row|asinh|asin|atan2|atanh|atan|bernoulli_ccdf_log|bernoulli_cdf|bernoulli_cdf_log|bernoulli_log|bernoulli_logit_log|bernoulli_logit|bernoulli|bernoulli_rng|bessel_first_kind|bessel_second_kind|beta_binomial_ccdf_log|beta_binomial_cdf|beta_binomial_cdf_log|beta_binomial_log|beta_binomial|beta_binomial_rng|beta_ccdf_log|beta_cdf_log|beta_cdf|beta_log|beta|beta_rng|binary_log_loss|binomial_ccdf_log|binomial_cdf|binomial_cdf_log|binomial_coefficient_log|binomial_log|binomial_logit_log|binomial_logit|binomial|binomial_rng|block|categorical_log|categorical_logit_log|categorical_logit|categorical|categorical_rng|cauchy_ccdf_log|cauchy_cdf_log|cauchy_cdf|cauchy_log|cauchy|cauchy_rng|cbrt|ceil|chi_square_ccdf_log|chi_square_cdf_log|chi_square_cdf|chi_square_log|chi_square|chi_square_rng|cholesky_decompose|col|cols|columns_dot_product|columns_dot_self|cosh|cos|crossprod|cumulative_sum|determinant|diag_matrix|diagonal|diag_post_multiply|diag_pre_multiply|digamma|dims|dirichlet_log|dirichlet|dirichlet_rng|distance|dot_product|dot_self|double_exponential_ccdf_log|double_exponential_cdf_log|double_exponential_cdf|double_exponential_log|double_exponential|double_exponential_rng|eigenvalues_sym|eigenvectors_sym|e|erfc|erf|exp2|expm1|exp|exp_mod_normal_ccdf_log|exp_mod_normal_cdf_log|exp_mod_normal_cdf|exp_mod_normal_log|exp_mod_normal|exp_mod_normal_rng|exponential_ccdf_log|exponential_cdf_log|exponential_cdf|exponential_log|exponential|exponential_rng|fabs|falling_factorial|fdim|floor|fma|fmax|fmin|fmod|frechet_ccdf_log|frechet_cdf_log|frechet_cdf|frechet_log|frechet|frechet_rng|gamma_ccdf_log|gamma_cdf_log|gamma_cdf|gamma_log|gamma_p|gamma_q|gamma|gamma_rng|gaussian_dlm_obs_log|gaussian_dlm_obs|get_lp|gumbel_ccdf_log|gumbel_cdf_log|gumbel_cdf|gumbel_log|gumbel|gumbel_rng|head|hypergeometric_log|hypergeometric|hypergeometric_rng|hypot|if_else|increment_log_prob|int_step|inv_chi_square_ccdf_log|inv_chi_square_cdf_log|inv_chi_square_cdf|inv_chi_square_log|inv_chi_square|inv_chi_square_rng|inv_cloglog|inverse|inverse_spd|inv_gamma_ccdf_log|inv_gamma_cdf_log|inv_gamma_cdf|inv_gamma_log|inv_gamma|inv_gamma_rng|inv_logit|inv|inv_sqrt|inv_square|inv_wishart_log|inv_wishart|inv_wishart_rng|is_inf|is_nan|lbeta|lgamma|lkj_corr_cholesky_log|lkj_corr_cholesky|lkj_corr_cholesky_rng|lkj_corr_log|lkj_corr|lkj_corr_rng|lmgamma|log10|log1m_exp|log1m_inv_logit|log1m|log1p_exp|log1p|log2|log_determinant|log_diff_exp|log_falling_factorial|log_inv_logit|logistic_ccdf_log|logistic_cdf_log|logistic_cdf|logistic_log|logistic|logistic_rng|logit|log|log_mix|lognormal_ccdf_log|lognormal_cdf_log|lognormal_cdf|lognormal_log|lognormal|lognormal_rng|log_rising_factorial|log_softmax|log_sum_exp|machine_precision|max|mdivide_left_tri_low|mdivide_right_tri_low|mean|min|modified_bessel_first_kind|modified_bessel_second_kind|multi_gp_cholesky_log|multi_gp_cholesky|multi_gp_log|multi_gp|multinomial_log|multinomial|multinomial_rng|multi_normal_cholesky_log|multi_normal_cholesky|multi_normal_cholesky_rng|multi_normal_log|multi_normal_prec_log|multi_normal_prec|multi_normal|multi_normal_rng|multiply_log|multiply_lower_tri_self_transpose|multi_student_t_log|multi_student_t|multi_student_t_rng|negative_infinity|neg_binomial_2_ccdf_log|neg_binomial_2_cdf|neg_binomial_2_cdf_log|neg_binomial_2_log|neg_binomial_2_log_log|neg_binomial_2_log_rng|neg_binomial_2|neg_binomial_2_rng|neg_binomial_ccdf_log|neg_binomial_cdf|neg_binomial_cdf_log|neg_binomial_log|neg_binomial|neg_binomial_rng|normal_ccdf_log|normal_cdf_log|normal_cdf|normal_log|normal|normal_rng|not_a_number|num_elements|ordered_logistic_log|ordered_logistic|ordered_logistic_rng|owens_t|pareto_ccdf_log|pareto_cdf_log|pareto_cdf|pareto_log|pareto|pareto_rng|pareto_type_2_ccdf_log|pareto_type_2_cdf_log|pareto_type_2_cdf|pareto_type_2_log|pareto_type_2|pareto_type_2_rng|Phi_approx|Phi|pi|poisson_ccdf_log|poisson_cdf|poisson_cdf_log|poisson_log|poisson_log_log|poisson_log_rng|poisson|poisson_rng|positive_infinity|pow|print|prod|qr_Q|qr_R|quad_form_diag|quad_form|quad_form_sym|rank|rayleigh_ccdf_log|rayleigh_cdf_log|rayleigh_cdf|rayleigh_log|rayleigh|rayleigh_rng|rep_array|rep_matrix|rep_row_vector|rep_vector|rising_factorial|round|row|rows_dot_product|rows_dot_self|rows|scaled_inv_chi_square_ccdf_log|scaled_inv_chi_square_cdf_log|scaled_inv_chi_square_cdf|scaled_inv_chi_square_log|scaled_inv_chi_square|scaled_inv_chi_square_rng|sd|segment|singular_values|sinh|sin|size|skew_normal_ccdf_log|skew_normal_cdf_log|skew_normal_cdf|skew_normal_log|skew_normal|skew_normal_rng|softmax|sort_asc|sort_desc|sort_indices_asc|sort_indices_desc|sqrt2|sqrt|squared_distance|square|step|student_t_ccdf_log|student_t_cdf_log|student_t_cdf|student_t_log|student_t|student_t_rng|sub_col|sub_row|sum|tail|tanh|tan|tcrossprod|tgamma|to_array_1d|to_array_2d|to_matrix|to_row_vector|to_vector|trace_gen_quad_form|trace|trace_quad_form|trigamma|trunc|uniform_ccdf_log|uniform_cdf_log|uniform_cdf|uniform_log|uniform|uniform_rng|variance|von_mises_log|von_mises|von_mises_rng|weibull_ccdf_log|weibull_cdf_log|weibull_cdf|weibull_log|weibull|weibull_rng|wishart_log|wishart|wishart_rng"

    var variableName = "[a-zA-Z$][a-zA-Z0-9_$]*\\b";

    var keywords = "for|in|while|if|then|else|return|lower|upper";

    // technically keywords, differentiate from regular functions
    var keywordFunctions = "print|increment_log_prob|integrate_ode|reject";
    var keywordFunctionsMap = lang.arrayToMap(keywordFunctions.split("|"));

    var storageType = "int|real|vector|simplex|unit_vector|ordered|" +
	"positive_ordered|row_vector|" +
	"matrix|cholesky_factor_cov|cholesky_factor_corr|" +
	"corr_matrix|cov_matrix|" +
	"void";

    var variableLanguage = "lp__";

    var keywordMapper = this.createKeywordMapper({
	"keyword": keywords + "|" + keywordFunctions,
	"storage.type": storageType,
	"variable.language": variableLanguage,
	"function": functionList
    }, "identifier", false);

    this.$rules = {
	"start" : [
	    {
		token : "comment",
		regex : "\\/\\/.*$"
	    }, {
		token : "comment",
		regex : "#.*$"
	    }, {
		token : "comment", // multi line comment
		merge : true,
		regex : "\\/\\*",
		next : "comment"
	    }, {
		token : "keyword.blockid",
		regex : "functions|data|transformed\\s+data|parameters|" +
			 "transformed\\s+parameters|model|generated\\s+quantities"
	    }, {
		token : "string", // single line
		regex : '["][^"]*["]'
	    }, {
		token : "constant.numeric",
		regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b"
	    }, {
		token : "keyword.operator", // truncation
		regex : "\\bT(?=\\s*\\[)"
	    }, {
		// highlights everything that looks like a function
		token : function(value) {
		    if (keywordFunctionsMap.hasOwnProperty(value)) {
			return "keyword"
		    } else {
			return "support.function"
		    }
		},
		regex : variableName + "(?=\\s*\\()"
	    }, {
		token : keywordMapper,
		regex : variableName + "\\b"
	    }, {
		token : "keyword.operator",
		regex : "<-|~"
	    }, {
		token : "keyword.operator",
		regex : "\\|\\||&&|==|!=|<=?|>=?|\\+|-|\\.?\\*|\\.?/|\\\\|\\^|!|'|%"
	    }, {
		token : "punctuation.operator",
		regex : ":|,|;|="
	    }, {
		token : "paren.lparen.keyword.operator",
		regex : "[\\[\\(\\{]"
	    }, {
		token : "paren.rparen.keyword.operator",
		regex : "[\\]\\)\\}]"
	    }, {
		token : "text",
		regex : "\\s+"
	    }
	],
	"comment" : [
	    {
		token : "comment", // closing comment
		regex : ".*?\\*\\/",
		next : "start"
	    }, {
		token : "comment", // comment spanning whole line
		merge : true,
		regex : ".+"
	    }
	]
    };
};

oop.inherits(StanHighlightRules, TextHighlightRules);

exports.StanHighlightRules = StanHighlightRules;
});
