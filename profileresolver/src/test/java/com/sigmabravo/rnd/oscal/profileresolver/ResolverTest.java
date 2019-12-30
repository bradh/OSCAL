/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sigmabravo.rnd.oscal.profileresolver;

import com.sigmabravo.rnd.ismcatalogschema.Catalog;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.w3c.dom.Element;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;
import org.xmlunit.util.Nodes;

/**
 *
 * @author bradh
 */
public class ResolverTest {
    
    public ResolverTest() {
    }
    
    @Test
    public void testBaseProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/base-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/base-test_profile_RESOLVED.xml");
    }
    
    @Test
    public void testBase2ProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/base2-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/base2-test_profile_RESOLVED.xml");
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

    @Test
    public void testFullProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/full-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/full-test_profile_RESOLVED.xml");
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

    // TODO: include-loose-param-test_profile.xml

    // TODO: merge-implicit-keep_profile.xml
    // TODO: merge-keep_profile.xml

    private void checkResultAgainstExpected(Catalog result, String expectedCatalogPath) {
        URL expectedCatalogUrl = Thread.currentThread().getContextClassLoader().getResource(expectedCatalogPath);
        File expectedCatalog = new File(expectedCatalogUrl.getPath());
        assertTrue(expectedCatalog.exists());
        
        StringWriter sw = new StringWriter();
        sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Catalog.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.marshal(result, sw);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        String xmlResult = sw.toString();
        System.out.println(xmlResult);
        Diff ds = DiffBuilder.compare(Input.fromFile(expectedCatalog))
                .withTest(Input.fromString(xmlResult))
                .ignoreWhitespace()
                .withNodeFilter(n -> !(n instanceof Element && Nodes.getQName(n).getLocalPart().equals("prop") && ((Element)n).getAttribute("name").equals("resolution-timestamp")))
                .build();
        if (ds.hasDifferences()) {
            StringBuilder diffs = new StringBuilder();
            for (Difference d: ds.getDifferences()) {
                diffs.append(d.toString());
            }
            System.out.println(diffs.toString());
        }
        assertFalse(ds.hasDifferences(), ds.toString());
    }
    
}
