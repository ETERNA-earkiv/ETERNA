/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.originalmets;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Path;
import javax.xml.datatype.DatatypeConfigurationException;
import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.fs.FSPathContentPayload;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip2.mets_v1_12.beans.Mets;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType;
import org.roda_project.commons_ip2.mets_v1_12.beans.ObjectFactory;
import org.roda_project.commons_ip2.model.IPConstants;
import org.roda_project.commons_ip2.model.MetsWrapper;
import org.roda_project.commons_ip2.model.SIP;
import org.roda_project.commons_ip2.utils.METSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Copies and update the original METS files from the SIP to the AIP
 *
 * @author Paul Hedberg <paul.hedberg@skane.se>
 */
public class CopyOriginalMETS {

    private static final Logger LOG = LoggerFactory.getLogger(
        CopyOriginalMETS.class
    );

    private static final String PROFILE =
        "https://earkaip.dilcis.eu/profile/E-ARK-AIP-v2-2-0.xml";

    public static void copy(String aipId, SIP sip, ModelService modelService)
        throws RequestNotValidException {
        boolean keepOriginalMets = RodaCoreFactory.getProperty(
            RodaConstants.CORE_PLUGINS_BASE_KEEP_ORIGINAL_METS,
            false
        );
        if (!keepOriginalMets) {
            return;
        }

        try {
            Path metsPath = sip
                .getBasePath()
                .resolve(Path.of(IPConstants.METS_FILE));
            Mets mets = METSUtils.instantiateMETSFromFile(metsPath);
            MetsWrapper metsWrapper = new MetsWrapper(mets, metsPath);

            ObjectFactory of = new ObjectFactory();
            MetsType.MetsHdr.AltRecordID altRecID =
                of.createMetsTypeMetsHdrAltRecordID();
            altRecID.setID(metsWrapper.getMets().getOBJID());
            altRecID.setTYPE("PREVIOUSREFERENCECODE");
            metsWrapper.getMets().getMetsHdr().getAltRecordID().add(altRecID);

            metsWrapper.getMets().setOBJID(aipId);
            metsWrapper.getMets().getMetsHdr().setOAISPACKAGETYPE("AIP");

            metsWrapper
                .getMets()
                .getMetsHdr()
                .setLASTMODDATE(OriginalMETSUtils.currentDateAndTime());

            metsWrapper.getMets().getMetsHdr().setRECORDSTATUS("Complete");

            metsWrapper.getMets().setPROFILE(PROFILE);

            METSUtils.marshallMETS(mets, metsPath, true);

            StoragePath aipPath = ModelUtils.getAIPStoragePath(aipId);
            StoragePath path = DefaultStoragePath.parse(
                aipPath,
                IPConstants.METS_FILE
            );

            ContentPayload payload = new FSPathContentPayload(metsPath);
            modelService.getStorage().createBinary(path, payload, false);

            sip
                .getRepresentations()
                .forEach(r -> {
                    try {
                        Path repMetsPath = sip
                            .getBasePath()
                            .resolve(Path.of("representations"))
                            .resolve(Path.of(r.getRepresentationID()))
                            .resolve(Path.of(IPConstants.METS_FILE));

                        Mets repMets = METSUtils.instantiateMETSFromFile(
                            repMetsPath
                        );
                        MetsWrapper repMetsWrapper = new MetsWrapper(
                            repMets,
                            repMetsPath
                        );

                        repMetsWrapper
                            .getMets()
                            .getMetsHdr()
                            .setOAISPACKAGETYPE("AIP");
                        repMetsWrapper
                            .getMets()
                            .getMetsHdr()
                            .setLASTMODDATE(
                                OriginalMETSUtils.currentDateAndTime()
                            );
                        repMetsWrapper
                            .getMets()
                            .getMetsHdr()
                            .setRECORDSTATUS("Complete");

                        repMetsWrapper.getMets().setPROFILE(PROFILE);

                        METSUtils.marshallMETS(repMets, repMetsPath, false);

                        StoragePath repPath = DefaultStoragePath.parse(
                            aipPath,
                            "representations",
                            r.getRepresentationID(),
                            IPConstants.METS_FILE
                        );
                        ContentPayload repPayload = new FSPathContentPayload(
                            repMetsPath
                        );
                        modelService
                            .getStorage()
                            .createBinary(repPath, repPayload, false);
                    } catch (
                        AlreadyExistsException
                        | AuthorizationDeniedException
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
                            "Then update and copy original AIP {} representation {} METS file - Error: {}",
                            aipId,
                            r.getRepresentationID(),
                            e
                        );
                    }
                });
        } catch (
            AlreadyExistsException
            | AuthorizationDeniedException
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
                "Then update and copy original AIP {} root METS file - Error: {}",
                aipId,
                e
            );
        }
    }
}
