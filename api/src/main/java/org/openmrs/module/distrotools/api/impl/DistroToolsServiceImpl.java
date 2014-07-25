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

package org.openmrs.module.distrotools.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.Handler;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.distrotools.ContentManager;
import org.openmrs.module.distrotools.api.DistroToolsService;
import org.openmrs.module.distrotools.chore.Chore;
import org.openmrs.module.distrotools.metadata.bundle.MetadataBundle;
import org.openmrs.module.distrotools.metadata.bundle.Requires;
import org.openmrs.module.distrotools.metadata.handler.ObjectDeployHandler;
import org.openmrs.module.distrotools.metadata.source.ObjectSource;
import org.openmrs.module.metadatasharing.ImportConfig;
import org.openmrs.module.metadatasharing.ImportMode;
import org.openmrs.module.metadatasharing.ImportedPackage;
import org.openmrs.module.metadatasharing.MetadataSharing;
import org.openmrs.module.metadatasharing.api.MetadataSharingService;
import org.openmrs.module.metadatasharing.wrapper.PackageImporter;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the metadata deploy service
 */
public class DistroToolsServiceImpl extends BaseOpenmrsService implements DistroToolsService {

	protected static final Log log = LogFactory.getLog(DistroToolsServiceImpl.class);

	private Map<Class<? extends OpenmrsObject>, ObjectDeployHandler> handlers;

	/**
	 * Sets the object handlers, reorganising them into a map
	 * @param handlers the handler components
	 */
	@Autowired
	public void setHandlers(Set<ObjectDeployHandler> handlers) {
		this.handlers = new HashMap<Class<? extends OpenmrsObject>, ObjectDeployHandler>();

		for (ObjectDeployHandler handler : handlers) {
			Handler handlerAnnotation = handler.getClass().getAnnotation(Handler.class);
			if (handlerAnnotation != null) {
				for (Class<?> supportedClass : handlerAnnotation.supports()) {
					if (OpenmrsObject.class.isAssignableFrom(supportedClass)) {
						this.handlers.put((Class<? extends OpenmrsObject>) supportedClass, handler);
					}
					else {
						throw new APIException("Handler annotation specifies a non OpenmrsObject subclass");
					}
				}
			}
		}
	}

	/**
	 * @see DistroToolsService#installBundles(java.util.Collection)
	 */
	@Override
	public void installBundles(Collection<MetadataBundle> bundles) throws APIException {
		// Organize into map by class
		Map<Class<? extends MetadataBundle>, MetadataBundle> all = new HashMap<Class<? extends MetadataBundle>, MetadataBundle>();
		for (MetadataBundle bundle : bundles) {
			all.put(bundle.getClass(), bundle);
		}

		// Begin recursive processing
		Set<MetadataBundle> installed = new HashSet<MetadataBundle>();
		for (MetadataBundle bundle : bundles) {
			installBundle(bundle, all, installed);
		}
	}

	/**
	 * Installs a metadata bundle by recursively installing it's required bundles
	 * @param bundle the bundle
	 * @param all the map of all bundles and their ids
	 * @param installed the set of previously installed bundles
	 */
	protected void installBundle(MetadataBundle bundle, Map<Class<? extends MetadataBundle>, MetadataBundle> all, Set<MetadataBundle> installed) throws APIException {
		// Return immediately if bundle has already been installed
		if (installed.contains(bundle)) {
			return;
		}

		try {
			// Install required bundles first
			Requires requires = bundle.getClass().getAnnotation(Requires.class);
			if (requires != null) {
				for (Class<? extends MetadataBundle> requiredClass : requires.value()) {
					MetadataBundle required = all.get(requiredClass);

					if (required == null) {
						throw new RuntimeException("Can't find required bundle class " + requiredClass + " for " + bundle.getClass());
					}

					installBundle(required, all, installed);
				}
			}

			bundle.install();
			installed.add(bundle);

			Context.flushSession();
		}
		catch (Exception ex) {
			throw new APIException("Unable to install bundle " + bundle.getClass().getSimpleName(), ex);
		}
	}

	/**
	 * @see DistroToolsService#installPackage(String, ClassLoader, String)
	 */
	public boolean installPackage(String filename, ClassLoader loader, String groupUuid) throws APIException {
		Matcher matcher = Pattern.compile("[\\w/-]+-(\\d+).zip").matcher(filename);
		if (!matcher.matches()) {
			throw new APIException("Filename must match PackageNameWithNoSpaces-X.zip");
		}

		Integer version = Integer.valueOf(matcher.group(1));

		ImportedPackage installed = Context.getService(MetadataSharingService.class).getImportedPackageByGroup(groupUuid);
		if (installed != null && installed.getVersion() >= version) {
			log.info("Metadata package " + filename + " is already installed with version " + installed.getVersion());
			return false;
		}

		if (loader.getResource(filename) == null) {
			throw new APIException("Cannot load " + filename + " for group " + groupUuid);
		}

		try {
			PackageImporter metadataImporter = MetadataSharing.getInstance().newPackageImporter();
			metadataImporter.setImportConfig(ImportConfig.valueOf(ImportMode.MIRROR));
			metadataImporter.loadSerializedPackageStream(loader.getResourceAsStream(filename));
			metadataImporter.importPackage();

			log.debug("Loaded metadata package '" + filename + "'");
			return true;

		} catch (Exception ex) {
			throw new APIException("Failed to install metadata package " + filename, ex);
		}
	}

	/**
	 * @see DistroToolsService#installObject(org.openmrs.OpenmrsObject)
	 */
	@Override
	public <T extends OpenmrsObject> T installObject(T incoming) {
		ObjectDeployHandler<T> handler = getHandler(incoming);

		// Get globally unique identifier
		String identifier = handler.getIdentifier(incoming);

		if (identifier == null) {
			throw new APIException("Can't install object with no identifier");
		}

		// Look for existing by primary identifier (i.e. exact match)
		T existing = handler.fetch(identifier);

		// If no exact match, look for another existing item that should be replaced
		if (existing == null) {
			existing = handler.findAlternateMatch(incoming);
		}

		if (existing != null) {
			handler.overwrite(incoming, existing);

			return handler.save(existing);
		}
		else {
			return handler.save(incoming);
		}
	}

	/**
	 * @see DistroToolsService#installFromSource(org.openmrs.module.distrotools.metadata.source.ObjectSource)
	 */
	@Override
	public <T extends OpenmrsObject> List<T> installFromSource(ObjectSource<T> source) throws APIException {
		List<T> installed = new ArrayList<T>();
		T incoming;

		try {
			while ((incoming = source.fetchNext()) != null) {
				installed.add(installObject(incoming));
			}
			return installed;
		}
		catch (Exception ex) {
			throw new APIException("Unable to install objects from " + source.getClass().getSimpleName());
		}
	}

	/**
	 * @see DistroToolsService#uninstallObject(org.openmrs.OpenmrsObject, String)
	 */
	@Override
	public <T extends OpenmrsObject> void uninstallObject(T outgoing, String reason) {
		ObjectDeployHandler<T> handler = getHandler(outgoing);

		handler.uninstall(outgoing, reason);
	}

	/**
	 * @see DistroToolsService#fetchObject(Class, String)
	 */
	@Override
	public <T extends OpenmrsObject> T fetchObject(Class<T> clazz, String identifier) {
		ObjectDeployHandler<T> handler = getHandler(clazz);
		return handler.fetch(identifier);
	}

	/**
	 * @see DistroToolsService#saveObject(org.openmrs.OpenmrsObject)
	 */
	@Override
	public <T extends OpenmrsObject> T saveObject(T obj) {
		ObjectDeployHandler<T> handler = getHandler(obj);
		return handler.save(obj);
	}

	/**
	 * @see DistroToolsService#overwriteObject(org.openmrs.OpenmrsObject, org.openmrs.OpenmrsObject)
	 */
	@Override
	public <T extends OpenmrsObject> void overwriteObject(T source, T target) {
		ObjectDeployHandler<T> handler = getHandler(source);

		handler.overwrite(source, target);
		handler.save(target);
	}

	/**
	 * @see DistroToolsService#refreshManager(ContentManager)
	 */
	@Override
	public void refreshManager(ContentManager manager) {
		log.info("Refreshing " + manager.getClass().getName() + "...");

		long start = System.currentTimeMillis();
		manager.refresh();

		// A content manager might load a lot of stuff into Hibernate's cache
		Context.flushSession();
		Context.clearSession();

		long time = System.currentTimeMillis() - start;

		log.info("Refreshed " + manager.getClass().getName() + " in " + time + "ms");
	}

	/**
	 * @see DistroToolsService#performChore(Chore)
	 */
	@Override
	public void performChore(Chore chore) {
		log.info("Performing chore '" + chore.getId() + "'...");

		PrintWriter writer = new PrintWriter(System.out);

		long start = System.currentTimeMillis();
		chore.perform(writer);

		writer.flush();

		Context.flushSession();
		Context.clearSession();

		long time = System.currentTimeMillis() - start;

		log.info("Performed chore '" + chore.getId() + "' in " + time + "ms");

		setGlobalProperty(chore.getId() + ".done", "true");
	}

	protected void setGlobalProperty(String property, String value) {
		GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(property);
		if (gp == null) {
			gp = new GlobalProperty();
			gp.setProperty(property);
		}
		gp.setPropertyValue(value);
		Context.getAdministrationService().saveGlobalProperty(gp);
	}

	/**
	 * Convenience method to get the handler for the given object
	 * @param obj the object
	 * @return the handler
	 * @throws RuntimeException if no suitable handler exists
	 */
	protected <T extends OpenmrsObject> ObjectDeployHandler<T> getHandler(T obj) throws RuntimeException {
		return getHandler((Class<T>) obj.getClass());
	}

	/**
	 * Convenience method to get the handler for the given object class
	 * @param clazz the object class
	 * @return the handler
	 * @throws RuntimeException if no suitable handler exists
	 */
	protected <T extends OpenmrsObject> ObjectDeployHandler<T> getHandler(Class<T> clazz) throws RuntimeException {
		ObjectDeployHandler<T> handler = handlers.get(clazz);
		if (handler != null) {
			return handler;
		}

		throw new RuntimeException("No handler class found for " + clazz.getName());
	}
}