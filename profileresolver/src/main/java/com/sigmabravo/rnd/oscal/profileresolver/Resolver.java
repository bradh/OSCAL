package com.sigmabravo.rnd.oscal.profileresolver;

import com.sigmabravo.rnd.ismcatalogschema.BackMatter;
import com.sigmabravo.rnd.ismcatalogschema.Catalog;
import com.sigmabravo.rnd.ismcatalogschema.Citation;
import com.sigmabravo.rnd.ismcatalogschema.Control;
import com.sigmabravo.rnd.ismcatalogschema.Group;
import com.sigmabravo.rnd.ismcatalogschema.Insert;
import com.sigmabravo.rnd.ismcatalogschema.LastModified;
import com.sigmabravo.rnd.ismcatalogschema.Link;
import com.sigmabravo.rnd.ismcatalogschema.Metadata;
import com.sigmabravo.rnd.ismcatalogschema.ObjectFactory;
import com.sigmabravo.rnd.ismcatalogschema.OscalVersion;
import com.sigmabravo.rnd.ismcatalogschema.P;
import com.sigmabravo.rnd.ismcatalogschema.Param;
import com.sigmabravo.rnd.ismcatalogschema.Part;
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
import java.util.Collection;
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

        // TODO: needs to clean up params.
        catalog.getControl().addAll(selectedControls);
        for (Group g : selectedGroups) {
            if (groupHasAnyControlsWeWant(g)) {
                catalog.getGroup().add(cleanGroup(g));
            }
        }

        if (!citations.isEmpty()) {
            BackMatter backMatter = catalogObjectFactory.createBackMatter();
            backMatter.getCitation().addAll(citations);
            catalog.setBackMatter(backMatter);
        }

        return catalog;
    }

    private Group cleanGroup(Group g) {
        // TODO: needs to handle nested groups too.
        List<String> paramForGroup = new ArrayList<>();
        Group cleanedGroup = g;
        List<Control> cleanedControls = new ArrayList<>();
        for (Control control : g.getControl()) {
            if (controlsWeWant.contains(control.getId())) {
                List<String> paramReferences = getParamsFromControl(control);
                paramForGroup.addAll(paramReferences);
                cleanedControls.add(control);
            }
        }
        cleanedGroup.getControl().clear();
        cleanedGroup.getControl().addAll(cleanedControls);
        List<Group> cleanedSubGroups = new ArrayList<>();
        for (Group subGroup: g.getGroup()) {
            if (groupHasAnyControlsWeWant(subGroup)) {
                cleanedSubGroups.add(cleanGroup(subGroup));
            }
        }
        cleanedGroup.getGroup().clear();
        cleanedGroup.getGroup().addAll(cleanedSubGroups);
        List<Param> cleanedParams = new ArrayList<>();
        for (Param param: g.getParam()) {
            if (paramForGroup.contains(param.getId())) {
                cleanedParams.add(param);
            }
        }
        cleanedGroup.getParam().clear();
        cleanedGroup.getParam().addAll(cleanedParams);
        return cleanedGroup;
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
                selectAllControlsFromSubGroups(group.getGroup(), withChildControls);
                selectedGroups.add(group);
            }
        }
    }
    
    private void selectAllControlsFromSubGroups(List<Group> groups, Boolean withChildControls) {
        for (Group group : groups) {
            if (groupHasAnyControls(group)) {
                for (Control control : group.getControl()) {
                    if (!withChildControls) {
                        control.getControl().clear();
                    }
                    controlsWeWant.add(control.getId());
                }
                selectAllControlsFromSubGroups(group.getGroup(), withChildControls);
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
        List<Group> cleanedGroups = new ArrayList<>();
        for (Group subGroup: group.getGroup()) {
            for (String c: controlsWeWant) {
                if (groupHasCalledControl(subGroup, c)) {
                    cleanedGroups.add(subGroup);
                }
            }
        }
        group.getGroup().clear();
        group.getGroup().addAll(cleanedGroups);
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

    private List<String> getParamsFromControl(Control control) {
        List<String> paramIds = new ArrayList<>();
        for (Part part: control.getPart()) {
            paramIds.addAll(getParamsFromPart(part));
        }
        return paramIds;
    }

    private List<String> getParamsFromPart(Part part) {
        List<String> paramIds = new ArrayList<>();
        for (Object prose: part.getPROSE()) {
            if (prose instanceof P) {
                P p = (P)prose;
                paramIds.addAll(getParamsFromP(p));
            }
        }
        return paramIds;
        
    }

    private List<String> getParamsFromP(P p) {
         List<String> paramIds = new ArrayList<>();
         for (Object content: p.getContent()) {
             if (content instanceof Insert) {
                 Insert insert = (Insert)content;
                 paramIds.add(insert.getParamId());
             }
         }
         return paramIds;
    }

    private boolean groupHasAnyControlsWeWant(Group group) {
        for (Control control: group.getControl()) {
            String controlId = control.getId();
            if (controlsWeWant.contains(controlId)) {
                return true;
            }
        }
        for (Group subGroup: group.getGroup()) {
            if (groupHasAnyControlsWeWant(subGroup)) {
                return true;
            }
        }
        return false;
    }

}
