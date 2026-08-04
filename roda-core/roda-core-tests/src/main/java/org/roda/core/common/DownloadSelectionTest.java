/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.roda.core.RodaCoreFactory;
import org.roda.core.TestsHelper;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.v2.StreamResponse;
import org.roda.core.data.v2.file.DownloadRefusal;
import org.roda.core.data.v2.file.DownloadRefusalReason;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.v2.index.select.SelectedItemsFilter;
import org.roda.core.data.v2.index.select.SelectedItemsList;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.user.User;
import org.roda.core.index.IndexService;
import org.roda.core.index.IndexTestUtils;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.UserUtility;
import org.roda.core.security.LdapUtilityTestHelper;
import org.roda.core.storage.StringContentPayload;
import org.roda.core.storage.fs.FSUtils;
import org.roda.core.util.IdUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Service-level tests for the selection-based download (#589): expansion,
 * validation and the structure of the resulting zip.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_TRAVIS})
public class DownloadSelectionTest {

  private static final String REPRESENTATION_ID = "rep-original";
  private static final String OTHER_REPRESENTATION_ID = "rep-derived";
  private static final String NON_ADMIN_USER = "archivist";

  private static Path basePath;
  private static ModelService model;
  private static IndexService index;
  private static LdapUtilityTestHelper ldapUtilityTestHelper;

  @BeforeClass
  public static void setUp() throws Exception {
    basePath = TestsHelper.createBaseTempDir(DownloadSelectionTest.class, true);
    ldapUtilityTestHelper = new LdapUtilityTestHelper();

    RodaCoreFactory.instantiateTest(true, true, false, false, false, false, false,
      ldapUtilityTestHelper.getLdapUtility());

    model = RodaCoreFactory.getModelService();
    index = RodaCoreFactory.getIndexService();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    IndexTestUtils.resetIndex();
    ldapUtilityTestHelper.shutdown();
    RodaCoreFactory.shutdown();
    FSUtils.deletePath(basePath);
  }

  @AfterMethod
  public void cleanUp() {
    RodaCoreFactory.getRodaConfiguration().clearProperty(RodaConstants.CORE_DOWNLOAD_MAX_FILES);
    IndexTestUtils.resetIndex();
  }

  @Test
  public void testSelectionOfBitstreamsYieldsExactlyThoseFiles() throws RODAException {
    AIP aip = createAIP(new Permissions());
    createRepresentation(aip, REPRESENTATION_ID);
    IndexedFile first = createFile(aip, REPRESENTATION_ID, List.of(), "one.pdf", "1");
    IndexedFile second = createFile(aip, REPRESENTATION_ID, List.of(), "two.pdf", "22");
    IndexedFile third = createFile(aip, REPRESENTATION_ID, List.of(), "three.pdf", "333");
    commit();

    List<IndexedFile> expanded = DownloadSelection.expand(index,
      selectionOf(first.getUUID(), second.getUUID(), third.getUUID()));

    assertEquals(uuidsOf(expanded), List.of(first.getUUID(), second.getUUID(), third.getUUID()));
  }

  @Test
  public void testSelectedFolderContributesItsContentsRecursively() throws RODAException {
    AIP aip = createAIP(new Permissions());
    createRepresentation(aip, REPRESENTATION_ID);
    IndexedFile folder = createFolder(aip, REPRESENTATION_ID, List.of(), null, "handlingar");
    createFolder(aip, REPRESENTATION_ID, List.of(), "handlingar", "2024");
    IndexedFile shallow = createFile(aip, REPRESENTATION_ID, List.of("handlingar"), "brev.pdf", "a");
    IndexedFile nested = createFile(aip, REPRESENTATION_ID, List.of("handlingar", "2024"), "bilaga.pdf", "bb");
    commit();

    List<IndexedFile> expanded = DownloadSelection.expand(index, selectionOf(folder.getUUID()));

    assertEquals(new HashSet<>(uuidsOf(expanded)), Set.of(shallow.getUUID(), nested.getUUID()),
      "the folder and its subfolder must not appear as entries, only the files beneath them");
  }

  @Test
  public void testFilterYieldsTheSameResultAsTheEquivalentExplicitList() throws RODAException {
    AIP aip = createAIP(new Permissions());
    createRepresentation(aip, REPRESENTATION_ID);
    IndexedFile first = createFile(aip, REPRESENTATION_ID, List.of(), "one.pdf", "1");
    IndexedFile second = createFile(aip, REPRESENTATION_ID, List.of(), "two.pdf", "22");
    commit();

    List<IndexedFile> fromList = DownloadSelection.expand(index,
      selectionOf(first.getUUID(), second.getUUID()));
    List<IndexedFile> fromFilter = DownloadSelection.expand(index, new SelectedItemsFilter<>(
      new Filter(new SimpleFilterParameter(RodaConstants.FILE_AIP_ID, aip.getId())), IndexedFile.class.getName(),
      Boolean.FALSE));

    assertEquals(new HashSet<>(uuidsOf(fromFilter)), new HashSet<>(uuidsOf(fromList)));
  }

  @Test
  public void testHardLimitRefusesTheSelectionWithBothTheCountAndTheLimit() throws RODAException {
    AIP aip = createAIP(new Permissions());
    createRepresentation(aip, REPRESENTATION_ID);
    IndexedFile first = createFile(aip, REPRESENTATION_ID, List.of(), "one.pdf", "1");
    IndexedFile second = createFile(aip, REPRESENTATION_ID, List.of(), "two.pdf", "22");
    commit();

    List<IndexedFile> files = DownloadSelection.expand(index, selectionOf(first.getUUID(), second.getUUID()));
    RodaCoreFactory.getRodaConfiguration().setProperty(RodaConstants.CORE_DOWNLOAD_MAX_FILES, 1);

    DownloadRefusal refusal = DownloadSelection.checkFileCount(files).orElseThrow();
    assertEquals(refusal.getReason(), DownloadRefusalReason.TOO_MANY_FILES);
    assertEquals(refusal.getFileCount(), 2, "the refusal must state how many files were selected");
    assertEquals(refusal.getFileLimit(), 1, "the refusal must state the configured limit");
  }

  @Test
  public void testLimitOfZeroRefusesNothing() throws RODAException {
    AIP aip = createAIP(new Permissions());
    createRepresentation(aip, REPRESENTATION_ID);
    IndexedFile first = createFile(aip, REPRESENTATION_ID, List.of(), "one.pdf", "1");
    IndexedFile second = createFile(aip, REPRESENTATION_ID, List.of(), "two.pdf", "22");
    commit();

    List<IndexedFile> files = DownloadSelection.expand(index, selectionOf(first.getUUID(), second.getUUID()));
    RodaCoreFactory.getRodaConfiguration().setProperty(RodaConstants.CORE_DOWNLOAD_MAX_FILES, 0);

    assertTrue(DownloadSelection.checkFileCount(files).isEmpty());
  }

  /**
   * The controller runs this check over the expanded UUIDs. What is pinned here
   * is the all-or-nothing behaviour it relies on: one unreadable file among
   * readable ones must sink the whole selection.
   */
  @Test
  public void testMissingPermissionOnASingleFileRejectsTheWholeSelection() throws RODAException {
    AIP readable = createAIP(readPermissionsFor(NON_ADMIN_USER));
    createRepresentation(readable, REPRESENTATION_ID);
    IndexedFile permittedFile = createFile(readable, REPRESENTATION_ID, List.of(), "one.pdf", "1");

    AIP unreadable = createAIP(new Permissions());
    createRepresentation(unreadable, REPRESENTATION_ID);
    IndexedFile forbiddenFile = createFile(unreadable, REPRESENTATION_ID, List.of(), "two.pdf", "22");
    commit();

    User user = new User(NON_ADMIN_USER);
    List<IndexedFile> expanded = DownloadSelection.expand(index,
      selectionOf(permittedFile.getUUID(), forbiddenFile.getUUID()));

    UserUtility.checkFilePermissions(user, selectionOf(permittedFile.getUUID()), PermissionType.READ);

    assertThrows(AuthorizationDeniedException.class, () -> UserUtility.checkFilePermissions(user,
      SelectedItemsList.create(IndexedFile.class, expanded.stream().map(IndexedFile::getUUID).toList()),
      PermissionType.READ));
  }

  @Test
  public void testUnreachableReferenceContentRefusesTheSelectionAndOnlyProbesReferences() {
    IndexedFile reference = referenceFile("http://archive.example.org/records/1.pdf");
    IndexedFile plainFile = new IndexedFile();
    plainFile.setUUID(IdUtils.createUUID());

    List<URI> probed = new ArrayList<>();
    DownloadRefusal refusal = DownloadSelection.checkDeliverability(List.of(plainFile, reference), uri -> {
      probed.add(uri);
      return false;
    }).orElseThrow();

    assertEquals(refusal.getReason(), DownloadRefusalReason.UNDELIVERABLE_CONTENT);
    assertEquals(refusal.getUndeliverableFileCount(), 1, "the refusal must state how many files are affected");
    assertEquals(refusal.getFileCount(), 2, "and out of how many");
    assertEquals(probed.size(), 1, "only the reference file may cost a probe");
  }

  @Test
  public void testAnUnresolvableProtocolCountsAsUndeliverable() {
    IndexedFile reference = referenceFile("unreachable-protocol://archive.example.org/records/1.pdf");

    assertTrue(DownloadSelection.checkDeliverability(List.of(reference)).isPresent());
  }

  @Test
  public void testAvailabilityIsCheckedOncePerProtocolAndHostAndNotOncePerFile() {
    List<IndexedFile> manyFilesSameHost = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      manyFilesSameHost.add(referenceFile("http://archive.example.org/records/" + i + ".pdf"));
    }
    manyFilesSameHost.add(referenceFile("http://archive.example.org:8080/records/1.pdf"));
    manyFilesSameHost.add(referenceFile("http://other.example.org/records/1.pdf"));

    List<URI> probed = new ArrayList<>();
    assertTrue(DownloadSelection.checkDeliverability(manyFilesSameHost, uri -> {
      probed.add(uri);
      return true;
    }).isEmpty());

    assertEquals(probed.size(), 3,
      "52 files against three distinct protocol/host/port endpoints must cost three probes");
  }

  @Test
  public void testZipContainsPathsRelativeToDataAndNoMetadata() throws RODAException, IOException {
    AIP aip = createAIP(new Permissions());
    createRepresentation(aip, REPRESENTATION_ID);
    createFolder(aip, REPRESENTATION_ID, List.of(), null, "handlingar");
    createFolder(aip, REPRESENTATION_ID, List.of(), "handlingar", "2024");
    IndexedFile nested = createFile(aip, REPRESENTATION_ID, List.of("handlingar", "2024"), "bilaga.pdf", "content");
    IndexedFile root = createFile(aip, REPRESENTATION_ID, List.of(), "top.pdf", "content");
    commit();

    List<IndexedFile> files = DownloadSelection.expand(index, selectionOf(nested.getUUID(), root.getUUID()));
    List<String> entries = zipEntryNames(DownloadSelection.createZipStreamResponse(model, index, files));

    assertEquals(new HashSet<>(entries), Set.of("handlingar/2024/bilaga.pdf", "top.pdf"));
    assertFalse(entries.stream().anyMatch(entry -> entry.contains(RodaConstants.STORAGE_DIRECTORY_METADATA)),
      "the zip must not contain any metadata entry");
  }

  @Test
  public void testASingleSelectedFileStillYieldsAZip() throws RODAException, IOException {
    AIP aip = createAIP(new Permissions());
    createRepresentation(aip, REPRESENTATION_ID);
    IndexedFile only = createFile(aip, REPRESENTATION_ID, List.of(), "one.pdf", "content");
    commit();

    List<IndexedFile> files = DownloadSelection.expand(index, selectionOf(only.getUUID()));
    StreamResponse response = DownloadSelection.createZipStreamResponse(model, index, files);

    assertEquals(zipEntryNames(response), List.of("one.pdf"));
    assertTrue(response.getStream().getFileName().endsWith(".zip"));
  }

  @Test
  public void testPathsAreNotPrefixedWithinASingleRepresentation() throws RODAException, IOException {
    AIP aip = createAIP(new Permissions());
    createRepresentation(aip, REPRESENTATION_ID);
    IndexedFile file = createFile(aip, REPRESENTATION_ID, List.of(), "same-name.pdf", "content");
    commit();

    List<IndexedFile> files = DownloadSelection.expand(index, selectionOf(file.getUUID()));

    assertEquals(zipEntryNames(DownloadSelection.createZipStreamResponse(model, index, files)),
      List.of("same-name.pdf"));
  }

  @Test
  public void testPathsArePrefixedWithTheRepresentationIdAcrossRepresentations() throws RODAException, IOException {
    AIP aip = createAIP(new Permissions());
    createRepresentation(aip, REPRESENTATION_ID);
    createRepresentation(aip, OTHER_REPRESENTATION_ID);
    IndexedFile inOriginal = createFile(aip, REPRESENTATION_ID, List.of(), "same-name.pdf", "content");
    IndexedFile inDerived = createFile(aip, OTHER_REPRESENTATION_ID, List.of(), "same-name.pdf", "content");
    commit();

    List<IndexedFile> files = DownloadSelection.expand(index,
      selectionOf(inOriginal.getUUID(), inDerived.getUUID()));
    List<String> entries = zipEntryNames(DownloadSelection.createZipStreamResponse(model, index, files));

    assertEquals(new HashSet<>(entries),
      Set.of(REPRESENTATION_ID + "/same-name.pdf", OTHER_REPRESENTATION_ID + "/same-name.pdf"),
      "identical internal paths in two representations must not collide silently");
  }

  @Test
  public void testZipNameKeepsSwedishCharactersAndDropsWhatAFileSystemWouldReject() {
    assertEquals(DownloadSelection.sanitizeFileName("Bygglov Åre/Ängelholm: 2024"), "Bygglov_Åre_Ängelholm_2024");
    assertEquals(DownloadSelection.sanitizeFileName("///"), "files");
  }

  // -- corpus helpers ------------------------------------------------------

  private AIP createAIP(Permissions permissions) throws RODAException {
    return model.createAIP(null, "", permissions, RodaConstants.ADMIN, null);
  }

  private void createRepresentation(AIP aip, String representationId) throws RODAException {
    model.createRepresentation(aip.getId(), representationId, true, "MIXED", true, RodaConstants.ADMIN);
  }

  private IndexedFile createFile(AIP aip, String representationId, List<String> directoryPath, String fileId,
    String content) throws RODAException {
    model.createFile(aip.getId(), representationId, directoryPath, fileId, new StringContentPayload(content),
      RodaConstants.ADMIN);
    return stub(aip, representationId, directoryPath, fileId);
  }

  private IndexedFile createFolder(AIP aip, String representationId, List<String> directoryPath, String parentFolderId,
    String folderName) throws RODAException {
    model.createFile(aip.getId(), representationId, directoryPath, parentFolderId, folderName, RodaConstants.ADMIN,
      true);

    List<String> folderPath = new ArrayList<>(directoryPath);
    if (parentFolderId != null) {
      folderPath.add(parentFolderId);
    }
    return stub(aip, representationId, folderPath, folderName);
  }

  /**
   * Only the UUID is used by the tests; the index is the source of truth for
   * everything else.
   */
  private IndexedFile stub(AIP aip, String representationId, List<String> directoryPath, String fileId) {
    IndexedFile file = new IndexedFile();
    file.setUUID(IdUtils.getFileId(aip.getId(), representationId, directoryPath, fileId));
    return file;
  }

  private IndexedFile referenceFile(String referenceURL) {
    IndexedFile file = new IndexedFile();
    file.setUUID(IdUtils.createUUID());
    file.setReference(true);
    file.setReferenceURL(referenceURL);
    return file;
  }

  private void commit() throws RODAException {
    index.commitAIPs();
  }

  private SelectedItems<IndexedFile> selectionOf(String... uuids) {
    return SelectedItemsList.create(IndexedFile.class, uuids);
  }

  private List<String> uuidsOf(List<IndexedFile> files) {
    return files.stream().map(IndexedFile::getUUID).toList();
  }

  private Permissions readPermissionsFor(String username) {
    Permissions permissions = new Permissions();
    permissions.setUserPermissions(username, new HashSet<>(Set.of(PermissionType.READ)));
    return permissions;
  }

  private List<String> zipEntryNames(StreamResponse response) throws IOException {
    ByteArrayOutputStream zipped = new ByteArrayOutputStream();
    response.getStream().consumeOutputStream(zipped);

    List<String> names = new ArrayList<>();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipped.toByteArray()))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        names.add(entry.getName());
      }
    }
    return names;
  }
}
