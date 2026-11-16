/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.originalmets;

import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.utils.JsonUtils;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.Representation;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.data.v2.ip.metadata.LinkingIdentifier;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.plugins.PluginException;
import org.roda.core.plugins.base.characterization.SiegfriedPlugin;
import org.roda.core.plugins.base.characterization.SiegfriedPluginUtils;
import org.roda.core.storage.DirectResourceAccess;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip2.mets_v1_12.beans.AmdSecType;
import org.roda_project.commons_ip2.mets_v1_12.beans.DivType;
import org.roda_project.commons_ip2.mets_v1_12.beans.DivType.Fptr;
import org.roda_project.commons_ip2.mets_v1_12.beans.FileType;
import org.roda_project.commons_ip2.mets_v1_12.beans.FileType.FLocat;
import org.roda_project.commons_ip2.mets_v1_12.beans.MdSecType;
import org.roda_project.commons_ip2.mets_v1_12.beans.Mets;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType;
import org.roda_project.commons_ip2.mets_v1_12.beans.ObjectFactory;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType.FileSec;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType.FileSec.FileGrp;
import org.roda_project.commons_ip2.mets_v1_12.beans.StructMapType;
import org.roda_project.commons_ip2.model.IPConstants;
import org.roda_project.commons_ip2.model.MetsWrapper;
import org.roda_project.commons_ip2.utils.METSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

import io.jsonwebtoken.lang.Arrays;

/**
 * Updates the original METS file(s) in the AIP
 *
 * @author Paul Hedberg <paul.hedberg@skane.se>
 */
public class UpdateOriginalMETS {

    public static Logger getLog() {
        return LOG;
    }

    public static String getPremisFileType() {
        return PREMIS_FILE_TYPE;
    }

    public static String getRepresentationFilePrefix() {
        return REPRESENTATION_FILE_PREFIX;
    }

    public static String getRelatedpackage() {
        return RELATEDPACKAGE;
    }

    public static String getAncestors() {
        return ANCESTORS;
    }

    public static String getDescendents() {
        return DESCENDENTS;
    }
    
    public static String getStructmapTypePhysical() {
		return STRUCTMAP_TYPE_PHYSICAL;
	}

	public static String getStructmapLabelCsip() {
		return STRUCTMAP_LABEL_CSIP;
	}

    private static final Logger LOG = LoggerFactory.getLogger(
        UpdateOriginalMETS.class
    );

    private static final String PREMIS_FILE_TYPE = ".xml";
    private static final String REPRESENTATION_FILE_PREFIX = "urn:roda:premis:representation:";

    private static final String RELATEDPACKAGE = "RelatedPackage";
    private static final String ANCESTORS = "Parent";
    private static final String DESCENDENTS = "Children";
    
    private static final String REPRESENTATION = "Representation";
    private static final String REPRESENTATIONS = "Representations";

	private static final String STRUCTMAP_TYPE_PHYSICAL = "PHYSICAL";
    private static final String STRUCTMAP_LABEL_CSIP = "CSIP";
    
    private static final String HREF_DATA_PREFIX = "data/";

    public static void addPremisToMETS(
        PreservationMetadata pm,
        ModelService modelService
    ) {
        boolean keepOriginalMets = RodaCoreFactory.getProperty(
            RodaConstants.CORE_PLUGINS_BASE_KEEP_ORIGINAL_METS,
            false
        );

        if (!keepOriginalMets) {
            return;
        }

        try {
            StoragePath aipPath = ModelUtils.getAIPStoragePath(pm.getAipId());
            
            // ROOT METS
            if (pm.getRepresentationId() == null) {
                Path metsPath = OriginalMETSUtils.metsPath(
                    modelService,
                    aipPath
                );
                
                MetsWrapper metsWrapper = new MetsWrapper(
                    METSUtils.instantiateMETSFromFile(metsPath),
                    metsPath
                );
                
                handleParentAIP(modelService, pm.getAipId(), metsWrapper);
 
                java.io.File file = modelService
                    .getStorage()
                    .getDirectAccess(aipPath)
                    .getPath()
                    .resolve(Path.of(IPConstants.PRESERVATION_FOLDER))
                    .resolve(pm.getId() + PREMIS_FILE_TYPE)
                    .toFile();

                if (file.exists()) {
                    String relativeFilePath =
                        IPConstants.METADATA +
                        java.io.File.separator +
                        IPConstants.PRESERVATION +
                        java.io.File.separator +
                        file.getName();

                    checkAmdSec(metsWrapper);

                    Optional<MdSecType> fileInMets = metsWrapper
                        .getMets()
                        .getAmdSec()
                        .getFirst()
                        .getDigiprovMD()
                        .stream()
                        .filter(m ->
                            m.getMdRef().getHref().equals(relativeFilePath)
                        )
                        .findFirst();
                    if (!fileInMets.isPresent()) {
                        addPremisToMETS(
                            relativeFilePath,
                            file,
                            metsWrapper.getMets(),
                            metsPath,
                            true);
                        
                        AIP aip = modelService.retrieveAIP(pm.getAipId());
                        if (aip.getParentId() != null) {
                        	updateLastModDateInParentMETS(
                        			modelService.retrieveAIP(aip.getParentId()),
                            		modelService);
                        } 
                    }
                }

                // REPRESENTATION METS
            } else {
                Path representationPath = Path.of(
                    IPConstants.REPRESENTATIONS_FOLDER
                ).resolve(Path.of(pm.getRepresentationId()));

                Path metsPath = OriginalMETSUtils.metsPath(
                    modelService,
                    aipPath,
                    representationPath
                );

                AIP aipTemp = modelService.retrieveAIP(pm.getAipId());
                Optional<Representation> rep = aipTemp.getRepresentations().stream().filter(r -> r.getId().equals(pm.getRepresentationId())).findFirst();
                if (rep.isPresent() && ! new File(metsPath.toString()).exists()) {
                    CreateRepresentationMETS.create(rep.get(), modelService);
                }
                
                MetsWrapper metsWrapper = new MetsWrapper(
                    METSUtils.instantiateMETSFromFile(metsPath),
                    metsPath
                );

                Path subFolders = Path.of("");
                if (pm.getFileDirectoryPath() != null) {
	                subFolders = Path.of(
	                    pm
	                        .getFileDirectoryPath()
	                        .stream()
	                        .map(p -> p)
	                        .collect(Collectors.joining(java.io.File.separator))
	                );
                }

                String fileName = "";
                if (pm.getType().name().equals("FILE")) {
                    fileName = pm.getId() + PREMIS_FILE_TYPE;
                } else if (pm.getType().name().equals("EVENT")) {
                	String pmFileId = pm.getFileId() != null ? "%3A" + pm.getFileId() : "";
                    fileName =
                        pm.getId() + pmFileId + PREMIS_FILE_TYPE;
                } else {
                    return;
                }
                
                java.io.File file = modelService
                    .getStorage()
                    .getDirectAccess(aipPath)
                    .getPath()
                    .resolve(Path.of(IPConstants.REPRESENTATIONS_FOLDER))
                    .resolve(Path.of(pm.getRepresentationId()))
                    .resolve(Path.of(IPConstants.PRESERVATION_FOLDER))
                    .resolve(subFolders)
                    .resolve(fileName)
                    .toFile();

                if (file.exists()) {
                    String relativeFilePath =
                        IPConstants.METADATA +
                        java.io.File.separator +
                        IPConstants.PRESERVATION +
                        java.io.File.separator +
                        subFolders +
                        java.io.File.separator +
                        file.getName();

                    checkAmdSec(metsWrapper);

                    Optional<MdSecType> fileInMets = metsWrapper
                        .getMets()
                        .getAmdSec()
                        .getFirst()
                        .getDigiprovMD()
                        .stream()
                        .filter(m ->
                            m.getMdRef().getHref().equals(relativeFilePath)
                        )
                        .findFirst();
                    if (!fileInMets.isPresent()) {
                        addPremisToMETS(
                            relativeFilePath,
                            file,
                            metsWrapper.getMets(),
                            metsPath,
                            false
                        );
                        
                        AIP aip = modelService.retrieveAIP(pm.getAipId());
                    	updateLastModDateInParentMETS(
                        		aip,
                        		modelService);
                    }
                }
            }
        } catch (
            AuthorizationDeniedException
            | DatatypeConfigurationException
            | GenericException
            | IOException
            | IPException
            | JAXBException
            | NotFoundException
            | RequestNotValidException
            | SAXException e
        ) {
            LOG.error(
                "Then update the original AIP {} METS file - Error: {}",
                pm.getAipId(),
                e
            );
        }
    }

    public static void addPremisToNewRepresentation(
        String aipId,
        String representationId,
        String representationUUID,
        ModelService modelService
    ) {
        boolean keepOriginalMets = RodaCoreFactory.getProperty(
            RodaConstants.CORE_PLUGINS_BASE_KEEP_ORIGINAL_METS,
            false
        );
        if (!keepOriginalMets) {
            return;
        }

        try {
            StoragePath aipPath = ModelUtils.getAIPStoragePath(aipId);
            Path representationPath = Path.of(
                IPConstants.REPRESENTATIONS_FOLDER
            ).resolve(Path.of(representationId));
            Path metsPath = OriginalMETSUtils.metsPath(
                modelService,
                aipPath,
                representationPath
            );
            MetsWrapper metsWrapper = new MetsWrapper(
                METSUtils.instantiateMETSFromFile(metsPath),
                metsPath
            );

            String fileName =
                REPRESENTATION_FILE_PREFIX +
                representationUUID +
                PREMIS_FILE_TYPE;
            java.io.File file = modelService
                .getStorage()
                .getDirectAccess(aipPath)
                .getPath()
                .resolve(Path.of(IPConstants.REPRESENTATIONS_FOLDER))
                .resolve(Path.of(representationId))
                .resolve(Path.of(IPConstants.PRESERVATION_FOLDER))
                .resolve(fileName)
                .toFile();

            if (file.exists()) {
                String relativeFilePath =
                    IPConstants.METADATA +
                    java.io.File.separator +
                    IPConstants.PRESERVATION +
                    java.io.File.separator +
                    file.getName();

                checkAmdSec(metsWrapper);

                Optional<MdSecType> fileInMets = metsWrapper
                    .getMets()
                    .getAmdSec()
                    .getFirst()
                    .getDigiprovMD()
                    .stream()
                    .filter(m ->
                        m.getMdRef().getHref().equals(relativeFilePath)
                    )
                    .findFirst();
                if (!fileInMets.isPresent()) {
                    addPremisToMETS(
                        relativeFilePath,
                        file,
                        metsWrapper.getMets(),
                        metsPath,
                        false);
                    
                    AIP aip = modelService.retrieveAIP(aipId);
                	updateLastModDateInParentMETS(
                    		aip,
                    		modelService);
                }
            }
        } catch (
            AuthorizationDeniedException
            | DatatypeConfigurationException
            | GenericException
            | IOException
            | IPException
            | JAXBException
            | NotFoundException
            | RequestNotValidException
            | SAXException e
        ) {
            LOG.error(
                "Then update the original AIP {} representation {} METS file - Error: {}",
                aipId,
                representationId,
                e
            );
        }
    }

    public static void handleParentAIP(
        ModelService modelService,
        String aipId,
        MetsWrapper metsWrapper
    )
        throws 
        	AuthorizationDeniedException, 
        	DatatypeConfigurationException,
        	GenericException, 
        	IOException, 
        	IPException, 
        	JAXBException, 
        	NotFoundException, 
        	RequestNotValidException, 
        	SAXException 
    {
        AIP aip = modelService.retrieveAIP(aipId);
        AIP parentAIP = null;

        if (aip.getParentId() != null) {
            parentAIP = modelService.retrieveAIP(
                modelService.retrieveAIP(aipId).getParentId()
            );
        }
        
        DivType divType = getStructMapRelatedPackageDiv(metsWrapper);
        Optional<DivType> optionalAncestorDivType = divType
            .getDiv()
            .stream()
            .filter(d -> d.getLABEL().equals(ANCESTORS))
            .findFirst();

        if (parentAIP != null) {
            final String pId = parentAIP.getId();
            
            DivType ancestorDivType;
            if (!optionalAncestorDivType.isPresent()) {
                ancestorDivType = createDivType(null, ANCESTORS);
                divType.getDiv().add(ancestorDivType);
            } else {
                ancestorDivType = optionalAncestorDivType.get();
            }

            Optional<DivType.Mptr> optionalDivTypeMptr = ancestorDivType
                .getMptr()
                .stream()
                .filter(m -> m.getHref().equals(pId))
                .findFirst();

            if (!optionalDivTypeMptr.isPresent()) {
                Optional<DivType.Mptr> optionalOldDivTypeMptr = ancestorDivType
                    .getMptr()
                    .stream()
                    .findFirst();
                if (optionalOldDivTypeMptr.isPresent()) {
                    removeDescendentFromParent(
                        modelService,
                        aipId,
                        optionalOldDivTypeMptr.get().getHref()
                    );
                }

                ancestorDivType.getMptr().clear();
                
                DivType.Mptr divTypeMptr = createMptrDivType(parentAIP.getId());
                ancestorDivType.getMptr().add(divTypeMptr);
                addDescendentToParent(modelService, aipId, parentAIP.getId());
            }
            
        } else {
            if (optionalAncestorDivType.isPresent()) {
                DivType ancestorDivType = optionalAncestorDivType.get();
                
                Optional<DivType.Mptr> optionalDivTypeMptr = ancestorDivType
                    .getMptr()
                    .stream()
                    .findFirst();
                if (optionalDivTypeMptr.isPresent()) {
                    removeDescendentFromParent(
                        modelService,
                        aipId,
                        optionalDivTypeMptr.get().getHref()
                    );
                }

                ancestorDivType.getMptr().clear();
            }
        }
    }
    
    private static DivType getStructMapRelatedPackageDiv(MetsWrapper metsWrapper) {
    	DivType packageDivType = getStructMapPackageDiv(getStructMap(metsWrapper));
   	
        List<DivType> divTypeList = findDivType(metsWrapper, RELATEDPACKAGE);
        DivType relatedPackageDivType = null;
        if (divTypeList.isEmpty()) {
        	relatedPackageDivType = new DivType();
        	relatedPackageDivType.setID("uuid-" + UUID.randomUUID().toString());
        	relatedPackageDivType.setLABEL(RELATEDPACKAGE);
        	packageDivType.getDiv().add(relatedPackageDivType);
        	
        } else {
        	relatedPackageDivType = divTypeList.getFirst();
        }

        return relatedPackageDivType;
    }
    
    public static DivType getStructMapPackageDiv(StructMapType structMapType) {
    	DivType packageDivType = null;
        if (structMapType.getDiv() == null) {
        	packageDivType = new DivType();
            packageDivType.setID("uuid-" + UUID.randomUUID().toString());
            packageDivType.setTYPE("Package");
            packageDivType.setLABEL("Package");
            structMapType.setDiv(packageDivType);
            
        } else {
        	packageDivType = structMapType.getDiv();
        }
        
        return packageDivType;
    }
    
    private static StructMapType getStructMap(MetsWrapper metsWrapper) {
    	StructMapType structMapType = null;
        if (metsWrapper.getMets().getStructMap().isEmpty()
        		|| !(metsWrapper.getMets().getStructMap().getFirst().getTYPE().equals(STRUCTMAP_TYPE_PHYSICAL)
                		&& metsWrapper.getMets().getStructMap().getFirst().getLABEL().equals(STRUCTMAP_LABEL_CSIP))
        				) {
        	
        	structMapType = new StructMapType();
        	structMapType.setID("uuid-" + UUID.randomUUID().toString());
        	structMapType.setTYPE(STRUCTMAP_TYPE_PHYSICAL);
        	structMapType.setLABEL(STRUCTMAP_LABEL_CSIP); 
            metsWrapper.getMets().getStructMap().add(structMapType);
        } 
        
        if (structMapType == null) {
        	for (StructMapType smt : metsWrapper.getMets().getStructMap()) {
        		if (
        				smt.getTYPE().equals(STRUCTMAP_TYPE_PHYSICAL)
        				&& smt.getLABEL().equals(STRUCTMAP_LABEL_CSIP)) {
        			
        			structMapType = smt;
        		}
        	}
        }
        
        return structMapType;
    }
    
    private static List<DivType> findDivType(MetsWrapper metsWrapper, String label) {
    	return metsWrapper
                .getMets()
                .getStructMap()
                .getFirst()
                .getDiv()
                .getDiv()
                .stream()
                .filter(s -> s.getLABEL().equals(label))
                .toList();
    }

    private static void addDescendentToParent(
        ModelService modelService,
        String aipId,
        String parentAipId
    )
        throws 
        	AuthorizationDeniedException,
        	DatatypeConfigurationException,
        	GenericException, 
        	IOException, 
        	IPException, 
        	JAXBException, 
        	NotFoundException, 
        	RequestNotValidException, 
        	SAXException 
    {
        StoragePath parentAipPath = ModelUtils.getAIPStoragePath(parentAipId);
        Path metsPath = OriginalMETSUtils.metsPath(modelService, parentAipPath);
        MetsWrapper metsWrapper = new MetsWrapper(
            METSUtils.instantiateMETSFromFile(metsPath),
            metsPath
        );

        DivType divType = getStructMapRelatedPackageDiv(metsWrapper);

        Optional<DivType> optionalDescendentsDivType = divType
            .getDiv()
            .stream()
            .filter(f -> f.getLABEL().equals(DESCENDENTS))
            .findFirst();

        if (!optionalDescendentsDivType.isPresent()) {
            DivType descendentsDivType = createDivType(null, DESCENDENTS);
            divType.getDiv().add(descendentsDivType);
            descendentsDivType.getMptr().add(createMptrDivType(aipId));
            metsWrapper.getMets().getMetsHdr()
    			.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
            METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, true);
            updateLastModDateInParentMETS(
        			modelService.retrieveAIP(parentAipId),
            		modelService);
            
        } else {
            Optional<DivType.Mptr> optionalDivTypeMptr =
                optionalDescendentsDivType
                    .get()
                    .getMptr()
                    .stream()
                    .filter(m -> m.getHref().equals(aipId))
                    .findFirst();

            if (!optionalDivTypeMptr.isPresent()) {
                optionalDescendentsDivType = divType
                    .getDiv()
                    .stream()
                    .filter(f -> f.getLABEL().equals(DESCENDENTS))
                    .findFirst();
                optionalDescendentsDivType
                    .get()
                    .getMptr()
                    .add(createMptrDivType(aipId));
                metsWrapper.getMets().getMetsHdr()
        			.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
                METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, true);
            	updateLastModDateInParentMETS(
            			modelService.retrieveAIP(parentAipId),
                		modelService);
            }
        }
    }

    private static void removeDescendentFromParent(
        ModelService modelService,
        String aipId,
        String parentAipId
    )
        throws 
        	AuthorizationDeniedException,
        	DatatypeConfigurationException,
        	GenericException, 
        	IOException, 
        	IPException, 
        	JAXBException, 
        	NotFoundException, 
        	RequestNotValidException, 
        	SAXException 
    {
        StoragePath parentAipPath = ModelUtils.getAIPStoragePath(parentAipId);
        Path metsPath = OriginalMETSUtils.metsPath(modelService, parentAipPath);
        MetsWrapper metsWrapper = new MetsWrapper(
            METSUtils.instantiateMETSFromFile(metsPath),
            metsPath
        );

        DivType divType = getStructMapRelatedPackageDiv(metsWrapper);
        Optional<DivType> optionalDescendentsDivType = divType
            .getDiv()
            .stream()
            .filter(f -> f.getLABEL().equals(DESCENDENTS))
            .findFirst();

        if (optionalDescendentsDivType.isPresent()) {
            Optional<DivType.Mptr> optionalDivTypeMptr =
                optionalDescendentsDivType
                    .get()
                    .getMptr()
                    .stream()
                    .filter(m -> m.getHref().equals(aipId))
                    .findFirst();
            if (optionalDivTypeMptr.isPresent()) {
                optionalDescendentsDivType
                    .get()
                    .getMptr()
                    .remove(optionalDivTypeMptr.get());
                metsWrapper.getMets().getMetsHdr()
    				.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
                METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, true);
            	updateLastModDateInParentMETS(
            			modelService.retrieveAIP(parentAipId),
                		modelService);
            }
        }
    }
    
    public static void handleParentWhenRemovedAIP(
        ModelService modelService,
        String removedAipId,
        String parentAipId
    )
        throws 
        	AuthorizationDeniedException, 
        	DatatypeConfigurationException,
        	GenericException, 
        	IOException, 
        	IPException, 
        	JAXBException, 
        	NotFoundException, 
        	RequestNotValidException, 
        	SAXException 
    {
    	
        boolean keepOriginalMets = RodaCoreFactory.getProperty(
            RodaConstants.CORE_PLUGINS_BASE_KEEP_ORIGINAL_METS,
            false
        );
        if (!keepOriginalMets) {
            return;
        }
        
        removeDescendentFromParent(modelService, removedAipId, parentAipId);
    }

    public static DivType createDivType(String type, String label) {
        DivType dt = new DivType();
        dt.setID("uuid-" + UUID.randomUUID().toString());
        if (type != null) {
        	dt.setTYPE(type);
        }
        dt.setLABEL(label);
        return dt;
    }

    private static DivType.Mptr createMptrDivType(String aipId) {
        DivType.Mptr mptr = new DivType.Mptr();
        mptr.setLOCTYPE("OTHER");
        mptr.setOTHERLOCTYPE("UUID");
        mptr.setType("simple");
        mptr.setHref(aipId);
        mptr.setTitle("UUID-" + aipId);
        return mptr;
    }

    private static void addPremisToMETS(
        String relativeFilePath,
        java.io.File file,
        Mets mets,
        Path metsPath,
        boolean isRoot
    )
        throws 
        	IOException, 
        	DatatypeConfigurationException, 
        	IPException, 
        	JAXBException 
    {
        MdSecType mdSecType = new MdSecType();
        mdSecType.setID("uuid-" + UUID.randomUUID().toString());
        mdSecType.setGROUPID("PREMIS");
        MdSecType.MdRef ref = new MdSecType.MdRef();
        ref.setID("uuid-" + UUID.randomUUID().toString());
        ref.setHref(relativeFilePath);
        ref.setType("simple");

        ref.setCHECKSUM(
            DigestUtils.md5Hex(
                java.nio.file.Files.readAllBytes(file.toPath())
            ).toUpperCase()
        );
        ref.setCHECKSUMTYPE("MD5");

        ref.setSIZE(FileUtils.sizeOf(file));
        ref.setLOCTYPE("URL");
        ref.setMDTYPE("PREMIS");
        ref.setMIMETYPE("text/xml");
        ref.setCREATED(OriginalMETSUtils.currentDateAndTime());

        mdSecType.setMdRef(ref);

        mets.getAmdSec().getFirst().getDigiprovMD().add(mdSecType);
        mets
            .getMetsHdr()
            .setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());

        METSUtils.marshallMETS(mets, metsPath, isRoot);
    }

    private static void checkAmdSec(MetsWrapper metsWrapper)
        throws 
        	IOException,
        	IPException, 
        	JAXBException {
    	
        try {
            metsWrapper.getMets().getAmdSec().getFirst();
        } catch (java.util.NoSuchElementException e) {
            AmdSecType amdSec = new AmdSecType();
            amdSec.setID("uuid-" + UUID.randomUUID().toString());
            metsWrapper.getMets().getAmdSec().add(amdSec);
        }
    }
    
    public static FileGrp createFileGrpRepresentation() {
    	FileGrp fileGrp = new FileGrp();
        fileGrp.setID("uuid-" + UUID.randomUUID().toString());
        fileGrp.setUSE("Data");
        return fileGrp;
    }
    
    public static void createStructMapRepresentationDiv(StructMapType structMapType, String representationId, FileGrp fileGrp) {
    	DivType presentationDivType = new DivType();
    	presentationDivType.setID("uuid-" + UUID.randomUUID().toString());
    	presentationDivType.setTYPE(REPRESENTATION);
    	presentationDivType.setLABEL(representationId);
    	structMapType.setDiv(presentationDivType);
    	
    	DivType dataDivType = new DivType();
    	dataDivType.setID("uuid-" + UUID.randomUUID().toString());
    	dataDivType.setTYPE("Data");
    	dataDivType.setLABEL("Data");
    	presentationDivType.getDiv().add(dataDivType);
    	
    	Fptr fptr = new Fptr();
		fptr.setFILEID(fileGrp);
		dataDivType.getFptr().add(fptr);
    }
    
    public static void addRepresentationToMETS(
    	Representation representation, 
    	ModelService modelService
    ) 
    	throws GenericException, 
    		IOException, 
    		DatatypeConfigurationException, 
    		IPException, 
    		JAXBException, 
    		NotFoundException, 
    		RequestNotValidException, 
    		SAXException, 
    		AuthorizationDeniedException 
    {
    	
    	AIP aip = modelService.retrieveAIP(representation.getAipId());
    	StoragePath aipPath = ModelUtils.getAIPStoragePath(representation.getAipId());
    	Path metsPath = OriginalMETSUtils.metsPath(
                modelService,
                aipPath
            );
        MetsWrapper metsWrapper = new MetsWrapper(
            METSUtils.instantiateMETSFromFile(metsPath),
            metsPath
        );
        
        DivType packageDivType = getStructMapPackageDiv(getStructMap(metsWrapper));
        DivType representationDivType = new DivType();
        representationDivType.setID("uuid-" + UUID.randomUUID().toString());
        representationDivType.setTYPE(REPRESENTATION);
        representationDivType.setLABEL(REPRESENTATIONS + "/" + representation.getId());
        packageDivType.getDiv().add(representationDivType);
        
        FileSec fileSec = metsWrapper.getMets().getFileSec();
        if (fileSec == null) {
        	fileSec = new FileSec();
            fileSec.setID("uuid-" + UUID.randomUUID().toString());
            metsWrapper.getMets().setFileSec(fileSec);   
        }
        
        FileGrp fileGrp = new FileGrp();
        fileGrp.setID("uuid-" + UUID.randomUUID().toString());
        fileGrp.setUSE(REPRESENTATIONS + "/" + representation.getId());
        fileSec.getFileGrp().add(fileGrp);
        
        FLocat flocat = new FLocat();
        flocat.setID("uuid-" + UUID.randomUUID().toString());
        flocat.setType("simple");
        flocat.setHref(REPRESENTATIONS.toLowerCase() + "/" + representation.getId() + "/" + IPConstants.METS_FILE);
        flocat.setLOCTYPE("URL");
        
        java.io.File file = modelService
                .getStorage()
                .getDirectAccess(aipPath)
                .getPath()
                .resolve(Path.of(IPConstants.REPRESENTATIONS_FOLDER))
                .resolve(Path.of(representation.getId()))
                .resolve(IPConstants.METS_FILE)
                .toFile();
        FileType fileType = new FileType();
        fileType.setID("uuid-" + UUID.randomUUID().toString());
        fileType.setMIMETYPE("text/xml");
        fileType.setCREATED(OriginalMETSUtils.currentDateAndTime());
        fileType.setSIZE(FileUtils.sizeOf(file));
        fileType.setCHECKSUM(
                DigestUtils.md5Hex(
                        java.nio.file.Files.readAllBytes(file.toPath())
                    ).toUpperCase());
        fileType.setCHECKSUMTYPE("MD5");
        fileType.getFLocat().add(flocat);
        fileGrp.getFile().add(fileType);
        
        DivType.Mptr mptr = new DivType.Mptr();
		mptr.setLOCTYPE("URL");
		mptr.setType("simple");
		mptr.setHref(REPRESENTATIONS.toLowerCase() + "/" + representation.getId() + "/" + IPConstants.METS_FILE);
		mptr.setTitle(fileGrp.getID());
		representationDivType.getMptr().add(mptr);
      
		metsWrapper.getMets().getMetsHdr()
			.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
    	METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, true);
    	if (aip.getParentId() != null) {
    		updateLastModDateInParentMETS(modelService.retrieveAIP(aip.getParentId()), modelService);
    	}
    }
    
    public static void removeRepresentationFromMETS(
    		ModelService modelService, 
    		String aipId, 
    		String representationId) 
    	throws 
        	AuthorizationDeniedException,
        	DatatypeConfigurationException,
        	GenericException, 
        	IOException, 
        	IPException, 
        	JAXBException, 
        	NotFoundException, 
        	RequestNotValidException, 
        	SAXException {
 
    	StoragePath aipPath = ModelUtils.getAIPStoragePath(aipId);
        Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath);
        MetsWrapper metsWrapper = new MetsWrapper(
            METSUtils.instantiateMETSFromFile(metsPath),
            metsPath
        );
        
        DivType packageDivType = getStructMapPackageDiv(getStructMap(metsWrapper));
        for (DivType dt : packageDivType.getDiv()) {
        	if (dt.getLABEL().equals(REPRESENTATIONS + "/" + representationId)) {
        		packageDivType.getDiv().remove(dt);
        		break;
        	}
        }
        
        FileSec fileSec = metsWrapper.getMets().getFileSec();
        if (fileSec != null) {
        	for (FileGrp fg : fileSec.getFileGrp()) {
        		if (fg.getUSE().equals(REPRESENTATIONS + "/" + representationId)) {
        			fileSec.getFileGrp().remove(fg);
        			break;
        		}
        	}
        }
	
    	AIP aip = modelService.retrieveAIP(aipId);
    	metsWrapper.getMets().getMetsHdr()
			.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
    	METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, true);
    	if (aip.getParentId() != null) {
    		updateLastModDateInParentMETS(modelService.retrieveAIP(aip.getParentId()), modelService);
    	}
    }

    public static void updateAIPTypeInMETS(AIP aip, ModelService modelService) {
    	try {
    		StoragePath aipPath = ModelUtils.getAIPStoragePath(aip.getId());
            Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath);
            MetsWrapper metsWrapper = new MetsWrapper(
                METSUtils.instantiateMETSFromFile(metsPath),
                metsPath
            );
            
            metsWrapper.getMets().setTYPE(aip.getType());
            metsWrapper.getMets().getMetsHdr()
				.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
        	METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, true);
        	if (aip.getParentId() != null) {
        		updateLastModDateInParentMETS(modelService.retrieveAIP(aip.getParentId()), modelService);
        	}
    		
		} catch (AuthorizationDeniedException
				| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
        		| RequestNotValidException
        		| SAXException e) {
    	
			LOG.error("Then update the AIP-type in AIP {} - Error: ", aip.getId(), e);
		}
    	
    }
    
    public static void updateRepresentationTypeInMETS(
    		Representation representation, 
    		ModelService modelService) {
    	
    	try {
    		Path representationPath = Path.of(
                    IPConstants.REPRESENTATIONS_FOLDER
                ).resolve(Path.of(representation.getId()));
    		StoragePath aipPath = ModelUtils.getAIPStoragePath(representation.getAipId());
    		Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
            MetsWrapper metsWrapper = new MetsWrapper(
                METSUtils.instantiateMETSFromFile(metsPath),
                metsPath
            );
            
            metsWrapper.getMets().setTYPE(representation.getType());
            
            AIP aip = modelService.retrieveAIP(representation.getAipId());
            metsWrapper.getMets().getMetsHdr()
				.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
        	METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, false);
        	updateLastModDateInParentMETS(aip, modelService);
	
		} catch (AuthorizationDeniedException
				| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
        		| RequestNotValidException
        		| SAXException e) {
    	
			LOG.error("Then update the Representation-type in {} within AIP {} - Error: ", representation.getId(), representation.getAipId(), e);
		}
    	
    }

    public static void addFilesystemObjectToMETS(
    		org.roda.core.data.v2.ip.File file,
    		ModelService modelService) {
    	
    	if (file.isDirectory()) {
    		addFolderToMETS(file, modelService);
    		
    	} else if (!file.isDirectory()) {
    		addFileToMETS(file, modelService);
    	}	
    }
    
    private static void addFolderToMETS(
    		org.roda.core.data.v2.ip.File file,
    		ModelService modelService) {
    	
    	try {
	    	AIP aip = modelService.retrieveAIP(file.getAipId());
	    	Optional<Representation> representation = 
	    			aip.getRepresentations()
	    			.stream()
	    			.filter(r -> r.getId().equals(file.getRepresentationId()))
	    			.findFirst();
	    	
	    	if (representation.isPresent()) {
	    		StoragePath aipPath = ModelUtils.getAIPStoragePath(file.getAipId());
	    		Path representationPath = Path.of(
	                    IPConstants.REPRESENTATIONS_FOLDER
	                )
	    			.resolve(Path.of(representation.get().getId()));
	    		
	    		Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
	        	MetsWrapper metsWrapper = new MetsWrapper(
	                METSUtils.instantiateMETSFromFile(metsPath),
	                metsPath
	            );
	        	
	        	DivType dataDiv = getStructMapRepresentationDataDiv(metsWrapper, representation.get().getId());
	        	
        		DivType dt = findDivByPath(dataDiv, file.getPath());
        		if (dt != null) {
        			dt.getDiv().add(createDivType("Folder", file.getId()));
        			
    	        	metsWrapper.getMets().getMetsHdr()
                		.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
    	        	METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, false);
    	        	updateLastModDateInParentMETS(modelService.retrieveAIP(representation.get().getAipId()), modelService);	
        		}	
	    	}
	    	
		} catch (AuthorizationDeniedException
				| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
        		| RequestNotValidException
        		| SAXException e) {
    	
			LOG.error("Then added folder {} to the representation {} METS file within AIP {} - Error: ", file.getId(), file.getRepresentationId(), file.getAipId(), e);
		}
    }
    
    private static void addFileToMETS(
    		org.roda.core.data.v2.ip.File file,
    		ModelService modelService) {
    	
    	try {
	    	AIP aip = modelService.retrieveAIP(file.getAipId());
	    	Optional<Representation> representation = 
	    			aip.getRepresentations()
	    			.stream()
	    			.filter(r -> r.getId().equals(file.getRepresentationId()))
	    			.findFirst();
	    	
	    	if (representation.isPresent()) {
	    		StoragePath aipPath = ModelUtils.getAIPStoragePath(file.getAipId());
	    		Path representationPath = Path.of(
	                    IPConstants.REPRESENTATIONS_FOLDER
	                )
	    			.resolve(Path.of(representation.get().getId()));
	    		
	    		Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
	        	MetsWrapper metsWrapper = new MetsWrapper(
	                METSUtils.instantiateMETSFromFile(metsPath),
	                metsPath
	            );
	        	
	        	DirectResourceAccess directAccess = modelService.getDirectAccess(file);
	        	String fileProps = SiegfriedPluginUtils.runSiegfriedOnPath(directAccess.getPath());	
	        	final JsonNode jsonObject = JsonUtils.parseJson(fileProps);
	            final JsonNode files = jsonObject.get("files");
	            String fileFullname = jsonObject.get("files").get(0).get("filename").toString().replaceAll("\"", "");
	            String filesize = jsonObject.get("files").get(0).get("filesize").toString().replaceAll("\"", "");
	            String modified = jsonObject.get("files").get(0).get("modified").toString().replaceAll("\"", "");
	            String mime = jsonObject.get("files").get(0).get("matches").get(0).get("mime").toString().replaceAll("\"", "");
	            
	        	FileType.FLocat flocat = new FileType.FLocat();
	            flocat.setID("uuid-" + UUID.randomUUID().toString());
	            flocat.setType("simple");
	            flocat.setHref(HREF_DATA_PREFIX + constructFilepath(file));
	            flocat.setLOCTYPE("URL");
	            
	            FileType fileType = new FileType();
	            fileType.setID("uuid-" + UUID.randomUUID().toString());
	            fileType.setMIMETYPE(mime);
	            fileType.setCREATED(OriginalMETSUtils.dateAndTimeFromUTC(modified));
	            fileType.setSIZE(Long.valueOf(filesize));
	            fileType.setCHECKSUM(
	                    DigestUtils.md5Hex(
	                            java.nio.file.Files.readAllBytes(Path.of(fileFullname))
	                        ).toUpperCase());
	            fileType.setCHECKSUMTYPE("MD5");
	            fileType.getFLocat().add(flocat);
	        	
	            FileGrp dataFileGrp = getFileGrp(metsWrapper, "Data");
	            dataFileGrp.getFile().add(fileType);

	            DivType dataDiv = getStructMapRepresentationDataDiv(metsWrapper, file.getRepresentationId());
	            DivType dt = findDivByPath(dataDiv, file.getPath());
        		if (dt != null) {
        			DivType.Fptr fptr = new DivType.Fptr();
        			fptr.setFILEID(fileType);
        			dt.getFptr().add(fptr);
        			
    	        	metsWrapper.getMets().getMetsHdr()
                		.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
    	        	METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, false);
    	        	updateLastModDateInParentMETS(modelService.retrieveAIP(representation.get().getAipId()), modelService);
        		}
	    	}
	    	
		} catch (AuthorizationDeniedException
        		| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
                | PluginException
        		| RequestNotValidException
        		| SAXException e) {
    	
			LOG.error("Then added file {} to the representation {} METS file within AIP {} - Error: ", file.getId(), file.getRepresentationId(), file.getAipId(), e);
		}
    }
    
    private static DivType getStructMapRepresentationDataDiv(MetsWrapper metsWrapper, String representationId) {
    	DivType representationDataDiv = null;
    	if (getStructMap(metsWrapper).getDiv().getLABEL().equals(representationId)) {
    		for (DivType dt : getStructMap(metsWrapper).getDiv().getDiv()) {
    			if (dt.getLABEL().toLowerCase().equals("data")) {
    				representationDataDiv = dt;
    			}
    		}
    	}

    	return representationDataDiv;
    }
    
    private static FileGrp getFileGrp(MetsWrapper metsWrapper, String useLabel){
    	if (metsWrapper.getMets().getFileSec() == null) {
    		ObjectFactory of = new ObjectFactory();
    		FileSec fileSec = of.createMetsTypeFileSec();
    		fileSec.setID("uuid-" + UUID.randomUUID().toString());
    		metsWrapper.getMets().setFileSec(fileSec);
    	}
    	
    	FileSec.FileGrp fileGrp = null;
    	for (FileSec.FileGrp fg : metsWrapper.getMets().getFileSec().getFileGrp()) {
    		if (fg.getUSE().equals(useLabel)) {
    			fileGrp = fg;
    			break;
    		}
    	}
    	
    	if (fileGrp == null) {
    		fileGrp = new FileSec.FileGrp();
    		fileGrp.setID("uuid-" + UUID.randomUUID().toString());
    		fileGrp.setUSE(useLabel);
    		metsWrapper.getMets().getFileSec().getFileGrp().add(fileGrp);
    	}
    	
    	return fileGrp;
    }
    
    private static String constructFilepath(org.roda.core.data.v2.ip.File file) {
    	String filepath = "";
    	if (!file.getPath().isEmpty()) {
    		filepath = file.getPath().stream().collect(Collectors.joining("/"))  + "/";
    	}
    	return filepath + file.getId();
    }
    
    public static void removeFilesystemObjectFromMETS(
    		org.roda.core.data.v2.ip.File file,
    		ModelService modelService) {
    	
    	if (file.isDirectory()) {
    		removeFolderFromMETS(file, modelService);
    		
    	} else if (!file.isDirectory()) {
    		removeFileFromMETS(file, modelService);
    	}
    }
    
    private static DivType findDivByPath(DivType topDiv, List<String> path) {
    	if (topDiv == null) {
    		return null;
    	}
    	
    	if (path.isEmpty()) {
    		return topDiv;	
    	}
    
    	DivType nextDiv = null;
		for (DivType divType : topDiv.getDiv()) {
			if (divType.getLABEL().equals(path.getFirst())) {
				nextDiv = divType;
				path.remove(0);
				break;
			}	
		}
	    
		return findDivByPath(nextDiv, path);
    }
    
    private static List<DivType> findSubDiv(DivType topDiv, List<DivType> subDivType) {
    	if (topDiv == null) {
    		return null;
    	}
    	
		for (DivType divType : topDiv.getDiv()) {
				subDivType.add(divType);
				return findSubDiv(divType, subDivType);	
		}
	    
		return subDivType;
    }
    
    private static void removeFolderFromMETS(
    		org.roda.core.data.v2.ip.File file,
    		ModelService modelService) {
    	
    	try {
	    	AIP aip = modelService.retrieveAIP(file.getAipId());
	    	Optional<Representation> representation = 
	    			aip.getRepresentations()
	    			.stream()
	    			.filter(r -> r.getId().equals(file.getRepresentationId()))
	    			.findFirst();
	    	
	    	if (representation.isPresent()) {
	    		StoragePath aipPath = ModelUtils.getAIPStoragePath(file.getAipId());
	    		Path representationPath = Path.of(
	                    IPConstants.REPRESENTATIONS_FOLDER
	                )
	    			.resolve(Path.of(representation.get().getId()));
	    		
	    		Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
	        	MetsWrapper metsWrapper = new MetsWrapper(
	                METSUtils.instantiateMETSFromFile(metsPath),
	                metsPath
	            );
	        	
	        	DivType dataDiv = getStructMapRepresentationDataDiv(metsWrapper, representation.get().getId());
	        	DivType parentDiv = findDivByPath(dataDiv, file.getPath());
	        	if (parentDiv != null) {
	        		List<String> path = new ArrayList<String>();
	        		path.add(file.getId());
	        		DivType divTypeToRemove = findDivByPath(parentDiv, path);
		        	if (divTypeToRemove != null) {
		        		
		        		// When removing folders with subfolders we have to cleanup filesec.
		        		FileGrp dataFileGrp = getFileGrp(metsWrapper, "Data");
			        	List<FileType> filesToRemove = new ArrayList<>();
			        	
			        	List<DivType> subDivType = new ArrayList<DivType>();
			        	subDivType.add(divTypeToRemove);
			        	for (DivType dt : findSubDiv(divTypeToRemove, subDivType)) {
			        		for (Fptr fptr : dt.getFptr()) {
					        	for (FileType fileType : dataFileGrp.getFile()) {
				    				if (fptr.getFILEID().equals(fileType)) {
				    					filesToRemove.add(fileType);
				    				}
					        	}
				    		}
			        	}
			        	
		        		for (FileType ft : filesToRemove) {
		        			dataFileGrp.getFile().remove(ft);
		        		}
			        	
		        		parentDiv.getDiv().remove(divTypeToRemove);
		        		metsWrapper.getMets().getMetsHdr()
                			.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
		        		METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, false);
		        		updateLastModDateInParentMETS(modelService.retrieveAIP(representation.get().getAipId()), modelService);
		        	}
	        	}
	    	}
	    	
		} catch (AuthorizationDeniedException
				| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
        		| RequestNotValidException
        		| SAXException e) {
    	
			LOG.error("Then removed file {} from the representation {} METS file within AIP {} - Error: ", file.getId(), file.getRepresentationId(), file.getAipId(), e);
		}
    }
    
    private static void removeFileFromMETS(
    		org.roda.core.data.v2.ip.File file,
    		ModelService modelService) {
    	
    	try {
	    	AIP aip = modelService.retrieveAIP(file.getAipId());
	    	Optional<Representation> representation = 
	    			aip.getRepresentations()
	    			.stream()
	    			.filter(r -> r.getId().equals(file.getRepresentationId()))
	    			.findFirst();
	    	
	    	if (representation.isPresent()) {
	    		StoragePath aipPath = ModelUtils.getAIPStoragePath(file.getAipId());
	    		Path representationPath = Path.of(
	                    IPConstants.REPRESENTATIONS_FOLDER
	                )
	    			.resolve(Path.of(representation.get().getId()));
	    		
	    		Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
	        	MetsWrapper metsWrapper = new MetsWrapper(
	                METSUtils.instantiateMETSFromFile(metsPath),
	                metsPath
	            );
	        	
	        	FileGrp dataFileGrp = getFileGrp(metsWrapper, "Data");
	        	List<FileType> filesToRemove = new ArrayList<>();
	        	for (FileType fileType : dataFileGrp.getFile()) {
	    			for (FileType.FLocat flocat : fileType.getFLocat()) {
	    				if (flocat.getHref().equals(HREF_DATA_PREFIX + constructFilepath(file))) {
	    					filesToRemove.add(fileType);
	    				}
	    			}
	        	}
	        	
	        	DivType dataDiv = getStructMapRepresentationDataDiv(metsWrapper, file.getRepresentationId());
	        	
	        	if (!filesToRemove.isEmpty()) {
	        		for (FileType fileType : filesToRemove) {
	        			dataFileGrp.getFile().remove(fileType);
	        			DivAndFptr divAndFptr = findDivAndFptrByPathAndFileId(dataDiv, file.getPath(), fileType);
		        		DivType dt = divAndFptr.getDivType();
		        		dt.getFptr().remove(divAndFptr.getFptr());
	        		}
	        				
	        		metsWrapper.getMets().getMetsHdr()
                		.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
	        		METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, false);
	        		updateLastModDateInParentMETS(modelService.retrieveAIP(representation.get().getAipId()), modelService);
	        	}
	    	}
	    	
		} catch (AuthorizationDeniedException
				| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
        		| RequestNotValidException
        		| SAXException e) {
    	
			LOG.error("Then removed file {} from the representation {} METS file within AIP {} - Error: ", file.getId(), file.getRepresentationId(), file.getAipId(), e);
		}
    }
    
    private static DivAndFptr findDivAndFptrByPathAndFileId(DivType topDiv, List<String> path, FileType fileType) {
    	DivAndFptr divAndFptr = null;
    	DivType divType = findDivByPath(topDiv, path);
		for (Fptr fptr : divType.getFptr()) {
			if (fptr.getFILEID().equals(fileType)) {
				divAndFptr = new DivAndFptr(divType, fptr);
				break;
			}		
		}
	    
		return divAndFptr;
    }

    private static FileType findFileById(MetsWrapper metsWrapper, String id) {
    	FileType file = null;
    	FileGrp dataFileGrp = getFileGrp(metsWrapper, "Data");
    	for (FileType f : dataFileGrp.getFile()) {
			if (file.getID().equals(id)) {
				file = f;
				break;
			}
    	}
    	
    	return file;
    }
    
    public static void moveFilesystemObjectInMETS(
    		org.roda.core.data.v2.ip.File fileToMove,
    		org.roda.core.data.v2.ip.File newFile,
    		ModelService modelService) {
    	
    	if (fileToMove.isDirectory()) {
    		moveFolderInMETS(fileToMove, newFile, modelService);
    		
    	} else if (!fileToMove.isDirectory()) {
    		moveFileInMETS(fileToMove, newFile, modelService);
    	}
    }
    
    private static void moveFolderInMETS(
    		org.roda.core.data.v2.ip.File fileToMove,
    		org.roda.core.data.v2.ip.File newFile,
    		ModelService modelService) {
    	
    	try {
	    	AIP aip = modelService.retrieveAIP(fileToMove.getAipId());
	    	Optional<Representation> representation = 
	    			aip.getRepresentations()
	    			.stream()
	    			.filter(r -> r.getId().equals(fileToMove.getRepresentationId()))
	    			.findFirst();
	    	
	    	if (representation.isPresent()) {
	    		StoragePath aipPath = ModelUtils.getAIPStoragePath(fileToMove.getAipId());
	    		Path representationPath = Path.of(
	                    IPConstants.REPRESENTATIONS_FOLDER
	                )
	    			.resolve(Path.of(representation.get().getId()));
	    		
	    		Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
	        	MetsWrapper metsWrapper = new MetsWrapper(
	                METSUtils.instantiateMETSFromFile(metsPath),
	                metsPath
	            );
	        	
	        	String oldFilePath = HREF_DATA_PREFIX + constructFilepath(fileToMove);
	        	String newFilePath = HREF_DATA_PREFIX + constructFilepath(newFile);
	        	
	        	DivType dataDiv = getStructMapRepresentationDataDiv(metsWrapper, fileToMove.getRepresentationId());
	        	DivType parentDiv = findDivByPath(dataDiv, fileToMove.getPath());
	        	
	        	if (parentDiv != null) {
	        		List<String> path = new ArrayList<String>();
	        		path.add(fileToMove.getId());
	        		DivType divTypeToMove = findDivByPath(parentDiv, path);
		        	if (divTypeToMove != null) {
		        		parentDiv.getDiv().remove(divTypeToMove);
		        		
		        		DivType newParentDiv = findDivByPath(dataDiv, newFile.getPath());
		        		if (newParentDiv != null) {
		        			newParentDiv.getDiv().add(divTypeToMove);

		        			renameFlocatByHref(
		        					metsWrapper, 
		        					oldFilePath, 
		        					newFilePath);
		        		}
		        		
		        		metsWrapper.getMets().getMetsHdr()
	                		.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
		        		METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, false);
		        		updateLastModDateInParentMETS(modelService.retrieveAIP(representation.get().getAipId()), modelService);
		        	}
	        	}
	    	}
	    	
		} catch (AuthorizationDeniedException
				| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
        		| RequestNotValidException
        		| SAXException e) {
    	
			LOG.error("Then moved folder {} in the representation {} METS file within AIP {} - Error: ", fileToMove.getId(), fileToMove.getRepresentationId(), fileToMove.getAipId(), e);
		}
    }
    
    private static void moveFileInMETS(
    		org.roda.core.data.v2.ip.File fileToMove,
    		org.roda.core.data.v2.ip.File newFile,
    		ModelService modelService) {
    	
    	try {
	    	AIP aip = modelService.retrieveAIP(fileToMove.getAipId());
	    	Optional<Representation> representation = 
	    			aip.getRepresentations()
	    			.stream()
	    			.filter(r -> r.getId().equals(fileToMove.getRepresentationId()))
	    			.findFirst();
	    	
	    	if (representation.isPresent()) {
	    		StoragePath aipPath = ModelUtils.getAIPStoragePath(fileToMove.getAipId());
	    		Path representationPath = Path.of(
	                    IPConstants.REPRESENTATIONS_FOLDER
	                )
	    			.resolve(Path.of(representation.get().getId()));
	    		
	    		Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
	        	MetsWrapper metsWrapper = new MetsWrapper(
	                METSUtils.instantiateMETSFromFile(metsPath),
	                metsPath
	            );
	        	
	        	String oldFilePath = HREF_DATA_PREFIX + constructFilepath(fileToMove);
	        	String newFilePath = HREF_DATA_PREFIX + constructFilepath(newFile);
	        	
	        	FileGrp dataFileGrp = getFileGrp(metsWrapper, "Data");
	        	FileType fileTypeToMove = null;
	        	for (FileType fileType : dataFileGrp.getFile()) {
	    			for (FileType.FLocat flocat : fileType.getFLocat()) {
	    				if (flocat.getHref().equals(HREF_DATA_PREFIX + constructFilepath(fileToMove))) {
	    					fileTypeToMove = fileType;
	    				}
	    			}
	        	}
	        	
	        	DivType dataDiv = getStructMapRepresentationDataDiv(metsWrapper, fileToMove.getRepresentationId());
	        	DivAndFptr divAndFptr = findDivAndFptrByPathAndFileId(dataDiv, fileToMove.getPath(), fileTypeToMove);
	        	if (divAndFptr != null) {
	        		DivType dt = divAndFptr.getDivType();
	        		dt.getFptr().remove(divAndFptr.getFptr());
	        	
	        		DivType newParentDiv = findDivByPath(dataDiv, newFile.getPath());
	        		if (newParentDiv != null) {
	        			newParentDiv.getFptr().add(divAndFptr.getFptr());
		        	
	        			renameFlocatByHref(
	        					metsWrapper, 
	        					oldFilePath, 
	        					newFilePath);
		        		
		        		metsWrapper.getMets().getMetsHdr()
	                		.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
		        		METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, false);
		        		updateLastModDateInParentMETS(modelService.retrieveAIP(representation.get().getAipId()), modelService);
		        	}
	        	}
	    	}
	    	
		} catch (AuthorizationDeniedException
				| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
        		| RequestNotValidException
        		| SAXException e) {
    	
			LOG.error("Then moved file {} in the representation {} METS file within AIP {} - Error: ", fileToMove.getId(), fileToMove.getRepresentationId(), fileToMove.getAipId(), e);
		}
    }
    
    private static void renameFlocatByHref(MetsWrapper metsWrapper, String oldHref, String newHref) {
    	FileType file = null;
    	FileGrp dataFileGrp = getFileGrp(metsWrapper, "Data");
    	
    	String oldPath = oldHref.substring(0, oldHref.lastIndexOf("/"));
    	for (FileType f : dataFileGrp.getFile()) {
    		for (FileType.FLocat flocat : f.getFLocat() ) {
    			if (flocat.getHref().matches("^" + oldPath + ".*")) {
    				String path = flocat.getHref();
    				flocat.setHref(path.replaceFirst(oldHref, newHref));
    				break;
    			}
    		}
    	}
    }
    
    public static void renameFilesystemObjectInMETS(
    		org.roda.core.data.v2.ip.File fileToRename,
    		org.roda.core.data.v2.ip.File newFile,
    		ModelService modelService) {
    	
    	if (fileToRename.isDirectory()) {
    		renameFolderInMETS(fileToRename, newFile, modelService);	
    	}
    }
    
    private static void renameFolderInMETS(
    		org.roda.core.data.v2.ip.File fileToRename,
    		org.roda.core.data.v2.ip.File newFile,
    		ModelService modelService) {
    	
    	try {
	    	AIP aip = modelService.retrieveAIP(fileToRename.getAipId());
	    	Optional<Representation> representation = 
	    			aip.getRepresentations()
	    			.stream()
	    			.filter(r -> r.getId().equals(fileToRename.getRepresentationId()))
	    			.findFirst();
	    	
	    	if (representation.isPresent()) {
	    		StoragePath aipPath = ModelUtils.getAIPStoragePath(fileToRename.getAipId());
	    		Path representationPath = Path.of(
	                    IPConstants.REPRESENTATIONS_FOLDER
	                )
	    			.resolve(Path.of(representation.get().getId()));
	    		
	    		Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
	        	MetsWrapper metsWrapper = new MetsWrapper(
	                METSUtils.instantiateMETSFromFile(metsPath),
	                metsPath
	            );
	        	
	        	String oldFilePath = HREF_DATA_PREFIX + constructFilepath(fileToRename);
	        	String newFilePath = HREF_DATA_PREFIX + constructFilepath(newFile);
	        	fileToRename.getPath().add(fileToRename.getId());
	        	DivType dataDiv = getStructMapRepresentationDataDiv(metsWrapper, fileToRename.getRepresentationId());
	        	
        		DivType divTypeToRename = findDivByPath(dataDiv, fileToRename.getPath());
	        	if (divTypeToRename != null) {
	        		divTypeToRename.setLABEL(newFile.getId());
	        		
        			renameFlocatByHref(
        					metsWrapper, 
        					oldFilePath, 
        					newFilePath);
	        		
	        		metsWrapper.getMets().getMetsHdr()
                		.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
	        		METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, false);
	        		updateLastModDateInParentMETS(modelService.retrieveAIP(representation.get().getAipId()), modelService);
	        	}
	    	}
	    	
		} catch (AuthorizationDeniedException
				| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
        		| RequestNotValidException
        		| SAXException e) {
    	
			LOG.error("Then moved folder {} in the representation {} METS file within AIP {} - Error: ", fileToRename.getId(), fileToRename.getRepresentationId(), fileToRename.getAipId(), e);
		}
    }
    
    private static void updateLastModDateInParentMETS(
    		AIP aip,
    		ModelService modelService)
        throws 
        	AuthorizationDeniedException,
        	DatatypeConfigurationException,
        	GenericException, 
        	IOException, 
        	IPException, 
        	JAXBException, 
        	NotFoundException, 
        	RequestNotValidException, 
        	SAXException {
    	
    	StoragePath aipPath = ModelUtils.getAIPStoragePath(aip.getId());
    	Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath);
    	MetsWrapper metsWrapper = new MetsWrapper(
                METSUtils.instantiateMETSFromFile(metsPath),
                metsPath
            );
    	
    	metsWrapper.getMets().getMetsHdr()
    		.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
    	
    	METSUtils.marshallMETS(metsWrapper.getMets(), metsPath, true);
    	
    	if (aip.getParentId() != null) {
    		updateLastModDateInParentMETS(modelService.retrieveAIP(aip.getParentId()), modelService);
    	}
    }
    
    static class DivAndFptr{
    	private DivType divType;
		private Fptr fptr;
    	
    	public DivAndFptr(DivType divType, Fptr fptr) {
    		this.divType = divType;
    		this.fptr = fptr;
    	}
    	
    	public DivType getDivType() {
			return divType;
		}

		public Fptr getFptr() {
			return fptr;
		}
    }
}
