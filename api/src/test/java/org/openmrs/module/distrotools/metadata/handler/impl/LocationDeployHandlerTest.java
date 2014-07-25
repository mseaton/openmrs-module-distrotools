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

package org.openmrs.module.distrotools.metadata.handler.impl;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.distrotools.api.DistroToolsService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.openmrs.module.distrotools.metadata.bundle.CoreConstructors.location;

/**
 * Tests for {@link LocationDeployHandler}
 */
public class LocationDeployHandlerTest extends BaseModuleContextSensitiveTest {

	@Autowired
	private DistroToolsService distroToolsService;

	@Test
	public void integration() {
		// Check installing new
		distroToolsService.installObject(location("New name", "New desc", "obj-uuid"));

		Location created = Context.getLocationService().getLocationByUuid("obj-uuid");
		Assert.assertThat(created.getName(), is("New name"));
		Assert.assertThat(created.getDescription(), is("New desc"));

		// Check updating existing
		distroToolsService.installObject(location("Updated name", "Updated desc", "obj-uuid"));

		Location updated = Context.getLocationService().getLocationByUuid("obj-uuid");
		Assert.assertThat(updated.getId(), is(created.getId()));
		Assert.assertThat(updated.getName(), is("Updated name"));
		Assert.assertThat(updated.getDescription(), is("Updated desc"));

		// Check uninstall retires
		distroToolsService.uninstallObject(distroToolsService.fetchObject(Location.class, "obj-uuid"), "Testing");

		Assert.assertThat(Context.getLocationService().getLocationByUuid("obj-uuid").isRetired(), is(true));

		// Check re-install unretires
		distroToolsService.installObject(location("Unretired name", "Unretired desc", "obj-uuid"));

		Location unretired = Context.getLocationService().getLocationByUuid("obj-uuid");
		Assert.assertThat(unretired.getName(), is("Unretired name"));
		Assert.assertThat(unretired.getDescription(), is("Unretired desc"));
		Assert.assertThat(unretired.isRetired(), is(false));
		Assert.assertThat(unretired.getDateRetired(), nullValue());
		Assert.assertThat(unretired.getRetiredBy(), nullValue());
		Assert.assertThat(unretired.getRetireReason(), nullValue());

		// Check everything can be persisted
		Context.flushSession();
	}
}