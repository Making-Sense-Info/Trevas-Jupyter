/**
 * VTL syntax palette — single source of truth for all editor colors.
 * Applied via vtl-theme.ts / vtl-lint-theme.ts; do not duplicate hex in CSS.
 */
export const VTL_COLORS = {
	comment: '#1A7526',
	string: '#DB463B',
	quotedId: '#98C379',
	number: '#359C65',
	bool: '#1B2B23',
	null: '#1543E8',
	keyword: '#1543E8',
	type: '#E5C07B',
	function: '#06207D',
	operator: '#06207D',
	punct: '#000000',
	identifier: '#661260',
	assignedVar: '#BA0929',
	/** ANTLR syntax errors — squiggles, gutter, tooltips (vtl-lint-theme.ts). */
	error: '#D11',
} as const;
