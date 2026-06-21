/**
 * VTL 2.1 reserved words (aligned with Trevas / VTL-2.1 grammar).
 */
export const VTL_KEYWORDS = [
	'abs', 'aggregate', 'aggr', 'all', 'all_measures', 'alterDataset', 'always',
	'always_null', 'always_zero', 'and', 'any', 'apply', 'as', 'asc', 'attribute',
	'avg', 'between', 'boolean', 'by', 'calc', 'case', 'cast', 'ceil', 'check',
	'check_datapoint', 'check_hierarchy', 'component', 'componentRole', 'components',
	'computed', 'condition', 'conditioned', 'count', 'cross_join', 'current',
	'current_date', 'customPivot', 'data', 'datapoint', 'datapoint_on_valuedomains',
	'datapoint_on_variables', 'dataset', 'dataset_priority', 'date', 'dateadd',
	'datediff', 'dayofmonth', 'dayofyear', 'daytomonth', 'daytoyear', 'default',
	'define', 'desc', 'diff', 'dimension', 'drop', 'duration', 'end', 'errorcode',
	'errorlevel', 'eval', 'except', 'exists_in', 'ex', 'exp', 'fill_time_series',
	'filter', 'first', 'first_value', 'float', 'floor', 'flow_to_stock', 'following',
	'from', 'full_join', 'group', 'having', 'hierarchical', 'hierarchical_on_valuedomains',
	'hierarchical_on_variables', 'hierarchy', 'if', 'imbalance', 'in', 'inner_join',
	'input', 'instr', 'intday', 'integer', 'intmonth', 'intyear', 'intersect',
	'invalid', 'is', 'isnull', 'keep', 'key', 'keys', 'lag', 'language', 'last',
	'last_value', 'lead', 'left_join', 'length', 'levenshtein', 'list', 'ln', 'log',
	'lower', 'map_from', 'map_to', 'maps_from', 'maps_to', 'max', 'maxLength',
	'measure', 'measures', 'median', 'merge', 'min', 'mod', 'monthtoday',
	'no_measures', 'non_null', 'non_zero', 'not', 'not_in', 'null', 'number', 'nvl',
	'on', 'operator', 'or', 'order', 'output', 'over', 'partial', 'partial_null',
	'partial_zero', 'partition', 'period_indicator', 'pivot', 'point', 'points', 'power',
	'preceding', 'random', 'range', 'rank', 'ratio_to_report', 'record', 'regexp',
	'rename', 'replace', 'restrict', 'return', 'role', 'round', 'rows', 'rule',
	'rule_priority', 'ruleset', 'scalar', 'set', 'setdiff', 'single', 'sqrt',
	'stddev_pop', 'stddev_samp', 'stock_to_flow', 'string', 'structure', 'sub',
	'substr', 'sum', 'symdiff', 'then', 'time', 'time_agg', 'time_period', 'timeshift',
	'to', 'total', 'trim', 'trunc', 'type', 'union', 'unit', 'unpivot', 'unbounded',
	'upper', 'using', 'valid', 'value', 'valuedomain', 'valuedomains', 'var_pop',
	'var_samp', 'variable', 'variables', 'viral', 'when', 'with', 'xor', 'yeartoday',
] as const;

export const VTL_KEYWORD_SET = new Set<string>(
	VTL_KEYWORDS.map(keyword => keyword.toLowerCase()),
);
