/**
 * VTL token categories for syntax highlighting.
 */
import { TREVAS_FUNCTION_NAMES } from './trevas-functions';
import { VTL_KEYWORDS, VTL_KEYWORD_SET } from './vtl-keywords';

const CONTROL_WORDS = new Set([
	'if', 'when', 'then', 'case', 'eval', 'return', 'apply', 'filter', 'having',
	'order', 'by', 'asc', 'desc', 'on', 'over', 'partition', 'rows', 'preceding',
	'following', 'unbounded', 'from', 'to', 'keep', 'drop', 'rename', 'check',
	'restrict', 'condition', 'conditioned', 'partial', 'with', 'using', 'start',
	'end', 'always', 'default', 'first', 'last', 'viral', 'hierarchical', 'valid',
	'invalid', 'current', 'current_date', 'range', 'between',
]);

const DEFINE_WORDS = new Set([
	'calc', 'define', 'dataset', 'ruleset', 'rule', 'structure', 'component',
	'components', 'hierarchy', 'measure', 'measures', 'valuedomain',
	'valuedomains', 'variable', 'variables', 'input', 'output', 'alterdataset',
	'custompivot', 'pivot', 'unpivot', 'map_from', 'map_to', 'maps_from',
	'maps_to', 'role', 'componentrole', 'key', 'keys', 'attribute', 'data',
	'datapoint', 'datapoint_on_valuedomains', 'datapoint_on_variables',
	'hierarchical_on_valuedomains', 'hierarchical_on_variables', 'dimension',
	'language', 'operator', 'unit', 'period_indicator', 'time_period',
	'dataset_priority', 'rule_priority', 'no_measures', 'all_measures', 'single',
	'total', 'aggregate', 'aggr', 'group', 'computed', 'points', 'point',
	'record', 'set', 'setdiff', 'symdiff', 'sub', 'replace', 'fill_time_series',
	'stock_to_flow', 'flow_to_stock', 'time_agg', 'timeshift', 'check_datapoint',
	'check_hierarchy', 'imbalance', 'cast',
]);

const TYPE_WORDS = new Set([
	'boolean', 'string', 'integer', 'number', 'float', 'date', 'time', 'duration',
	'scalar', 'list',
]);

const LOGIC_WORDS = new Set([
	'and', 'or', 'not', 'xor', 'is', 'in', 'not_in', 'exists_in', 'isnull', 'all',
	'any',
]);

const JOIN_WORDS = new Set([
	'inner_join', 'left_join', 'full_join', 'cross_join', 'union', 'intersect',
	'except', 'merge',
]);

const RESERVED = new Set<string>([
	...CONTROL_WORDS,
	...DEFINE_WORDS,
	...TYPE_WORDS,
	...LOGIC_WORDS,
	...JOIN_WORDS,
	'true',
	'false',
	'null',
]);

/** VTL standard functions (keywords that are not control/define/type/join). */
export const VTL_FUNCTION_WORDS = new Set<string>(
	VTL_KEYWORDS.map(w => w.toLowerCase()).filter(w => !RESERVED.has(w)),
);

export type VtlTokenKind =
	| 'control'
	| 'define'
	| 'type'
	| 'logic'
	| 'join'
	| 'function'
	| 'trevas'
	| 'bool'
	| 'null'
	| 'identifier';

export function classifyWord(word: string): VtlTokenKind {
	const lower = word.toLowerCase();
	if (lower === 'true' || lower === 'false') {
		return 'bool';
	}
	if (lower === 'null') {
		return 'null';
	}
	if (TREVAS_FUNCTION_NAMES.has(lower)) {
		return 'trevas';
	}
	if (CONTROL_WORDS.has(lower)) {
		return 'control';
	}
	if (DEFINE_WORDS.has(lower)) {
		return 'define';
	}
	if (TYPE_WORDS.has(lower)) {
		return 'type';
	}
	if (LOGIC_WORDS.has(lower)) {
		return 'logic';
	}
	if (JOIN_WORDS.has(lower)) {
		return 'join';
	}
	if (VTL_FUNCTION_WORDS.has(lower) || VTL_KEYWORD_SET.has(lower)) {
		return 'function';
	}
	return 'identifier';
}
