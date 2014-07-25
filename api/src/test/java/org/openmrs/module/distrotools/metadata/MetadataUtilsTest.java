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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.VisitType;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link MetadataUtils}
 */
public class MetadataUtilsTest extends BaseModuleContextSensitiveTest {

	private static final String NONEXISTENT_UUID = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"; // Valid syntactically

	@Test
	public void integration() {
		new MetadataUtils();
	}

	/**
	 * @see MetadataUtils#existing(Class, String)
	 */
	@Test
	public void existing_shouldFetchExisting() {
		VisitType initial = Context.getVisitService().getVisitTypeByUuid("c0c579b0-8e59-401d-8a4a-976a0b183519");
		Assert.assertThat(MetadataUtils.existing(VisitType.class, "c0c579b0-8e59-401d-8a4a-976a0b183519"), is(initial));
	}

	/**
	 * @see MetadataUtils#existing(Class, String)
	 */
	@Test(expected = MissingMetadataException.class)
	public void existing_shouldThrowExceptionForNonExistent() {
		MetadataUtils.existing(VisitType.class, NONEXISTENT_UUID);
	}

	/**
	 * @see MetadataUtils#possible(Class, String)
	 */
	@Test
	public void possible_shouldFetchExisting() {
		VisitType initial = Context.getVisitService().getVisitTypeByUuid("c0c579b0-8e59-401d-8a4a-976a0b183519");
		Assert.assertThat(MetadataUtils.possible(VisitType.class, "c0c579b0-8e59-401d-8a4a-976a0b183519"), is(initial));
	}

	/**
	 * @see MetadataUtils#existing(Class, String)
	 */
	@Test
	public void possible_shouldReturnNullForNonExistent() {
		Assert.assertThat(MetadataUtils.possible(VisitType.class, NONEXISTENT_UUID), nullValue());
	}

	/**
	 * @see MetadataUtils#isValidUuid(String)
	 */
	@Test
	public void isValidUuid_shouldCheckForValidUuids() {
		Assert.assertThat(MetadataUtils.isValidUuid(null), is(false));
		Assert.assertThat(MetadataUtils.isValidUuid(""), is(false));
		Assert.assertThat(MetadataUtils.isValidUuid("xxxx-xxxxx"), is(false));

		Assert.assertThat(MetadataUtils.isValidUuid(NONEXISTENT_UUID), is(true));
		Assert.assertThat(MetadataUtils.isValidUuid("c0c579b0-8e59-401d-8a4a-976a0b183519"), is(true));
	}
}