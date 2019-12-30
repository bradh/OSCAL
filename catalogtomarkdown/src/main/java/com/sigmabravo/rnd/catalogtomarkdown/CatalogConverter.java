package com.sigmabravo.rnd.catalogtomarkdown;

import com.sigmabravo.rnd.ismcatalogschema.BackMatter;
import com.sigmabravo.rnd.ismcatalogschema.Catalog;
import com.sigmabravo.rnd.ismcatalogschema.Control;
import com.sigmabravo.rnd.ismcatalogschema.Group;
import com.sigmabravo.rnd.ismcatalogschema.Img;
import com.sigmabravo.rnd.ismcatalogschema.Li;
import com.sigmabravo.rnd.ismcatalogschema.Metadata;
import com.sigmabravo.rnd.ismcatalogschema.P;
import com.sigmabravo.rnd.ismcatalogschema.Part;
import com.sigmabravo.rnd.ismcatalogschema.Prop;
import com.sigmabravo.rnd.ismcatalogschema.Table;
import com.sigmabravo.rnd.ismcatalogschema.Td;
import com.sigmabravo.rnd.ismcatalogschema.Th;
import com.sigmabravo.rnd.ismcatalogschema.Tr;
import com.sigmabravo.rnd.ismcatalogschema.Ul;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import net.steppschuh.markdowngenerator.image.Image;
import net.steppschuh.markdowngenerator.list.UnorderedList;
import net.steppschuh.markdowngenerator.table.Table.Builder;
import net.steppschuh.markdowngenerator.table.TableRow;
import net.steppschuh.markdowngenerator.text.Text;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.heading.Heading;

class CatalogConverter {

    private Catalog catalog;

    public CatalogConverter() {
    }

    void loadCatalog(File file) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Catalog.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            catalog = (Catalog) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    void writeToMarkdown() throws IOException {
        StringBuilder sb = new StringBuilder();
        Metadata metadata = catalog.getMetadata();
        List<Group> groups = catalog.getGroup();
        for (Group group : catalog.getGroup()) {
            sb.append(new Heading(group.getTitle().getContent().get(0), 1)).append("\n");
            for (Group subgroup : group.getGroup()) {
                sb.append(new Heading(subgroup.getTitle().getContent().get(0), 2)).append("\n");
                for (Group subsubgroup : subgroup.getGroup()) {
                    sb.append(new Heading(subsubgroup.getTitle().getContent().get(0), 3)).append("\n");
                    for (Part guidancePart : subsubgroup.getPart()) {
                        writeProse(guidancePart, sb);
                    }
                    for (Control control : subsubgroup.getControl()) {
                        String labelProp = "UNKNOWN";
                        String versionProp = "UNKNOWN";
                        String lastUpdatedProp = "UNKNOWN";
                        for (Prop prop : control.getProp()) {
                            if (prop.getName().equals("label")) {
                                labelProp = prop.getValue();
                            } else if (prop.getName().equals("Revision") && prop.getNs().equals("urn:uuid:5dab2ee4-11be-11ea-865a-672481b505d3")) {
                                versionProp = prop.getValue();
                            } else if (prop.getName().equals("Updated") && prop.getNs().equals("urn:uuid:5dab2ee4-11be-11ea-865a-672481b505d3")) {
                                lastUpdatedProp = prop.getValue();
                            }
                        }
                        sb.append(new BoldText("Control: " + labelProp + "; Revision: " + versionProp + "; Last Updated: " + lastUpdatedProp)).append("\n");
                        for (Part controlPart : control.getPart()) {
                            writeProse(controlPart, sb);
                        }
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        BackMatter backmatter = catalog.getBackMatter();
        File file = new File("catalog.md");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.append(sb);
        }
    }

    private void writeProse(Part part, StringBuilder sb) {
        for (Object prose : part.getPROSE()) {
            if (prose instanceof P) {
                P p = (P) prose;
                if (p.getContent().isEmpty()) {
                    continue;
                }
                for (Object o : p.getContent()) {
                    if (o instanceof String) {
                        sb.append(new Text(o)).append("\n");
                    } else if (o instanceof Img) {
                        Img img = (Img)o;
                        // TODO: copy file from src...
                        Image image = new Image(img.getSrc(), img.getSrc());
                        sb.append(image);
                    } else {
                        System.out.println("Unhandled content: " + o.getClass().getName());
                    }
                }
            } else if (prose instanceof Ul) {
                Ul ul = (Ul) prose;
                UnorderedList unorderedList = new UnorderedList();
                for (Li li : ul.getLi()) {
                    if (li.getContent().size() != 1) {
                        System.out.println("Unhandled Li size");
                    }
                    unorderedList.getItems().add(li.getContent().get(0));
                }
                sb.append(unorderedList);
                sb.append("\n");
            } else if (prose instanceof Table) {
                Table table = (Table) prose;
                Builder tableBuilder = new Builder();
                TableRow tableRow = new TableRow();
                for (Object thObj : table.getTr().get(0).getTdOrTh()) {
                    Th th = (Th) thObj;
                    tableRow.getColumns().add(th.getContent().get(0));
                }
                tableBuilder.addRow(tableRow);
                // TODO: this isn't producing the right result for the three accounts table.
                for (int i = 1; i < table.getTr().size(); ++i) {
                    Tr tr = table.getTr().get(i);
                    TableRow tdRow = new TableRow();
                    for (Object tdObj : tr.getTdOrTh()) {
                        Td td = (Td) tdObj;
                        List<Object> tdContent = td.getContent();
                        if (tdContent.isEmpty()) {
                            tdRow.getColumns().add("");
                        } else if (tdContent.size() == 1) {
                            String str = tdContent.get(0).toString().trim();
                            String cleanstr = str.replace("\n", "<br/>");
                            tdRow.getColumns().add(cleanstr);
                        } else {
                            System.out.println("Unhanlded content");
                        }
                    }
                    tableBuilder.addRow(tdRow);
                }
                sb.append(tableBuilder.build());
                sb.append("\n");
            } else {
                System.out.println("Unhandled case: " + prose.getClass().getName());
            }
        }
        sb.append("\n");
    }

}
