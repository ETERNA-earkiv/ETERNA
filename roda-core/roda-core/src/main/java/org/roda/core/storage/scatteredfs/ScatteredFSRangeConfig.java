package org.roda.core.storage.scatteredfs;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.fs.FSUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScatteredFSRangeConfig implements ScatteredFSConfig {
  private Pattern pattern;

  private Matcher matcher;

  private boolean isDirectory;

  private List<ScatteredFSRange> ranges;

  public ScatteredFSRangeConfig(String fileSystemName) throws GenericException {
    String regex = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "regex");

    String type = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "type");

    String rule = RodaCoreFactory.getRodaConfigurationAsString("core.storage.filesystem", fileSystemName,
            "rule");

    if (!regex.isEmpty()) {
      this.pattern = Pattern.compile(regex);
      this.matcher = this.pattern.matcher("");
    }

    this.isDirectory = type.equals("directory");

    String[] rangeStrings = rule.split(",");

    List<ScatteredFSRange> ranges = new ArrayList<>();

    for(String rangeString : rangeStrings) {
      int dashIndex = rangeString.indexOf("-");

      if(dashIndex == -1 || dashIndex == rangeString.length() - 1) {
        throw new GenericException("Error! Invalid rule property.");
      }

      try {
        int beginIndex = Integer.parseInt(rangeString, 0, dashIndex, 10);
        int endIndex = Integer.parseInt(rangeString, dashIndex+1, rangeString.length(), 10);

        if(endIndex <= beginIndex) {
          throw new GenericException("Error! Invalid rule property.");
        }

        ranges.add(new ScatteredFSRange(beginIndex, endIndex));
      } catch(Exception ignored) {
        throw new GenericException("Error! Invalid rule property.");
      }
    }

    if(ranges.isEmpty()) {
      throw new GenericException("Error! Invalid rule property.");
    }

    this.ranges = ranges;
  }

  public StoragePath getStoragePath(Path relativePath) throws RequestNotValidException {
    ArrayList<String> pathPartials = new ArrayList<>();

    int i;
    for (i = 0; i < relativePath.getNameCount(); i++) {
      String pathPartial = relativePath.getName(i).toString();
      pathPartials.add(FSUtils.decodePathPartial(pathPartial));
    }

    if(i > 1) {
      int numPathPartialsToRemove = Math.min(pathPartials.size(), this.ranges.size());
      pathPartials.subList(1, numPathPartialsToRemove + 1).clear();
    }

    return DefaultStoragePath.parse(pathPartials);
  }

  public Path getScatteredPath(String id) {
    Path scatteredPath = Paths.get(
            ranges.stream().map(range -> id.substring(range.beginIndex(), range.endIndex()))
                    .collect(Collectors.joining(File.separator))
    );

    scatteredPath = scatteredPath.resolve(id);

    return scatteredPath;
  }

  public boolean isValidName(Path path) {
    this.matcher.reset(path.getFileName().toString());
    return this.matcher.matches();
  }

  public boolean isDirectory() {
    return this.isDirectory;
  }

  public int getScatteredFolderDepth() {
    return this.ranges.size();
  }
}