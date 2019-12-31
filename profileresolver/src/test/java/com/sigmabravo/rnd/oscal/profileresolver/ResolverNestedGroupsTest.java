/*
 * Copyright 2019 Sigma Bravo Pty Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sigmabravo.rnd.oscal.profileresolver;

import com.sigmabravo.rnd.ismcatalogschema.Catalog;
import java.io.File;
import java.net.URL;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ResolverNestedGroupsTest extends AbstractResolverTest {

    public ResolverNestedGroupsTest() {
    }

    @Test
    public void testNestedGroupsAllProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/include-all-nested-groups-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/include-all-nested-groups-test_profile_RESOLVED.xml");
    }

    @Test
    public void testNestedGroupsCall1lProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/include-call1-nested-groups-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/include-call1-nested-groups-test_profile_RESOLVED.xml");
    }
}
