import {
	BaseErrorListener,
	CharStream,
	CommonTokenStream,
	type Token,
} from '@making-sense/antlr4ng';
import {
	initialRule,
	Lexer,
	Parser,
} from '@making-sense/vtl-2-1-antlr-tools-ts';

export type VtlSyntaxError = {
	startLine: number;
	endLine: number;
	startCol: number;
	endCol: number;
	message: string;
};

class CollectingErrorListener extends BaseErrorListener {
	constructor(private readonly errors: VtlSyntaxError[]) {
		super();
	}

	override syntaxError(
		_recognizer: unknown,
		offendingSymbol: Token | null,
		line: number,
		column: number,
		msg: string,
	): void {
		let endColumn = column + 1;
		const text = offendingSymbol?.text;
		if (text) {
			endColumn = column + text.length;
		}
		this.errors.push({
			startLine: line,
			endLine: line,
			startCol: column,
			endCol: endColumn,
			message: msg,
		});
	}
}

/**
 * Parse VTL 2.1 source with ANTLR and return syntax errors (empty when valid).
 */
export function validateVtl(input: string): VtlSyntaxError[] {
	const errors: VtlSyntaxError[] = [];
	const chars = CharStream.fromString(input);
	const lexer = new Lexer(chars);
	lexer.removeErrorListeners();

	const tokens = new CommonTokenStream(lexer);
	const parser = new Parser(tokens);
	parser.removeErrorListeners();
	parser.addErrorListener(new CollectingErrorListener(errors));

	const parseRule = (parser as unknown as Record<string, () => unknown>)[initialRule];
	parseRule.call(parser);
	return errors;
}
