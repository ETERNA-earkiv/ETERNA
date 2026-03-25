package org.roda.core.plugins.base.originalmets;

import jakarta.xml.bind.JAXBException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDateTime;
import org.roda.core.data.common.RodaConstants;

import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;

import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.StorageService;

import org.roda_project.commons_ip.utils.IPException;

import org.roda_project.commons_ip2.validator.utils.CHECKSUMTYPE;
import org.roda_project.commons_ip2.mets_v1_12.beans.Mets;
import org.roda_project.commons_ip2.mets_v1_12.beans.StructMapType;
import org.roda_project.commons_ip2.mets_v1_12.beans.AmdSecType;
import org.roda_project.commons_ip2.mets_v1_12.beans.DivType;
import org.roda_project.commons_ip2.mets_v1_12.beans.MdSecType;
import org.roda_project.commons_ip2.model.IPConstants;
import org.roda_project.commons_ip2.model.MetsWrapper;
import org.roda_project.commons_ip2.utils.METSUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.SAXException;

import io.jsonwebtoken.lang.Arrays;

/**
 * Updates the original METS file(s) in the AIP
 * 
 * @author Paul Hedberg <paul.hedberg@skane.se>
 */
public class UpdateOriginalMETS {
	private static final Logger LOG = LoggerFactory.getLogger(UpdateOriginalMETS.class);
	
	private static final String PREMIS_FILE_TYPE = ".xml";
	private static final String REPRESENTATION_FILE_PREFIX = "urn:roda:premis:representation:";
	
	private static final String RELATEDPACKAGE = "RelatedPackage";
	private static final String ANCESTORS = "Parent";
	private static final String DESCENDENTS = "Children";
	
	public static void update(PreservationMetadata pm,  ModelService modelService) {
		
		boolean keepOriginalMets = RodaCoreFactory.getProperty(RodaConstants.CORE_PLUGINS_BASE_KEEP_ORIGINAL_METS, false);
		if (!keepOriginalMets) {
			return;
		}
		
		try {
			StoragePath aipPath = ModelUtils.getAIPStoragePath(pm.getAipId());
			
			// ROOT METS
			if (pm.getRepresentationId() == null) {
				Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath);
				Mets mets = METSUtils.instantiateMETSFromFile(metsPath);
		    	MetsWrapper metsWrapper = new MetsWrapper(mets, metsPath); 
		    	
		    	handleStructurMap(modelService, pm.getAipId(), metsWrapper);
		    	
		    	java.io.File file = modelService.getStorage().getDirectAccess(aipPath).getPath().resolve(Path.of(IPConstants.PRESERVATION_FOLDER)).resolve(pm.getId() + PREMIS_FILE_TYPE).toFile();
		    	if (file.exists()) {
			    	String relativeFilePath = IPConstants.METADATA + java.io.File.separator + IPConstants.PRESERVATION + java.io.File.separator + file.getName();
			    	Optional<MdSecType> fileInMets = metsWrapper.getMets().getAmdSec().getFirst().getDigiprovMD().stream().filter(m -> m.getMdRef().getHref().equals(relativeFilePath)).findFirst();
			    	if (!fileInMets.isPresent()) {
			    		addToMETS(relativeFilePath, file, mets, metsPath, true);
			    	}
		    	}
		    	
		    // REPRESENTATION METS
			} else {
				Path representationPath = Path.of(IPConstants.REPRESENTATIONS_FOLDER).resolve(Path.of(pm.getRepresentationId()));
				Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
				Mets mets = METSUtils.instantiateMETSFromFile(metsPath);
		    	MetsWrapper metsWrapper = new MetsWrapper(mets, metsPath);
				
		    	Path subFolders = Path.of(pm.getFileDirectoryPath().stream().map(p -> p).collect(Collectors.joining(java.io.File.separator)));
		    	
		    	String fileName = "";
		    	if (pm.getType().name().equals("FILE"))
		    		fileName = pm.getId() + PREMIS_FILE_TYPE;
		    	if (pm.getType().name().equals("EVENT"))
		    		fileName = pm.getId() + "%3A" + pm.getFileId() + PREMIS_FILE_TYPE;
		    	
		    	java.io.File file = modelService.getStorage().getDirectAccess(aipPath).getPath()
		    			.resolve(Path.of(IPConstants.REPRESENTATIONS_FOLDER))
		    			.resolve(Path.of(pm.getRepresentationId()))
		    			.resolve(Path.of(IPConstants.PRESERVATION_FOLDER))
		    			.resolve(subFolders)
		    			.resolve(fileName).toFile();
		    	
		    	if (file.exists()) {
			    	String relativeFilePath = IPConstants.METADATA + java.io.File.separator + IPConstants.PRESERVATION + java.io.File.separator + pm.getFileDirectoryPath().stream().map(p -> p).collect(Collectors.joining(java.io.File.separator)) + java.io.File.separator + file.getName();
			    	Optional<MdSecType> fileInMets = metsWrapper.getMets().getAmdSec().getFirst().getDigiprovMD().stream().filter(m -> m.getMdRef().getHref().equals(relativeFilePath)).findFirst();
			    	if (!fileInMets.isPresent()) {
			    		addToMETS(relativeFilePath, file, mets, metsPath, false);
			    	} 		    	
		    	}
			}

		} catch (AuthorizationDeniedException 
					| DatatypeConfigurationException 
					| GenericException 
					| IOException 
					| IPException
					| JAXBException
					| NotFoundException
					| RequestNotValidException
					| SAXException e) {
			
			LOG.error("Then update the original AIP {} METS file - Error: {}", pm.getAipId(), e);
		}
	}
	
	// REPRESENTATION
	public static void handleRepresentation(String aipId, String representationId, String representationUUID, ModelService modelService) {
		
		boolean keepOriginalMets = RodaCoreFactory.getProperty(RodaConstants.CORE_PLUGINS_BASE_KEEP_ORIGINAL_METS, false);
		if (!keepOriginalMets) {
			return;
		}
		
		try {
			StoragePath aipPath = ModelUtils.getAIPStoragePath(aipId);
			Path representationPath = Path.of(IPConstants.REPRESENTATIONS_FOLDER).resolve(Path.of(representationId));
			Path metsPath = OriginalMETSUtils.metsPath(modelService, aipPath, representationPath);
			Mets mets = METSUtils.instantiateMETSFromFile(metsPath);
	    	MetsWrapper metsWrapper = new MetsWrapper(mets, metsPath);
	    	
	    	String fileName = REPRESENTATION_FILE_PREFIX + representationUUID + PREMIS_FILE_TYPE;
	    	java.io.File file = modelService.getStorage().getDirectAccess(aipPath).getPath()
	    			.resolve(Path.of(IPConstants.REPRESENTATIONS_FOLDER))
	    			.resolve(Path.of(representationId))
	    			.resolve(Path.of(IPConstants.PRESERVATION_FOLDER))
	    			.resolve(fileName).toFile();
	    
	    	if (file.exists()) {
		    	String relativeFilePath = IPConstants.METADATA + java.io.File.separator + IPConstants.PRESERVATION + java.io.File.separator + file.getName();
		    
		    	Optional<MdSecType> fileInMets = metsWrapper.getMets().getAmdSec().getFirst().getDigiprovMD().stream().filter(m -> m.getMdRef().getHref().equals(relativeFilePath)).findFirst();
		    	if (!fileInMets.isPresent()) {
		    		addToMETS(relativeFilePath, file, mets, metsPath, false);
		    	}
	    	}		
		
		} catch (AuthorizationDeniedException 
				| DatatypeConfigurationException 
				| GenericException 
				| IOException 
				| IPException
				| JAXBException
				| NotFoundException
				| RequestNotValidException
				| SAXException e) {
			
			LOG.error("Then update the original AIP {} representation {} METS file - Error: {}", aipId, representationId, e);
		}
		
	}
	
	// STRUCTUREMAP
	private static void handleStructurMap(ModelService modelService, String aipId, MetsWrapper metsWrapper)
			throws AuthorizationDeniedException, 
				GenericException, 
				IOException, 
				IPException, 
				JAXBException, 
				NotFoundException, 
				RequestNotValidException, 
				SAXException {
		
    	AIP aip = modelService.retrieveAIP(aipId);
    	AIP parentAIP = null;
    	
    	if (aip.getParentId() != null){
    		parentAIP = modelService.retrieveAIP( modelService.retrieveAIP(aipId).getParentId() );
    	}
    	
    	StructMapType structMapType = getStructureMap(metsWrapper);	
    	Optional<DivType> optionalAncestorDivType = structMapType.getDiv().getDiv().stream()
    			.filter(d -> d.getLABEL().equals(ANCESTORS))
    			.findFirst(); 

    	if (parentAIP != null) {
    		final String pId = parentAIP.getId();
    		
	    	DivType ancestorDivType;
    		if (!optionalAncestorDivType.isPresent()) {
	    		ancestorDivType = createDivType(ANCESTORS);
	    		structMapType.getDiv().getDiv().add(ancestorDivType);
	    		
	    	} else {
	    		ancestorDivType = optionalAncestorDivType.get();
	    	}
	    	
    		Optional<DivType.Mptr> optionalDivTypeMptr = ancestorDivType.getMptr().stream()
    				.filter(m -> m.getHref().equals(pId))
    				.findFirst();

    		if (!optionalDivTypeMptr.isPresent()) {
    			Optional<DivType.Mptr> optionalOldDivTypeMptr = ancestorDivType.getMptr().stream()
        				.findFirst();
    			if (optionalOldDivTypeMptr.isPresent()) {
    				removeDescendentFromParent(modelService, aipId, optionalOldDivTypeMptr.get().getHref());
    			}
    			
    			ancestorDivType.getMptr().clear();
    			
    			DivType.Mptr divTypeMptr = createMptrDivType(parentAIP.getId());
    			ancestorDivType.getMptr().add(divTypeMptr);
    			addDescendentToParent(modelService, aipId, parentAIP.getId());		
    		}	
    		
    	} else {
    		if (optionalAncestorDivType.isPresent()) {
	    		DivType ancestorDivType = optionalAncestorDivType.get();
	    		
	    		Optional<DivType.Mptr> optionalDivTypeMptr = ancestorDivType.getMptr().stream().findFirst();
	    		if (optionalDivTypeMptr.isPresent()) {
	    			removeDescendentFromParent(modelService, aipId, optionalDivTypeMptr.get().getHref());
	    		}
	    		
	    		ancestorDivType.getMptr().clear();
    		}
    	}	
	}
	
	private static StructMapType getStructureMap(MetsWrapper metsWrapper) {
		List<StructMapType> structMapTypeList = metsWrapper.getMets().getStructMap().stream()
    			.filter(s -> s.getLABEL().equals(RELATEDPACKAGE)).toList();
		
		StructMapType structMapType;
		if (!structMapTypeList.isEmpty()) {	
	    	structMapType = structMapTypeList.getFirst();
	    	
    	} else {
    		structMapType = createStructureMap();
    		metsWrapper.getMets().getStructMap().add(structMapType);
    	}
		
		return structMapType;	
	}
	
	private static StructMapType createStructureMap() {
		StructMapType structMapType = new StructMapType();
    	structMapType.setID("uuid-" + UUID.randomUUID().toString());
    	structMapType.setLABEL(RELATEDPACKAGE);
    	DivType dt = new DivType();
    	dt.setID("uuid-" + UUID.randomUUID().toString());
    	structMapType.setDiv(dt);
    	return structMapType;
	}
	
	private static void addDescendentToParent(ModelService modelService, String aipId, String parentAipId)
			throws AuthorizationDeniedException, 
				GenericException, 
				IOException, 
				IPException, 
				JAXBException, 
				NotFoundException, 
				RequestNotValidException, 
				SAXException {
		
		StoragePath parentAipPath = ModelUtils.getAIPStoragePath(parentAipId);	
		Path metsPath = OriginalMETSUtils.metsPath(modelService, parentAipPath);
		Mets mets = METSUtils.instantiateMETSFromFile(metsPath);
    	MetsWrapper metsWrapper = new MetsWrapper(mets, metsPath);
    	
    	StructMapType structMapType = getStructureMap(metsWrapper);	
    	
    	Optional<DivType> optionalDescendentsDivType = structMapType.getDiv().getDiv().stream().filter(f -> f.getLABEL().equals(DESCENDENTS)).findFirst();
    	
		if (!optionalDescendentsDivType.isPresent()) {
			DivType descendentsDivType = createDivType(DESCENDENTS);
    		structMapType.getDiv().getDiv().add(descendentsDivType);
    		descendentsDivType.getMptr().add(createMptrDivType(aipId));
    		METSUtils.marshallMETS(mets, metsPath, true);
    		
    	} else {
    		Optional<DivType.Mptr> optionalDivTypeMptr = optionalDescendentsDivType.get().getMptr().stream()
    				.filter(m -> m.getHref().equals(aipId))
    				.findFirst();
    				
    		if (!optionalDivTypeMptr.isPresent()) {
    			optionalDescendentsDivType = structMapType.getDiv().getDiv().stream().filter(f -> f.getLABEL().equals(DESCENDENTS)).findFirst();
    			optionalDescendentsDivType.get().getMptr().add(createMptrDivType(aipId));
    			METSUtils.marshallMETS(mets, metsPath, true);
    		}    		
    	}	
	}
	
	private static void removeDescendentFromParent(ModelService modelService, String aipId, String parentAipId)
			throws AuthorizationDeniedException, 
				GenericException, 
				IOException, 
				IPException, 
				JAXBException, 
				NotFoundException, 
				RequestNotValidException, 
				SAXException {
	
		StoragePath parentAipPath = ModelUtils.getAIPStoragePath(parentAipId);	
		Path metsPath = OriginalMETSUtils.metsPath(modelService, parentAipPath);
		Mets mets = METSUtils.instantiateMETSFromFile(metsPath);
    	MetsWrapper metsWrapper = new MetsWrapper(mets, metsPath);
    	
    	StructMapType structMapType = getStructureMap(metsWrapper);		
    	Optional<DivType> optionalDescendentsDivType = structMapType.getDiv().getDiv().stream().filter(f -> f.getLABEL().equals(DESCENDENTS)).findFirst();
    	
    	if (optionalDescendentsDivType.isPresent()) {
    		Optional<DivType.Mptr> optionalDivTypeMptr = optionalDescendentsDivType.get().getMptr().stream().filter(m -> m.getHref().equals(aipId)).findFirst();
    		if (optionalDivTypeMptr.isPresent()) {
    			optionalDescendentsDivType.get().getMptr().remove(optionalDivTypeMptr.get()); 
    			METSUtils.marshallMETS(mets, metsPath, true);
    		}
    	}
	}
	
	// HANDLE PARENT WHEN REMOVED AIP
	public static void handleParentWhenRemovedAIP(ModelService modelService, String removedAipId, String parentAipId)
			throws AuthorizationDeniedException, 
			GenericException, 
			IOException, 
			IPException, 
			JAXBException, 
			NotFoundException, 
			RequestNotValidException, 
			SAXException {

		boolean keepOriginalMets = RodaCoreFactory.getProperty(RodaConstants.CORE_PLUGINS_BASE_KEEP_ORIGINAL_METS, false);
		if (!keepOriginalMets) {
			return;
		}
		
		removeDescendentFromParent(modelService, removedAipId, parentAipId);
	}
	
	private static DivType createDivType(String label) {
		DivType dt = new DivType();
		dt.setID("uuid-" + UUID.randomUUID().toString());
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

	private static void addToMETS(String relativeFilePath, java.io.File file, Mets mets, Path metsPath, boolean isRoot) 
			throws IOException, DatatypeConfigurationException, IPException, JAXBException {
		
		MdSecType mdSecType = new MdSecType();
    	mdSecType.setID("uuid-" + UUID.randomUUID().toString());
    	mdSecType.setGROUPID("PREMIS");
    	MdSecType.MdRef ref = new MdSecType.MdRef();
    	ref.setID("uuid-" + UUID.randomUUID().toString());
    	ref.setHref(relativeFilePath);
    	ref.setType("simple");
    	
    	ref.setCHECKSUM(DigestUtils.md5Hex(java.nio.file.Files.readAllBytes(file.toPath())).toUpperCase());
    	ref.setCHECKSUMTYPE("MD5");
    	
    	ref.setSIZE(FileUtils.sizeOf(file));
    	ref.setLOCTYPE("URL");
    	ref.setMDTYPE("PREMIS");
    	ref.setMIMETYPE("text/xml");
    	ref.setCREATED(OriginalMETSUtils.currentDateAndTime());
    	
    	mdSecType.setMdRef(ref);
    	
    	mets.getAmdSec().getFirst().getDigiprovMD().add(mdSecType);
    	mets.getMetsHdr().setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
    		
    	METSUtils.marshallMETS(mets, metsPath, isRoot);
	}
}
