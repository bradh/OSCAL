package com.sigmabravo.rnd.oscal.profileresolver;

import com.sigmabravo.rnd.ismcatalogschema.BackMatter;
import com.sigmabravo.rnd.ismcatalogschema.Catalog;
import com.sigmabravo.rnd.ismcatalogschema.Citation;
import com.sigmabravo.rnd.ismcatalogschema.Control;
import com.sigmabravo.rnd.ismcatalogschema.Group;
import com.sigmabravo.rnd.ismcatalogschema.LastModified;
import com.sigmabravo.rnd.ismcatalogschema.Link;
import com.sigmabravo.rnd.ismcatalogschema.Metadata;
import com.sigmabravo.rnd.ismcatalogschema.ObjectFactory;
import com.sigmabravo.rnd.ismcatalogschema.OscalVersion;
import com.sigmabravo.rnd.ismcatalogschema.Prop;
import com.sigmabravo.rnd.ismcatalogschema.Title;
import com.sigmabravo.rnd.ismcatalogschema.Version;
import com.sigmabravo.rnd.ismprofileschema.All;
import com.sigmabravo.rnd.ismprofileschema.Call;
import com.sigmabravo.rnd.ismprofileschema.Exclude;
import com.sigmabravo.rnd.ismprofileschema.Import;
import com.sigmabravo.rnd.ismprofileschema.Include;
import com.sigmabravo.rnd.ismprofileschema.Match;
import com.sigmabravo.rnd.ismprofileschema.Profile;
import com.sigmabravo.rnd.ismprofileschema.Resource;
import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.io.FilenameUtils;

public class Resolver {

    private ObjectFactory catalogObjectFactory;
    private String profileBaseName;
    private String profileFileName;
    private String profilePath;
    private Profile profile;
    private List<Control> selectedControls = new ArrayList<>();
    private List<Group> selectedGroups = new ArrayList<>();
    private List<String> controlsWeWant = new ArrayList<>();
    private List<Citation> citations = new ArrayList<>();

    public Resolver(File file) {
        catalogObjectFactory = new ObjectFactory();
        profilePath = FilenameUtils.getPath(file.getPath());
        profileFileName = file.getName();
        profileBaseName = FilenameUtils.getBaseName(profileFileName);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Profile.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            profile = (Profile) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public Catalog resolve() {
        Catalog catalog = catalogObjectFactory.createCatalog();
        catalog.setId(profileBaseName + "-RESOLVED");
        catalog.setMetadata(createMetadata());

        doSelection();
        doMerge();
        doModify();

        catalog.getControl().addAll(selectedControls);
        // TODO: needs to handle nested groups too.
        for (Group g : selectedGroups) {
            Group cleanedGroup = g;
            List<Control> cleanedControls = new ArrayList<>();
            for (Control control : g.getControl()) {
                if (controlsWeWant.contains(control.getId())) {
                    cleanedControls.add(control);
                }
            }
            cleanedGroup.getControl().clear();
            cleanedGroup.getControl().addAll(cleanedControls);
            catalog.getGroup().add(cleanedGroup);
        }

        if (!citations.isEmpty()) {
            BackMatter backMatter = catalogObjectFactory.createBackMatter();
            backMatter.getCitation().addAll(citations);
            catalog.setBackMatter(backMatter);
        }

        return catalog;
    }

    private Metadata createMetadata() {
        Metadata metadata = catalogObjectFactory.createMetadata();

        Title title = catalogObjectFactory.createTitle();
        title.getContent().addAll(profile.getMetadata().getTitle().getContent());
        metadata.setTitle(title);

        LastModified lastModified = catalogObjectFactory.createLastModified();
        lastModified.setValue(profile.getMetadata().getLastModified().getValue());
        metadata.setLastModified(lastModified);

        Version version = catalogObjectFactory.createVersion();
        version.setValue(profile.getMetadata().getVersion().getValue());
        metadata.setVersion(version);

        OscalVersion oscalVersion = catalogObjectFactory.createOscalVersion();
        oscalVersion.setValue("1.0-MR2");
        metadata.setOscalVersion(oscalVersion);

        Prop resolutionTimestampProp = catalogObjectFactory.createProp();
        resolutionTimestampProp.setName("resolution-timestamp");
        resolutionTimestampProp.setValue(ZonedDateTime.now(ZoneId.of("UTC")).toString());
        metadata.getProp().add(resolutionTimestampProp);

        Link resolutionSourceLink = catalogObjectFactory.createLink();
        resolutionSourceLink.setRel("resolution-source");
        resolutionSourceLink.setHref("../" + profileFileName);
        resolutionSourceLink.getContent().addAll(profile.getMetadata().getTitle().getContent());
        metadata.getLink().add(resolutionSourceLink);
        return metadata;
    }

    private static XMLGregorianCalendar getNowAsXMLGregorianCalendar() throws DatatypeConfigurationException {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        GregorianCalendar calNow = GregorianCalendar.from(now);
        XMLGregorianCalendar xmlNow = DatatypeFactory.newInstance().newXMLGregorianCalendar(calNow);
        return xmlNow;
    }

    private void doSelection() {
        for (Import im : profile.getImport()) {
            String href = im.getHref();
            String actualPath = href;
            if (href.startsWith("#")) {
                actualPath = lookupResource(href);
            }
            Catalog catalogForThisImport = getCatalog(actualPath);
            if (catalogForThisImport == null) {
                return;
            }
            Include in = im.getInclude();
            if (in == null) {
                // Implicit "all", includes child controls
                addAll(catalogForThisImport, true);
            } else {
                if (in.getAll() != null) {
                    All all = in.getAll();
                    Boolean withChildControls = !("no".equalsIgnoreCase(all.getWithChildControls()));
                    addAll(catalogForThisImport, withChildControls);
                }
                for (Call call : in.getCall()) {
                    controlsWeWant.add(call.getControlId());
                    selectCalledControl(call, catalogForThisImport.getControl());
                    selectGroupsWithCalledControls(call, catalogForThisImport.getGroup());
                }
                for (Match match : in.getMatch()) {
                    selectMatchedControl(match, catalogForThisImport.getControl());
                    selectGroupsWithMatchingControls(match, catalogForThisImport.getGroup());
                }
            }
            Exclude ex = im.getExclude();
            if (ex != null) {
                for (Call call : ex.getCall()) {
                    String controlIdToExclude = call.getControlId();
                    controlsWeWant.remove(controlIdToExclude);
                }
            }
            // TODO: we should be more selective about what gets pushed into citations - check for use.
            if (catalogForThisImport.getBackMatter() != null) {
                citations.addAll(catalogForThisImport.getBackMatter().getCitation());
            }
        }

    }

    private void addAll(Catalog catalogForThisImport, Boolean withChildControls) {
        for (Control control : catalogForThisImport.getControl()) {
            if (!withChildControls) {
                control.getControl().clear();
            }
            controlsWeWant.add(control.getId());
        }
        selectedControls.addAll(catalogForThisImport.getControl());
        selectAllGroupsWithControls(catalogForThisImport.getGroup(), withChildControls);
    }

    private void selectCalledControl(Call call, List<Control> controls) {
        String controlId = call.getControlId();
        Boolean withChildControls = !("no".equalsIgnoreCase(call.getWithChildControls()));
        for (Control control : controls) {
            if (control.getId().equals(controlId)) {
                controlsWeWant.add(controlId);
                selectedControls.add(control);
            }
        }
    }

    private void selectMatchedControl(Match match, List<Control> controls) {
        Pattern pattern = Pattern.compile(match.getPattern());
        Boolean withChildControls = !("no".equalsIgnoreCase(match.getWithChildControls()));
        for (Control control : controls) {
            if (pattern.matcher(control.getId()).find()) {
                controlsWeWant.add(control.getId());
                selectedControls.add(control);
            }
        }
    }

    private void selectAllGroupsWithControls(List<Group> groups, Boolean withChildControls) {
        for (Group group : groups) {
            if (groupHasAnyControls(group)) {
                for (Control control : group.getControl()) {
                    if (!withChildControls) {
                        control.getControl().clear();
                    }
                    controlsWeWant.add(control.getId());
                }
                // TODO: this probably needs to recurse
                selectedGroups.add(group);
            }
        }
    }

    private boolean groupHasAnyControls(Group group) {
        if (!group.getControl().isEmpty()) {
            return true;
        }
        for (Group subgroup : group.getGroup()) {
            if (groupHasAnyControls(subgroup)) {
                return true;
            }
        }
        return false;
    }

    private void selectGroupsWithCalledControls(Call call, List<Group> groups) {
        String controlId = call.getControlId();
        for (Group group : groups) {
            if (groupHasCalledControl(group, controlId)) {
                selectGroup(group);
            }
        }
    }

    private void selectGroupsWithMatchingControls(Match match, List<Group> groups) {
        Pattern pattern = Pattern.compile(match.getPattern());
        for (Group group : groups) {
            if (groupHasMatchingControl(group, pattern)) {
                selectGroup(group);
                addMatchingControlsToList(group, pattern);
            }
        }
    }

    private void selectGroup(Group group) {
        // TODO: this will need to be smarter
        for (Group g : selectedGroups) {
            if (g.getTitle().equals(group.getTitle())) {
                // We already have this one
                return;
            }
        }
        selectedGroups.add(group);
    }

    private boolean groupHasCalledControl(Group group, String controlId) {
        for (Control control : group.getControl()) {
            if (control.getId().equals(controlId)) {
                return true;
            }
        }
        for (Group subgroup : group.getGroup()) {
            if (groupHasCalledControl(subgroup, controlId)) {
                return true;
            }
        }
        return false;
    }

    private boolean groupHasMatchingControl(Group group, Pattern pattern) {
        for (Control control : group.getControl()) {
            if (pattern.matcher(control.getId()).find()) {
                return true;
            }
        }
        for (Group subgroup : group.getGroup()) {
            if (groupHasMatchingControl(subgroup, pattern)) {
                return true;
            }
        }
        return false;
    }

    private void addMatchingControlsToList(Group group, Pattern pattern) {
        for (Control control : group.getControl()) {
            if (pattern.matcher(control.getId()).find()) {
                controlsWeWant.add(control.getId());
            }
        }
        for (Group subgroup : group.getGroup()) {
            addMatchingControlsToList(subgroup, pattern);
        }
    }

    private Catalog getCatalog(String href) {
        File file = new File("/" + profilePath + "/" + href);
        if (!file.exists()) {
            return null;
        }
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Catalog.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Catalog catalog = (Catalog) jaxbUnmarshaller.unmarshal(file);
            return catalog;
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void doMerge() {

    }

    private void doModify() {

    }

    private String lookupResource(String resource) {
        // Trim off leading #
        String resourceKey = resource.substring(1);
        for (Resource r : profile.getBackMatter().getResource()) {
            if (r.getId().equals(resourceKey)) {
                return r.getRlink().get(0).getHref();
            }
        }
        return null;
    }

}
