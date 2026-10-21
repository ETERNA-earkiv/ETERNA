package org.roda.core.plugins.base.originalmets;

import jakarta.xml.bind.JAXBException;

import java.io.IOException;

import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.utils.XMLUtils;
import org.roda.core.data.v2.ip.Representation;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.StringContentPayload;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip2.mets_v1_12.beans.AmdSecType;
import org.roda_project.commons_ip2.mets_v1_12.beans.DivType;
import org.roda_project.commons_ip2.mets_v1_12.beans.MdSecType;
import org.roda_project.commons_ip2.mets_v1_12.beans.Mets;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType;
import org.roda_project.commons_ip2.mets_v1_12.beans.ObjectFactory;
import org.roda_project.commons_ip2.mets_v1_12.beans.StructMapType;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType.FileSec;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType.FileSec.FileGrp;
import org.roda_project.commons_ip2.model.IPConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.SAXException;

/**
 * Creates the METS file in the representation
 *
 * @author Paul Hedberg <paul.hedberg@skane.se>
 */
public class CreateRepresentationMETS {
	private static final Logger LOG = LoggerFactory.getLogger(CreateRepresentationMETS.class);

	private static final String PROFILE = "https://earkaip.dilcis.eu/profile/E-ARK-AIP-v2-2-0.xml";

	public static void create(Representation representation, ModelService modelService) {

		boolean keepOriginalMets = RodaCoreFactory.getProperty(RodaConstants.CORE_PLUGINS_BASE_KEEP_ORIGINAL_METS, false);
		if (!keepOriginalMets) {
			return;
		}

		try {
			StoragePath aipPath = ModelUtils.getAIPStoragePath(representation.getAipId());

			String[] s = {IPConstants.REPRESENTATIONS, representation.getId(), IPConstants.METS_FILE};
			StoragePath path = DefaultStoragePath.parse(aipPath, s);

			try {
			    // Bad test but necessary
				modelService.getStorage().getBinary(path);
				return;

			} catch (NotFoundException e) {
			    // All find, create representation METS
			}

			ObjectFactory of = new ObjectFactory();
			Mets mets = of.createMets();
			mets.setOBJID(representation.getId());
			mets.setLABEL("");
			mets.setPROFILE(PROFILE);
			mets.setTYPE(representation.getType());
			mets.setCONTENTINFORMATIONTYPE(representation.getType());

			MetsType.MetsHdr metsHeader = new MetsType.MetsHdr();
			metsHeader.setCREATEDATE(OriginalMETSUtils.currentDateAndTime());
			metsHeader.setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());
			metsHeader.setRECORDSTATUS("COMPLETE");
			metsHeader.setOAISPACKAGETYPE("AIP");
			mets.setMetsHdr(metsHeader);

			MdSecType mdSecType = of.createMdSecType();
			mdSecType.setID("uuid-" + UUID.randomUUID().toString());
			mets.getDmdSec().add(mdSecType);

			AmdSecType amdSecType = of.createAmdSecType();
			amdSecType.setID("uuid-" + UUID.randomUUID().toString());
			mets.getAmdSec().add(amdSecType);

			FileGrp fileGrp = of.createMetsTypeFileSecFileGrp();
			fileGrp.setID("uuid-" + UUID.randomUUID().toString());
			FileSec fileSec = of.createMetsTypeFileSec();
			fileSec.setID("uuid-" + UUID.randomUUID().toString());
			fileSec.getFileGrp().add(fileGrp);
			mets.setFileSec(fileSec);

			StructMapType structMapType = of.createStructMapType();
			structMapType.setID("uuid-" + UUID.randomUUID().toString());
			DivType dt = new DivType();
            dt.setID("uuid-" + UUID.randomUUID().toString());
            structMapType.setDiv(dt);
			mets.getStructMap().add(structMapType);

			modelService.getStorage().createBinary(path, new StringContentPayload(XMLUtils.getXMLFromObject(mets)), false);
			
			UpdateOriginalMETS.addRepresentationToMETS(representation, modelService);

		} catch (AlreadyExistsException
        		| AuthorizationDeniedException
        		| DatatypeConfigurationException
        		| IOException
        		| IPException
        		| JAXBException
        		| GenericException
                | NotFoundException
        		| RequestNotValidException
        		| SAXException e) {

	    	LOG.error("Then create the representation {} METS file {} - Error: {}", representation.getId(), representation.getAipId(), e);
		}
	}
}
