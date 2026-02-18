package org.roda.wui.client.common.utils;

import java.util.Objects;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.metadata.FileFormat;

public class FileFormatSharedUtils {

  private FileFormatSharedUtils() {}

  public static boolean hasFileFormat(final IndexedFile file, final String mimeType, final String fileExtension) {
    FileFormat fileFormat = file.getFileFormat();
    String mime = fileFormat.getMimeType();
    String extension = fileFormat.getExtension();
    String fileIdExtension = getFileExtension(file.getId());

    if (Objects.equals(mime, mimeType)) {
      return true;
    } else if (mime == null && extension != null && (extension.equalsIgnoreCase("." + fileExtension) || extension.equalsIgnoreCase(fileExtension))) {
      return true;
    } else {
      return mime == null && extension == null && fileIdExtension.equalsIgnoreCase(fileExtension);
    }
  }

  public static String getFileExtension(String name) {
    String extension = "";
    if (name == null) { return extension; }

    int dotIndex = name.lastIndexOf('.');
    int unixPathSeparatorIndex = name.lastIndexOf('/');
    int windowsPathSeparatorIndex = name.lastIndexOf('\\');

    if (dotIndex > unixPathSeparatorIndex && dotIndex > windowsPathSeparatorIndex && dotIndex < name.length() - 1) {
      extension = name.substring(dotIndex + 1);
    }
    return extension;
  }
}