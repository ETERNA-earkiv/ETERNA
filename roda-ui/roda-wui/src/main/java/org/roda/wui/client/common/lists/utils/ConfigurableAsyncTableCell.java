/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.common.lists.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.IsIndexed;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.DIP;
import org.roda.core.data.v2.ip.HasDisposal;
import org.roda.core.data.v2.ip.HasPermissions;
import org.roda.core.data.v2.ip.HasState;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.IndexedDIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.data.v2.ip.metadata.FileFormat;
import org.roda.wui.client.common.lists.utils.ColumnOptions.RenderingHint;
import org.roda.wui.common.client.tools.DescriptionLevelUtils;
import org.roda.wui.common.client.tools.Humanize;
import org.roda.wui.common.client.tools.StringUtils;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.index.facet.Facets;
import org.roda.wui.client.browse.BrowserService;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.i18n.client.LocaleInfo;


import config.i18n.client.ClientMessages;

public class ConfigurableAsyncTableCell<T extends IsIndexed> extends AsyncTableCell<T> {

  private static final String DEFAULT_INDEXED_AIP_PATH = "default_IndexedAIP_path";
  private static final Map<String, Column<?, ?>> DEFAULT_COLUMNS = new HashMap<>();
  private static final Map<String, List<String>> DEFAULT_COLUMNS_FIELDS = new HashMap<>();

  static {
    /********************************************
     * AIP
     ********************************************/

    DEFAULT_COLUMNS.put("default_IndexedAIP_level", new Column<IndexedAIP, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(IndexedAIP aip) {
        SafeHtml ret;
        if (aip == null) {
          ret = null;
        } else {
          ret = DescriptionLevelUtils.getElementLevelIconSafeHtml(aip.getLevel(), true);
        }
        return ret;
      }
    });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedAIP_level", Arrays.asList(RodaConstants.AIP_LEVEL));

    DEFAULT_COLUMNS.put("default_IndexedAIP_dates", new TextColumn<IndexedAIP>() {
      @Override
      public String getValue(IndexedAIP aip) {
        return Humanize.getDatesText(aip.getDateInitial(), aip.getDateFinal(), false);
      }
    });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedAIP_dates",
      Arrays.asList(RodaConstants.AIP_DATE_INITIAL, RodaConstants.AIP_DATE_FINAL));

    DEFAULT_COLUMNS.put("default_IndexedAIP_hasrepresentations", new Column<IndexedAIP, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(IndexedAIP aip) {
        SafeHtml ret;
        if (aip == null) {
          ret = null;
        } else if (aip.getHasRepresentations()) {
          ret = messages.defaultColumnHeader("default_IndexedAIP_hasrepresentations");
        } else {
          ret = null;
        }
        return ret;
      }
    });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedAIP_hasrepresentations",
      Arrays.asList(RodaConstants.AIP_HAS_REPRESENTATIONS));

    DEFAULT_COLUMNS_FIELDS.put(DEFAULT_INDEXED_AIP_PATH, Arrays.asList(RodaConstants.AIP_ANCESTORS, RodaConstants.AIP_TITLE));

    /********************************************
     * Representations
     ********************************************/
    DEFAULT_COLUMNS.put("default_IndexedRepresentation_type",
      new Column<IndexedRepresentation, SafeHtml>(new SafeHtmlCell()) {
        @Override
        public SafeHtml getValue(IndexedRepresentation rep) {
          SafeHtml ret;
          if (rep == null) {
            ret = null;
          } else {
            ret = DescriptionLevelUtils.getRepresentationTypeIcon(rep.getType(), true);
          }
          return ret;
        }
      });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedRepresentation_type", Arrays.asList(RodaConstants.REPRESENTATION_TYPE));

    DEFAULT_COLUMNS.put("default_IndexedRepresentation_states", new TextColumn<IndexedRepresentation>() {
      @Override
      public String getValue(IndexedRepresentation rep) {
        List<String> translatedStates = new ArrayList<>();
        for (String state : rep.getRepresentationStates()) {
          translatedStates.add(messages.statusLabel(state));
        }
        return StringUtils.prettyPrint(translatedStates);
      }
    });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedRepresentation_states",
      Arrays.asList(RodaConstants.REPRESENTATION_STATES));

    DEFAULT_COLUMNS.put("default_IndexedRepresentation_numberOfDataFiles", new TextColumn<IndexedRepresentation>() {
      @Override
      public String getValue(IndexedRepresentation rep) {
        return rep != null ? messages.numberOfFiles(rep.getNumberOfDataFiles(), rep.getNumberOfDataFolders()) : null;
      }
    });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedRepresentation_numberOfDataFiles", Arrays
      .asList(RodaConstants.REPRESENTATION_NUMBER_OF_DATA_FOLDERS, RodaConstants.REPRESENTATION_NUMBER_OF_DATA_FILES));

    /********************************************
     * Files
     ********************************************/
    DEFAULT_COLUMNS.put("default_IndexedFile_icon", new Column<IndexedFile, SafeHtml>(new SafeHtmlCell()) {

      @Override
      public SafeHtml getValue(IndexedFile file) {
        if (file != null) {
          if (file.isDirectory()) {
            return SafeHtmlUtils.fromSafeConstant("<i class='fa fa-folder-open'></i>");
          } else {
            if (file.isReference()) {
              return SafeHtmlUtils.fromSafeConstant("<i class='fa fa-file-export'></i>");
            } else {
              return SafeHtmlUtils.fromSafeConstant("<i class='fa fa-file-o'></i>");
            }

          }
        }
        return null;
      }
    });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedFile_icon", Arrays.asList(RodaConstants.FILE_ISDIRECTORY));

    DEFAULT_COLUMNS.put("default_IndexedFile_fullpath", new Column<IndexedFile, SafeHtml>(new SafeHtmlCell()) {

      @Override
      public SafeHtml getValue(IndexedFile file) {
        SafeHtmlBuilder b = new SafeHtmlBuilder();
        if (file != null) {
          List<String> filePath = file.getPath();
          String fileName = file.getOriginalName() != null ? file.getOriginalName() : file.getId();
          List<String> fullpath = new ArrayList<>(filePath);
          fullpath.add(fileName);
          b.append(SafeHtmlUtils.fromSafeConstant("<div title='"));
          b.append(SafeHtmlUtils.fromString(StringUtils.join(fullpath, "/")));
          b.append(SafeHtmlUtils.fromSafeConstant("'>"));
          if (filePath != null && !filePath.isEmpty()) {
            String path = StringUtils.join(filePath, "/");
            b.append(SafeHtmlUtils.fromSafeConstant("<span class='table-file-path'>"));
            b.append(SafeHtmlUtils.fromString(path));
            b.append(SafeHtmlUtils.fromSafeConstant("/"));
            b.append(SafeHtmlUtils.fromSafeConstant("</span>"));
          }

          b.append(SafeHtmlUtils.fromString(fileName));
          b.append(SafeHtmlUtils.fromSafeConstant("</div>"));
        }

        return b.toSafeHtml();
      }
    });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedFile_fullpath",
      Arrays.asList(RodaConstants.FILE_PATH, RodaConstants.FILE_ORIGINALNAME));

    DEFAULT_COLUMNS.put("default_IndexedFile_shortpath", new Column<IndexedFile, SafeHtml>(new SafeHtmlCell()) {

      @Override
      public SafeHtml getValue(IndexedFile file) {
        SafeHtmlBuilder b = new SafeHtmlBuilder();
        if (file != null) {
          List<String> filePath = file.getPath();
          String fileName = file.getOriginalName() != null ? file.getOriginalName() : file.getId();
          List<String> fullpath = new ArrayList<>(filePath);
          fullpath.add(fileName);
          b.append(SafeHtmlUtils.fromSafeConstant("<div title='"));
          b.append(SafeHtmlUtils.fromString(StringUtils.join(fullpath, "/")));
          b.append(SafeHtmlUtils.fromSafeConstant("'>"));
          b.append(SafeHtmlUtils.fromString(fileName));
          b.append(SafeHtmlUtils.fromSafeConstant("</div>"));
        }

        return b.toSafeHtml();
      }
    });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedFile_shortpath",
      Arrays.asList(RodaConstants.FILE_PATH, RodaConstants.FILE_ORIGINALNAME));

    DEFAULT_COLUMNS.put("default_IndexedFile_format", new TextColumn<IndexedFile>() {

      @Override
      public String getValue(IndexedFile file) {
        if (file != null && file.getFileFormat() != null) {
          FileFormat format = file.getFileFormat();
          String ret;
          if (StringUtils.isNotBlank(format.getFormatDesignationName())) {
            ret = format.getFormatDesignationName();
            if (StringUtils.isNotBlank(format.getFormatDesignationVersion())) {
              ret = ret + " " + format.getFormatDesignationVersion();
            }
          } else if (StringUtils.isNotBlank(format.getPronom())) {
            ret = format.getPronom();
          } else if (StringUtils.isNotBlank(format.getMimeType())) {
            ret = format.getMimeType();
          } else {
            ret = null;
          }
          return ret;

        } else {
          return null;
        }
      }
    });
    DEFAULT_COLUMNS_FIELDS.put("default_IndexedFile_format", Arrays.asList(RodaConstants.FILE_FILEFORMAT,
      RodaConstants.FILE_FORMAT_VERSION, RodaConstants.FILE_FORMAT_MIMETYPE, RodaConstants.FILE_PRONOM));
  }

  // XXX Due to the lack of reflection capabilities in GWT
  // A manual list must be maintained to keep the classes that follow
  // The HasPermissions and HasState interfaces

  private static final List<Class<? extends HasPermissions>> HAS_PERMISSIONS = Arrays.asList(AIP.class,
    IndexedAIP.class, DIP.class, IndexedDIP.class);

  private static final List<Class<? extends HasState>> HAS_STATE = Arrays.asList(AIP.class, IndexedAIP.class);

  private static final List<Class<? extends HasDisposal>> HAS_DISPOSAL = Arrays.asList(AIP.class, IndexedAIP.class);

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private final Map<Column<T, ?>, List<String>> columnSortingKeyMap = new HashMap<>();
  private long aipPathLookupGeneration = 0L;

  private static <T extends IsIndexed> List<String> calculateFieldsToReturn(AsyncTableCellOptions<T> options) {
    List<String> fieldsToReturn = new ArrayList<>();

    // add UUID
    fieldsToReturn.add(RodaConstants.INDEX_UUID);
    fieldsToReturn.add(RodaConstants.INDEX_ID);

    options.getColumnOptions().forEach(c -> {
      if (c.getName().startsWith("default_")) {
        List<String> list = DEFAULT_COLUMNS_FIELDS.get(c.getName());
        if (list != null) {
          fieldsToReturn.addAll(list);
        }
      } else if (StringUtils.isNotBlank(c.getField())) {
        fieldsToReturn.add(c.getField());
      }
    });

    // if index object has permissions, add permissions fields
    if (HAS_PERMISSIONS.contains(options.getClassToReturn())) {
      fieldsToReturn.addAll(RodaConstants.AIP_PERMISSIONS_FIELDS_TO_RETURN);
    }

    // if index object has state, add state field
    if (HAS_STATE.contains(options.getClassToReturn())) {
      fieldsToReturn.add(RodaConstants.AIP_STATE);
    }

    // if index object has disposal hold status, add status field
    if (HAS_DISPOSAL.contains(options.getClassToReturn())) {
      fieldsToReturn.add(RodaConstants.AIP_DISPOSAL_HOLD_STATUS);
      fieldsToReturn.add(RodaConstants.AIP_DISPOSAL_SCHEDULE_ID);
    }

    // nested collections
    if (IndexedRepresentation.class.equals(options.getClassToReturn())) {
      fieldsToReturn.add(RodaConstants.REPRESENTATION_AIP_ID);
    } else if (IndexedFile.class.equals(options.getClassToReturn())) {
      fieldsToReturn.add(RodaConstants.FILE_AIP_ID);
      fieldsToReturn.add(RodaConstants.FILE_REPRESENTATION_ID);
      fieldsToReturn.add(RodaConstants.FILE_ISREFERENCE);
      fieldsToReturn.add(RodaConstants.FILE_CREATED_ON);
    }

    return fieldsToReturn;
  }

  @Override
  protected void adjustOptions(AsyncTableCellOptions<T> options) {
    options.withFieldsToReturn(calculateFieldsToReturn(options));
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configureDisplay(CellTable<T> display) {

    List<ColumnOptions> columns = getOptions().getColumnOptions();
    AipPathColumn pathColumn = null;
    columnSortingKeyMap.clear();

    for (final ColumnOptions c : columns) {
      String name = c.getName();
      String header = c.getHeader();

      Column<T, ?> column;
      SafeHtml htmlHeader;
      List<String> sortBy = c.getSortBy();

      if (name.startsWith("default_")) {
        // is a default column
        if (DEFAULT_INDEXED_AIP_PATH.equals(name)) {
          pathColumn = new AipPathColumn();
          column = (Column<T, ?>) pathColumn;
        } else {
          column = (Column<T, ?>) DEFAULT_COLUMNS.get(name);
        }
        htmlHeader = header.equals(name) ? messages.defaultColumnHeader(name) : SafeHtmlUtils.fromString(header);
        sortBy = c.getSortBy().equals(Collections.singletonList(name)) ? DEFAULT_COLUMNS_FIELDS.get(name) : sortBy;
      } else {
        column = new TextColumn<T>() {
          @Override
          public String getValue(T object) {
            String ret = null;

            if (object != null) {
              Object value = object.getFields().get(c.getField());

              if (value != null) {
                ret = renderValue(value, c.getRenderingHint());
              }
            }

            return ret;
          }

          private String renderValue(Object value, RenderingHint hint) {
            String ret;
            if (value instanceof Date) {
              switch (hint) {
                case DATE_FORMAT_TITLE:
                  ret = Humanize.formatDate((Date) value, true);
                  break;
                case DATE_FORMAT_SIMPLE:
                  ret = Humanize.formatDate((Date) value, false);
                  break;
                case DATETIME_FORMAT_SIMPLE:
                  ret = Humanize.formatDateTime((Date) value);
                  break;
                default:
                  ret = Humanize.formatDate((Date) value);
                  break;
              }
            } else if (value instanceof List) {
              List<String> renderedList = ((List<?>) value).stream().map(v -> renderValue(v, hint))
                .collect(Collectors.toList());
              ret = StringUtils.prettyPrint(renderedList);
            } else if (value instanceof Long && RenderingHint.FILE_SIZE.equals(hint)) {
              ret = ((Long) value > 0) ? Humanize.readableFileSize((Long) value) : "";
            } else {
              ret = value.toString();
            }
            return ret;
          }
        };

        htmlHeader = SafeHtmlUtils.fromString(header);
      }
      column.setDataStoreName(name);

      // set column sortable
      column.setSortable(c.isSortable());
      columnSortingKeyMap.put(column, sortBy);

      addColumn(column, htmlHeader, c.isNowrap(), c.isAlignRight());

      // define column sorted by default (if any)
      if (name.equals(getOptions().getDefaultSortListColumnName())) {
        display.getColumnSortList().push(new ColumnSortInfo(column, getOptions().isDefaultSortListAscending()));
      }

      // set fixed size (if defined)
      if (c.getWidth() > 0) {
        display.setColumnWidth(column, c.getWidth(), c.getWidthUnit());
      }

    }

    addStyleName("my-collections-table");
    final AipPathColumn finalPathColumn = pathColumn;
    if (finalPathColumn != null) {
      addValueChangeHandler(event -> {
        final long lookupGeneration = ++aipPathLookupGeneration;
        IndexResult<T> result = event.getValue();
        if (result == null || result.getResults().isEmpty()) {
          return;
        }

        // Collect all unique ancestor IDs across visible rows
        Set<String> ancestorIds = new HashSet<>();
        for (T item : result.getResults()) {
          if (item instanceof IndexedAIP) {
            List<String> anc = ((IndexedAIP) item).getAncestors();
            if (anc != null) {
              ancestorIds.addAll(anc);
            }
          }
        }

        if (ancestorIds.isEmpty()) {
          return;
        }

        Filter batchFilter = new Filter(new OneOfManyFilterParameter(RodaConstants.INDEX_UUID, new ArrayList<>(ancestorIds)));
        BrowserService.Util.getInstance().find(IndexedAIP.class.getName(), batchFilter, Sorter.NONE,
            new Sublist(0, ancestorIds.size()), Facets.NONE, LocaleInfo.getCurrentLocale().getLocaleName(),
            false, Arrays.asList(RodaConstants.INDEX_UUID, RodaConstants.AIP_TITLE),
            new AsyncCallback<IndexResult<IsIndexed>>() {
              @Override
              public void onFailure(Throwable caught) {
                if (lookupGeneration != aipPathLookupGeneration) {
                  return;
                }
              }

              @Override
              public void onSuccess(IndexResult<IsIndexed> res) {
                if (lookupGeneration != aipPathLookupGeneration) {
                  return;
                }
                Map<String, String> titles = new HashMap<>();
                for (IsIndexed r : res.getResults()) {
                  IndexedAIP a = (IndexedAIP) r;
                  String t = a.getTitle();
                  titles.put(a.getUUID(), (t != null && !t.trim().isEmpty()) ? t : a.getUUID());
                }
                finalPathColumn.setAncestorTitles(titles);
                redraw();
              }
            });
      });
    }
  }

  private static class AipPathColumn extends TextColumn<IndexedAIP> {
    private Map<String, String> ancestorTitles = new HashMap<>();

    @Override
    public String getValue(IndexedAIP aip) {
      if (aip == null) {
        return null;
      }
      String rootLabel = messages.aipPathRootLabel();
      List<String> ancestors = aip.getAncestors(); // ordered: closest parent first
      String selfTitle = titleOrId(aip.getTitle(), aip.getId());
      if (ancestors == null || ancestors.isEmpty()) {
        return rootLabel + " / " + selfTitle;
      }

      List<String> parts = new ArrayList<>(ancestors.size() + 2);
      parts.add(rootLabel);
      // Show full path from top ancestor down to immediate parent.
      for (int i = ancestors.size() - 1; i >= 0; i--) {
        String id = ancestors.get(i);
        parts.add(ancestorTitles.getOrDefault(id, id));
      }
      parts.add(selfTitle);
      return StringUtils.join(parts, " / ");
    }

    private String titleOrId(String title, String id) {
      return (title != null && !title.trim().isEmpty()) ? title : id;
    }

    public void setAncestorTitles(Map<String, String> titles) {
      this.ancestorTitles = titles;
    }
  }

  @Override
  protected Sorter getSorter(ColumnSortList columnSortList) {
    return createSorter(columnSortList, columnSortingKeyMap);
  }

}
