
# **Original-METS**

## **Functionality**
  - When a logical AIP is created, a METS file will also be created.
  - When a SIP is ingested, the AIP will retain the original METS file(s) from the SIP.
  - In the ingested METS files some attributes are updated so they comply with the DILCIS Board.
  - At each archival preservation action a PREMIS file is created, and that file is recorded in the related METS file.
  - When an AIP is ingested and moved, the nearest relationships are documented in the METS files for both parent and child.


## **System overview**

#### The original-METS has to know the main-storage-path.
![Overall system](./assets/image-2.png)

#### When a logical AIP is created then also a METS will be created.
![Overall system](./assets/image-3.png)

#### When a SIP is imported then also the METS will be imported.
![Overall system](./assets/image-4.png)

#### PREMIS processes are caught and record in the related METS.
![Overall system](./assets/image-5.png)
