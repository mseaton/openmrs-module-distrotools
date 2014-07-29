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

package org.openmrs.module.distrotools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.distrotools.api.DistroToolsService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base abstract class that dependent modules can extend, which ensure that the included tools are run,
 * including ContentManagers in the appropriate order
 */
public class ContentManagerUtil extends BaseModuleActivator {

	protected static final Log log = LogFactory.getLog(ContentManagerUtil.class);

	/**
	 * Refresh all content managers of a particular type by priority within that type
	 */
	public static synchronized <T extends ContentManager> void refreshContentManagers(Class<T> contentManagerType) {

		long start = System.currentTimeMillis();
		log.info("Refreshing all content managers of type: " + contentManagerType.getSimpleName());

		List<T> contentManagers = Context.getRegisteredComponents(contentManagerType);
		log.info("Found " + contentManagers.size() + " to refresh");

		Collections.sort(contentManagers, new Comparator<ContentManager>() {
			@Override
			public int compare(ContentManager cm1, ContentManager cm2) {
				return cm1.getPriority() - cm2.getPriority();
			}
		});

		DistroToolsService distroToolsService = Context.getService(DistroToolsService.class);
		for (ContentManager manager : contentManagers) {
			log.info("Refreshing: " + manager.getClass() + " with priority: " + manager.getPriority());
			distroToolsService.refreshManager(manager);
		}

		long time = System.currentTimeMillis() - start;
		log.info("Refreshed content managers of type " + contentManagerType + " in " + time + "ms");
	}
}