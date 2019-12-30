/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sigmabravo.rnd.catalogtomarkdown;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author bradh
 */
public class NewMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        CatalogConverter catalogConverter = new CatalogConverter();
        catalogConverter.loadCatalog(new File("../ismwordconverter/Australian_Government_Information_Security_Manual_NOV19_catalog.xml"));
        catalogConverter.writeToMarkdown();
    }
    
}
