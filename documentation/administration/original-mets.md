
# **Original-METS**

## **Functionality**

  - When a logical AIP is created, a METS file will also be created.
  - When a SIP is ingested, the AIP will retain the original METS file(s) from the SIP.
  - In the ingested METS files some attributes are updated so they comply with the DILCIS Board.
  - At each archival preservation action a PREMIS file is created, and that file is recorded in the related METS file.
  - When an AIP is ingested and moved, the nearest relationships are documented in the METS files for both parent and child.


## **System overview**

### The original-METS has to know the main-storage-path.

![The original-METS has to know the main-storage-path](../images/original-mets_storage.png "The original-METS has to know the main-storage-path")

### When a logical AIP is created then also a METS will be created.

![When a logical AIP is created then also a METS will be created.
](../images/original-mets_create_logical.png "When a logical AIP is created then also a METS will be created")

### When a SIP is imported then also the METS will be imported.

![When a SIP is imported then also the METS will be imported](../images/original-mets_copy.png "When a SIP is imported then also the METS will be imported")

### PREMIS processes are caught and record in the related METS.

![PREMIS processes are caught and record in the related METS](../images/original-mets_update.png "PREMIS processes are caught and record in the related METS")

## **Activate**

To use the original-METS function, add the property

```javaproperties
core.plugins.base.keep_original_mets = true
```

and indicates ETERNA's version 

```javaproperties
core.plugins.base.keep_original_mets.software_version = xxxx
```

in the ETERNA'S configuration file, `roda-core.properties`
