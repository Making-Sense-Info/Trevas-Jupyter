/**
 * Global functions registered by the Trevas Jupyter kernel (VtlKernel).
 */
export type TrevasFunction = {
	name: string;
	signature: string;
	detail: string;
};

export const TREVAS_FUNCTIONS: TrevasFunction[] = [
	{
		name: 'loadParquet',
		signature: 'loadParquet(url)',
		detail: 'Load Parquet dataset',
	},
	{
		name: 'loadCSV',
		signature: 'loadCSV(url)',
		detail: 'Load CSV dataset',
	},
	{
		name: 'loadSas',
		signature: 'loadSas(url)',
		detail: 'Load SAS dataset',
	},
	{
		name: 'loadSDMXEmptySource',
		signature: 'loadSDMXEmptySource(sdmxMesUrl, structureId)',
		detail: 'Load SDMX empty source',
	},
	{
		name: 'loadSDMXSource',
		signature: 'loadSDMXSource(sdmxMesUrl, structureId, dataUrl)',
		detail: 'Load SDMX source',
	},
	{
		name: 'writeParquet',
		signature: 'writeParquet(url, dataset)',
		detail: 'Write dataset to Parquet',
	},
	{
		name: 'writeCSV',
		signature: 'writeCSV(url, dataset)',
		detail: 'Write dataset to CSV',
	},
	{
		name: 'show',
		signature: 'show(dataset)',
		detail: 'Display first rows of a dataset',
	},
	{
		name: 'showMetadata',
		signature: 'showMetadata(dataset)',
		detail: 'Display dataset metadata',
	},
	{
		name: 'size',
		signature: 'size(dataset)',
		detail: 'Display dataset size',
	},
	{
		name: 'getSize',
		signature: 'getSize(dataset)',
		detail: 'Display dataset size',
	},
	{
		name: 'runSDMXPreview',
		signature: 'runSDMXPreview(sdmxMesUrl)',
		detail: 'Run SDMX VTL transformations with empty datasets',
	},
	{
		name: 'runSDMX',
		signature: 'runSDMX(sdmxMesUrl, dataLocations)',
		detail: 'Run SDMX VTL transformations with sources',
	},
	{
		name: 'getTransformationsVTL',
		signature: 'getTransformationsVTL(sdmxMesUrl)',
		detail: 'Display VTL transformations from SDMX message',
	},
	{
		name: 'getRulesetsVTL',
		signature: 'getRulesetsVTL(sdmxMesUrl)',
		detail: 'Display VTL rulesets from SDMX message',
	},
];

export const TREVAS_FUNCTION_NAMES = new Set(
	TREVAS_FUNCTIONS.map(fn => fn.name.toLowerCase()),
);
