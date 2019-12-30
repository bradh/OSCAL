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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.w3c.dom.Element;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.util.Nodes;

public class ResolverMatchTest {
    
    public ResolverMatchTest() {
    }
    
    @Test
    public void testIncludeMatchProfileResolution() {
        URL sourceProfileUrl = Thread.currentThread().getContextClassLoader().getResource("profile-resolution-examples/include-match-test_profile.xml");
        File sourceProfile = new File(sourceProfileUrl.getPath());
        assertTrue(sourceProfile.exists());

        Resolver resolver = new Resolver(sourceProfile);
        Catalog result = resolver.resolve();

        checkResultAgainstExpected(result, "profile-resolution-examples/output-expected/include-match-test_profile_RESOLVED.xml");
    }

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
