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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ResolverTest extends AbstractResolverTest {
    
    public ResolverTest() {
    }
    
    @Test
    public void testBrokenProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/broken_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/broken_profile_RESOLVED.xml");
    }
    
    @Test
    public void testCircularProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/circular_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/circular_profile_RESOLVED.xml");
    }

    @Test
    public void testExcludeCallProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/exclude-call-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/exclude-call-test_profile_RESOLVED.xml");
    }

    // TODO: write resolution code for import of profile.
    /*
    @Test
    public void testHomeProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/home_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/home_profile_RESOLVED.xml");
    }
    */
    // TODO: import-twice_profile.xml

    @Test
    public void testIncludeAllImplicitProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/include-all-implicit-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/include-all-implicit-test_profile_RESOLVED.xml");
    }

    @Test
    public void testIncludeAllNoChildrenProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/include-all-no-children-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/include-all-no-children-test_profile_RESOLVED.xml");
    }

    @Test
    public void testIncludeAllProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/include-all-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/include-all-test_profile_RESOLVED.xml");
    }
    
    @Test
    public void testIncludeCallWithChildrenProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/include-call-with-children-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/include-call-with-children-test_profile_RESOLVED.xml");
    }

    // TODO: merge-implicit-keep_profile.xml
    // TODO: merge-keep_profile.xml
    
}
