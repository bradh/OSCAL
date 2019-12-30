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
import java.io.StringWriter;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.util.Nodes;

/**
 * Shared code for resolver tests
 */
abstract public class AbstractResolverTest {
    
    public AbstractResolverTest() {
    }

    protected void checkResultAgainstExpected(Catalog result, String expectedCatalogPath) {
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
        Diff ds = DiffBuilder.compare(Input.fromFile(expectedCatalog)).withTest(Input.fromString(xmlResult)).ignoreWhitespace().withNodeFilter((Node n) -> !(n instanceof Element && Nodes.getQName(n).getLocalPart().equals("prop") && ((Element) n).getAttribute("name").equals("resolution-timestamp"))).build();
        if (ds.hasDifferences()) {
            StringBuilder diffs = new StringBuilder();
            for (Difference d : ds.getDifferences()) {
                diffs.append(d.toString());
            }
            System.out.println(diffs.toString());
        }
        assertFalse(ds.hasDifferences(), ds.toString());
    }
    
}
