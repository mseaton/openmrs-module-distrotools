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
import org.openmrs.EncounterRole;
import org.openmrs.api.context.Context;
import org.openmrs.module.distrotools.api.DistroToolsService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.openmrs.module.distrotools.metadata.bundle.CoreConstructors.encounterRole;

/**
 * Tests for {@link EncounterRoleDeployHandler}
 */
public class EncounterRoleDeployHandlerTest extends BaseModuleContextSensitiveTest {

	@Autowired
	private DistroToolsService distroToolsService;

	/**
	 * Tests use of handler for installation
	 */
	@Test
	public void integration() {
		// Check installing new
		distroToolsService.installObject(encounterRole("New name", "New desc", "obj-uuid"));

		EncounterRole created = Context.getEncounterService().getEncounterRoleByUuid("obj-uuid");
		Assert.assertThat(created.getName(), is("New name"));
		Assert.assertThat(created.getDescription(), is("New desc"));

		// Check updating existing
		distroToolsService.installObject(encounterRole("Updated name", "Updated desc", "obj-uuid"));

		EncounterRole updated = Context.getEncounterService().getEncounterRoleByUuid("obj-uuid");
		Assert.assertThat(updated.getId(), is(created.getId()));
		Assert.assertThat(updated.getName(), is("Updated name"));
		Assert.assertThat(updated.getDescription(), is("Updated desc"));

		// Check uninstall retires
		distroToolsService.uninstallObject(distroToolsService.fetchObject(EncounterRole.class, "obj-uuid"), "Testing");

		Assert.assertThat(Context.getEncounterService().getEncounterRoleByUuid("obj-uuid").isRetired(), is(true));

		// Check re-install unretires
		distroToolsService.installObject(encounterRole("Unretired name", "Unretired desc", "obj-uuid"));

		EncounterRole unretired = Context.getEncounterService().getEncounterRoleByUuid("obj-uuid");
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