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

package org.openmrs.module.distrotools.api;

import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.distrotools.ContentManager;
import org.openmrs.module.distrotools.chore.Chore;
import org.openmrs.module.distrotools.metadata.bundle.MetadataBundle;
import org.openmrs.module.distrotools.metadata.source.ObjectSource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Service for distribution content and installation management
 */
@Transactional
public interface DistroToolsService extends OpenmrsService {

	/**
	 * Installs a collection of bundles
	 * @param bundles the bundles
	 * @throws APIException if an error occurs
	 */
	void installBundles(Collection<MetadataBundle> bundles) throws APIException;

	/**
	 * Installs a MDS package if it has not been installed yet or the installed version is out of date
	 * @param filename the package filename
	 * @param loader the class loader to use for loading the package
	 * @param groupUuid the package group UUID
	 * @return whether package was installed
	 * @throws APIException if an error occurs
	 */
	boolean installPackage(String filename, ClassLoader loader, String groupUuid) throws APIException;

	/**
	 * Installs the incoming object
	 * @param incoming the incoming object
	 * @return the installed object (can be incoming or existing)
	 */
	<T extends OpenmrsObject> T installObject(T incoming);

	/**
	 * Installs all objects from the given source
	 * @param source the object source
	 * @param <T> the object type
	 * @return the list of installed objects
	 * @throws APIException if an error occurs
	 */
	<T extends OpenmrsObject> List<T> installFromSource(ObjectSource<T> source) throws APIException;

	/**
	 * Uninstalls the given object
	 * @param outgoing the outgoing object
	 * @param reason the reason for uninstallation
	 */
	<T extends OpenmrsObject> void uninstallObject(T outgoing, String reason);

	/**
	 * Fetches an existing object if it exists
	 * @param clazz the object's class
	 * @param identifier the object's identifier
	 */
	@Transactional(readOnly = true)
	<T extends OpenmrsObject> T fetchObject(Class<T> clazz, String identifier);

	/**
	 * Saves the given object
	 * @param obj the object
	 * @return the object
	 */
	<T extends OpenmrsObject> T saveObject(T obj);

	/**
	 * Overwrites one object with another
	 * @param source the source object
	 * @param target the target object
	 */
	<T extends OpenmrsObject> void overwriteObject(T source, T target);

	/**
	 * Refreshes a content manager
	 * @param manager the manager
	 */
	void refreshManager(ContentManager manager) throws APIException;

	/**
	 * Performs the given chore
	 * @param chore the chore
	 */
	void performChore(Chore chore) throws APIException;
}