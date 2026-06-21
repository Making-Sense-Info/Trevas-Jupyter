import {
	JupyterFrontEnd,
	JupyterFrontEndPlugin,
} from '@jupyterlab/application';
import type { DocumentRegistry } from '@jupyterlab/docregistry';
import {
	EditorExtensionRegistry,
	IEditorExtensionRegistry,
	IEditorLanguageRegistry,
} from '@jupyterlab/codemirror';
import { INotebookTracker } from '@jupyterlab/notebook';

import { vtlAssignedVarHighlightForModel } from './vtl-assigned-vars';
import { vtlPlainTextClipboardForModel } from './vtl-clipboard';
import { vtlLintTheme } from './vtl-lint-theme';
import {
	looksLikeVtl,
	repairOpenVtlDocuments,
	VTL_MIME,
	watchVtlDocumentContext,
} from './vtl-mime';
import { vtlAutocompleteForModel, vtlCompletionKeymap } from './vtl-completion';
import { vtlLanguageSupport } from './vtl-language';
import packageJson from '../package.json';
import { vtlLinterForModel } from './vtl-lint';
import {
	ensureTrevasNotebook,
	registerVtlNotebookHooks,
} from './vtl-notebook';

import '../style/index.css';

const VTL_LANGUAGE = {
	name: 'vtl',
	mime: VTL_MIME,
	extensions: ['vtl'],
	aliases: ['VTL', 'Trevas VTL', 'Trevas VTL engine', 'vtl-2.1'],
} as const;

const TEXT_EDITOR_FACTORY_NAMES = ['Editor', 'editor'];

/** Set mime when content looks like VTL (helps language load before kernel metadata). */
function ensureVtlMime(model: {
	mimeType: string;
	sharedModel: { getSource: () => string };
}): void {
	if (model.mimeType !== VTL_MIME && model.mimeType === 'text/plain') {
		if (looksLikeVtl(model.sharedModel.getSource())) {
			model.mimeType = VTL_MIME;
		}
	}
}

/** Eagerly built so the language is available before any kernel handshake. */
const vtlSupport = vtlLanguageSupport();

/**
 * Editor extensions — always registered; gated at runtime via model mime/content.
 * Never return null from factory: mime may be corrected after editor creation.
 */
function registerVtlEditorExtensions(extensions: IEditorExtensionRegistry): void {
	// Lint CSS must load for every editor — independent of language compartment.
	extensions.addExtension({
		name: 'vtl-lint-theme',
		factory: () =>
			EditorExtensionRegistry.createImmutableExtension(vtlLintTheme),
	});

	extensions.addExtension({
		name: 'vtl-clipboard',
		factory: options => {
			ensureVtlMime(options.model);
			return EditorExtensionRegistry.createImmutableExtension(
				vtlPlainTextClipboardForModel(options.model),
			);
		},
	});

	extensions.addExtension({
		name: 'vtl-assigned-vars',
		factory: options => {
			const { model } = options;
			ensureVtlMime(model);
			return EditorExtensionRegistry.createImmutableExtension(
				vtlAssignedVarHighlightForModel(model),
			);
		},
	});

	extensions.addExtension({
		name: 'vtl-lint',
		factory: options => {
			ensureVtlMime(options.model);
			return EditorExtensionRegistry.createImmutableExtension(
				vtlLinterForModel(options.model),
			);
		},
	});

	extensions.addExtension({
		name: 'vtl-autocomplete',
		factory: options => {
			ensureVtlMime(options.model);
			return EditorExtensionRegistry.createImmutableExtension([
				...vtlAutocompleteForModel(options.model),
				vtlCompletionKeymap,
			]);
		},
	});
}

function registerVtlWidgetExtension(app: JupyterFrontEnd): void {
	const extension: DocumentRegistry.WidgetExtension = {
		createNew: (_widget, context) => {
			watchVtlDocumentContext(context);
		},
	};
	const registered = new Set<string>();

	const attach = (factoryName: string) => {
		const key = factoryName.toLowerCase();
		if (registered.has(key)) {
			return;
		}
		registered.add(key);
		app.docRegistry.addWidgetExtension(factoryName, extension);
	};

	for (const name of TEXT_EDITOR_FACTORY_NAMES) {
		if (app.docRegistry.getWidgetFactory(name)) {
			attach(name);
		}
	}
	for (const factory of app.docRegistry.widgetFactories()) {
		if ((factory.modelName ?? 'text') === 'text') {
			attach(factory.name);
		}
	}
}

/**
 * Register VTL 2.1 editor support for notebook cells and .vtl files.
 */
const plugin: JupyterFrontEndPlugin<void> = {
	id: 'jupyterlab-vtl-2-1:plugin',
	description: 'VTL 2.1 editor support (highlighting, validation, autocomplete)',
	autoStart: true,
	requires: [IEditorLanguageRegistry, IEditorExtensionRegistry, INotebookTracker],
	activate: (
		app: JupyterFrontEnd,
		languages: IEditorLanguageRegistry,
		extensions: IEditorExtensionRegistry,
		notebookTracker: INotebookTracker,
	) => {
		languages.addLanguage({
			...VTL_LANGUAGE,
			support: vtlSupport,
		});

		registerVtlEditorExtensions(extensions);
		registerVtlNotebookHooks(app, notebookTracker);

		app.docRegistry.addFileType(
			{
				name: 'vtl',
				displayName: 'VTL Script',
				extensions: ['.vtl'],
				mimeTypes: [VTL_MIME],
				contentType: 'file',
				fileFormat: 'text',
			},
			['Editor'],
		);

		const setupDocumentHooks = () => {
			registerVtlWidgetExtension(app);
			repairOpenVtlDocuments(app);
			notebookTracker.forEach(panel => ensureTrevasNotebook(panel));
		};

		void app.started.then(setupDocumentHooks);
		void app.restored.then(() => {
			repairOpenVtlDocuments(app);
			notebookTracker.forEach(panel => ensureTrevasNotebook(panel));
		});

		app.docRegistry.changed.connect((_, change) => {
			if (change.type === 'widgetFactory' && change.change === 'added') {
				registerVtlWidgetExtension(app);
			}
		});

		console.info(`[jupyterlab-vtl-2-1] v${packageJson.version} active`);
	},
};

export default plugin;
export { VTL_MIME } from './vtl-mime';
