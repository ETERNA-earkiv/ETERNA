/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.originalmets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.utils.JsonUtils;
import org.roda.core.data.utils.XMLUtils;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.StorageService;
import org.roda.core.storage.StringContentPayload;
import org.roda.core.storage.fs.FSPathContentPayload;
import org.roda_project.commons_ip2.validator.utils.CHECKSUMTYPE;
import org.roda_project.commons_ip2.mets_v1_12.beans.Mets;
import org.roda_project.commons_ip2.mets_v1_12.beans.StructMapType;
import org.roda_project.commons_ip2.mets_v1_12.beans.AmdSecType;
import org.roda_project.commons_ip2.mets_v1_12.beans.DivType;
import org.roda_project.commons_ip2.mets_v1_12.beans.MdSecType;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType.MetsHdr.Agent;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType.MetsHdr.Agent.Note;
import org.roda_project.commons_ip2.mets_v1_12.beans.ObjectFactory;
import org.roda_project.commons_ip2.model.IPConstants;
import org.roda_project.commons_ip2.model.MetsWrapper;
import org.roda_project.commons_ip2.utils.METSUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.lang.Arrays;

/**
 * Creates the METS file in the logical AIP
 * 
 * @author Paul Hedberg <paul.hedberg@skane.se>
 */
public class CreateLogicalMETS {
	private static final Logger LOG = LoggerFactory.getLogger(CreateLogicalMETS.class);
	
	private static final String PROFILE = "https://earkaip.dilcis.eu/profile/E-ARK-AIP-v2-2-0.xml";
	
	public static void create(AIP aip, ModelService modelService) {
		
		boolean keepOriginalMets = RodaCoreFactory.getProperty(RodaConstants.CORE_PLUGINS_BASE_KEEP_ORIGINAL_METS, false);
		if (!keepOriginalMets) {
			return;
		}
		
		String softwareVersion = RodaCoreFactory.getProperty("core.plugins.base.keep_original_mets.software_version", "NOT DEFINED");

		try {
			StoragePath aipPath = ModelUtils.getAIPStoragePath(aip.getId());
			
			ObjectFactory of = new ObjectFactory();
			Mets mets = of.createMets();
			mets.setOBJID(aip.getId());
			mets.setLABEL("");
			mets.setPROFILE(PROFILE);
			mets.setTYPE("MIXED");
			mets.setOTHERTYPE("OTHERTYPE");
			
			MetsType.MetsHdr metsHeader = new MetsType.MetsHdr();
			metsHeader.setCREATEDATE(OriginalMETSUtils.currentDateAndTime());
			metsHeader.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
			
			Agent agent = new Agent();
			agent.setID("uuid-" + UUID.randomUUID().toString());
			agent.setROLE("CREATOR");
			agent.setTYPE("OTHER");
			agent.setOTHERTYPE("SOFTWARE");
			agent.setName("RODA");	
			Note note = new Note();
			note.setNOTETYPE("SOFTWARE VERSION");
			note.setValue(softwareVersion);
			agent.getNote().add(note);	
			metsHeader.getAgent().add(agent);
			
			metsHeader.setOAISPACKAGETYPE("AIP");
			mets.setMetsHdr(metsHeader);
			
			AmdSecType amdSec = new AmdSecType();
			amdSec.setID("uuid-" + UUID.randomUUID().toString());
			mets.getAmdSec().add(amdSec);
			
			StructMapType structMapType = new StructMapType();
	    	structMapType.setID("uuid-" + UUID.randomUUID().toString());
	    	structMapType.setLABEL("RelatedPackage");
	    	DivType dt = new DivType();
	    	dt.setID("uuid-" + UUID.randomUUID().toString());
	    	structMapType.setDiv(dt);
	    	mets.getStructMap().add(structMapType);
			
			if (aip.getParentId() != null) {
				DivType dt2 = new DivType();
	    		dt2.setID("uuid-" + UUID.randomUUID().toString());
	    		dt2.setLABEL("Parent");
	    		DivType.Mptr mptr = new DivType.Mptr();
	    		mptr.setLOCTYPE("OTHER");
	    		mptr.setOTHERLOCTYPE("UUID");
	    		mptr.setType("simple");
	    		mptr.setHref(aip.getParentId());
	    		mptr.setTitle("UUID-" + aip.getParentId());
	    		dt2.getMptr().add(mptr);
	    		structMapType.getDiv().getDiv().add(dt2);
			}
			
			StoragePath path = DefaultStoragePath.parse(aipPath, IPConstants.METS_FILE);	
			modelService.getStorage().createBinary(path, new StringContentPayload(XMLUtils.getXMLFromObject(mets)), false);
			Path metsPath = modelService.getStorage().getDirectAccess(aipPath).getPath().resolve(Path.of(IPConstants.METS_FILE));
			
		} catch (AlreadyExistsException
        		| AuthorizationDeniedException
        		| DatatypeConfigurationException 
        		| GenericException
        		| NotFoundException
        		| RequestNotValidException e) {
			
	    	LOG.error("Then create the logical METS file {} - Error: {}", aip.getId(), e);
		}
	}
}
