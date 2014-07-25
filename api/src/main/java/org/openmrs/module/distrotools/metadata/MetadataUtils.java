/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.distrotools.metadata;

import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.distrotools.api.DistroToolsService;

/**
 * Utility methods for fetching of metadata outside of a bundle.
 */
public class MetadataUtils {

	/**
	 * Fetches an object which is assumed to exist
	 * @param clazz the object class
	 * @param identifier the object identifier
	 * @return the object
	 * @throws org.openmrs.module.distrotools.metadata.MissingMetadataException if object doesn't exist
	 */
	public static <T extends OpenmrsObject> T existing(Class<T> clazz, String identifier) {
		T ret = Context.getService(DistroToolsService.class).fetchObject(clazz, identifier);
		if (ret == null) {
			throw new MissingMetadataException(clazz, identifier);
		}
		return ret;
	}

	/**
	 * Fetches an object which may or may not exist
	 * @param clazz the object class
	 * @param identifier the object identifier
	 * @return the object or null
	 */
	public static <T extends OpenmrsObject> T possible(Class<T> clazz, String identifier) {
		return Context.getService(DistroToolsService.class).fetchObject(clazz, identifier);
	}

	/**
	 * Determines if the passed string is in valid UUID format By OpenMRS standards, a UUID must be
	 * 36 characters in length and not contain whitespace, but we do not enforce that a uuid be in
	 * the "canonical" form, with alphanumerics separated by dashes, since the MVP dictionary does
	 * not use this format.
	 */
	protected static boolean isValidUuid(String uuid) {
		return uuid != null && uuid.length() == 36 && !uuid.contains(" ");
	}
}