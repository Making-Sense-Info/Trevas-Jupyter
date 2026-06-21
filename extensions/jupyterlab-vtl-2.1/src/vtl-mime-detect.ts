import { PathExt } from '@jupyterlab/coreutils';

export const VTL_MIME = 'text/x-vtl';

export function isVtlPath(path: string): boolean {
	return PathExt.extname(path).toLowerCase() === '.vtl';
}

export function isVtlModel(model: { mimeType: string }): boolean {
	return model.mimeType === VTL_MIME;
}

/** Heuristic when mime is not yet set (editor created before notebook repair). */
export function looksLikeVtl(text: string): boolean {
	return (
		/\b(calc|define\s+dataset|select|from|where|keep|drop)\b/i.test(text) ||
		text.includes(':=')
	);
}

/** Runtime gate for editor extensions — survives late mimeType changes. */
export function isVtlEditorActive(model: {
	mimeType: string;
	sharedModel: { getSource: () => string };
}): boolean {
	if (isVtlModel(model)) {
		return true;
	}
	return looksLikeVtl(model.sharedModel.getSource());
}
