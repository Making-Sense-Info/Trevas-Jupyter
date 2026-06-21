import type { CodeEditor } from '@jupyterlab/codeeditor';
import { MainAreaWidget } from '@jupyterlab/apputils';
import type { DocumentRegistry, IDocumentWidget } from '@jupyterlab/docregistry';
import type { JupyterFrontEnd } from '@jupyterlab/application';
import type { Widget } from '@lumino/widgets';

export {
	VTL_MIME,
	isVtlEditorActive,
	isVtlModel,
	isVtlPath,
	looksLikeVtl,
} from './vtl-mime-detect';
import { VTL_MIME, isVtlPath } from './vtl-mime-detect';

/**
 * Ensure a code model uses the VTL mime type (triggers language reload).
 */
export function ensureVtlMimeType(model: CodeEditor.IModel): void {
	if (model.mimeType !== VTL_MIME) {
		model.mimeType = VTL_MIME;
	}
}

export function repairVtlDocumentContext(
	context: DocumentRegistry.IContext<DocumentRegistry.IModel>,
): void {
	if (!isVtlPath(context.path)) {
		return;
	}
	const model = context.model as DocumentRegistry.ICodeModel;
	ensureVtlMimeType(model);
}

function documentWidgetFrom(widget: Widget): IDocumentWidget | null {
	if (widget instanceof MainAreaWidget) {
		return documentWidgetFrom(widget.content);
	}
	const doc = widget as IDocumentWidget;
	return doc.context ? doc : null;
}

function repairDocumentWidget(widget: Widget): void {
	const doc = documentWidgetFrom(widget);
	if (!doc?.context) {
		return;
	}
	repairVtlDocumentContext(doc.context);
}

/**
 * Fix mime types for restored or already-open .vtl editor tabs.
 */
export function repairOpenVtlDocuments(app: JupyterFrontEnd): void {
	for (const area of ['main', 'left', 'right', 'bottom'] as const) {
		for (const widget of app.shell.widgets(area)) {
			repairDocumentWidget(widget);
		}
	}
}

/**
 * Watch a document context and keep the VTL mime type in sync (e.g. Save As .vtl).
 */
export function watchVtlDocumentContext(
	context: DocumentRegistry.IContext<DocumentRegistry.IModel>,
): void {
	const onPathChange = () => repairVtlDocumentContext(context);
	repairVtlDocumentContext(context);
	context.pathChanged.connect(onPathChange);
}
