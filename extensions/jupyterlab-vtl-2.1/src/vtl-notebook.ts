import type { JupyterFrontEnd } from '@jupyterlab/application';
import type { ICellModel } from '@jupyterlab/cells';
import type { NotebookPanel } from '@jupyterlab/notebook';
import { INotebookTracker } from '@jupyterlab/notebook';

import { VTL_MIME, ensureVtlMimeType } from './vtl-mime';

/** Kernel / notebook metadata we expect for Trevas VTL. */
export const VTL_LANGUAGE_INFO = {
	name: 'VTL',
	version: '2.3',
	mimetype: VTL_MIME,
	file_extension: '.vtl',
	codemirror_mode: 'vtl',
} as const;

export function isVtlLanguageInfo(info: {
	name?: string;
	mimetype?: string;
	codemirror_mode?: string | Record<string, unknown>;
} | null | undefined): boolean {
	if (!info) {
		return false;
	}
	if (info.mimetype === VTL_MIME) {
		return true;
	}
	const name = info.name?.toLowerCase();
	if (name === 'vtl') {
		return true;
	}
	const mode = info.codemirror_mode;
	if (typeof mode === 'string' && mode.toLowerCase() === 'vtl') {
		return true;
	}
	return false;
}

export function isTrevasNotebook(panel: NotebookPanel): boolean {
	const ks = panel.model?.getMetadata('kernelspec') as
		| { name?: string; language?: string; display_name?: string }
		| undefined;
	if (ks?.name === 'trevas' || ks?.language === 'vtl') {
		return true;
	}
	if (ks?.display_name?.toLowerCase().includes('trevas')) {
		return true;
	}
	if (panel.sessionContext.kernelDisplayName.toLowerCase().includes('trevas')) {
		return true;
	}
	const info = panel.model?.getMetadata('language_info');
	return isVtlLanguageInfo(info as { name?: string; mimetype?: string; codemirror_mode?: string });
}

function repairCodeCell(model: ICellModel): void {
	if (model.type !== 'code') {
		return;
	}
	ensureVtlMimeType(model);
}

export function repairNotebookCells(panel: NotebookPanel): void {
	for (const cell of panel.content.widgets) {
		repairCodeCell(cell.model);
	}
}

/** Sync notebook metadata + cell mime types for Trevas kernels. */
export function ensureTrevasNotebook(panel: NotebookPanel): void {
	if (!isTrevasNotebook(panel)) {
		return;
	}

	const nbModel = panel.model;
	if (nbModel) {
		const ks = (nbModel.getMetadata('kernelspec') ?? {}) as Record<string, unknown>;
		if (ks.language !== 'vtl' || ks.name !== 'trevas') {
			nbModel.setMetadata('kernelspec', {
				name: 'trevas',
				display_name: 'Trevas VTL',
				language: 'vtl',
				...ks,
			});
		}

		const info = (nbModel.getMetadata('language_info') ?? {}) as Record<
			string,
			unknown
		>;
		if (!isVtlLanguageInfo(info as { name?: string; mimetype?: string; codemirror_mode?: string })) {
			nbModel.setMetadata('language_info', { ...VTL_LANGUAGE_INFO, ...info });
		}
	}

	repairNotebookCells(panel);
}

async function repairNotebookFromKernel(panel: NotebookPanel): Promise<void> {
	if (isTrevasNotebook(panel)) {
		ensureTrevasNotebook(panel);
		return;
	}

	const kernel = panel.sessionContext.session?.kernel;
	if (!kernel) {
		return;
	}
	try {
		const info = await kernel.info;
		if (isVtlLanguageInfo(info.language_info)) {
			ensureTrevasNotebook(panel);
		}
	} catch {
		// Kernel not ready yet.
	}
}

function watchNotebook(panel: NotebookPanel): void {
	const onSession = () => {
		void repairNotebookFromKernel(panel);
	};

	void panel.sessionContext.ready.then(onSession);
	panel.sessionContext.sessionChanged.connect(onSession);

	panel.model?.metadataChanged.connect(() => {
		ensureTrevasNotebook(panel);
	});

	panel.content.model?.cells.changed.connect(() => {
		void repairNotebookFromKernel(panel);
	});

	// Cells may exist before kernel metadata is ready.
	ensureTrevasNotebook(panel);
}

export function registerVtlNotebookHooks(
	_app: JupyterFrontEnd,
	tracker: INotebookTracker,
): void {
	const onPanel = (panel: NotebookPanel) => {
		watchNotebook(panel);
	};

	tracker.forEach(onPanel);
	tracker.widgetAdded.connect((_sender, panel) => {
		onPanel(panel);
	});
}
