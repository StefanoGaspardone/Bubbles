export interface ReactNativeFile {
	uri: string;
	name: string;
	type: string;
}

export type UploadFile = Blob | ReactNativeFile;

export const toFormData = (fields: Record<string, UploadFile | string | number | boolean | undefined>): FormData => {
	const form = new FormData();

	for(const [name, value] of Object.entries(fields)) {
		if(value === undefined) continue;

		if(typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
			form.append(name, String(value));
		} else {
			form.append(name, value as Blob);
		}
	}

	return form;
}