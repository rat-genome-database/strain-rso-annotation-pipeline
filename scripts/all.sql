/* Mark all annotations that were created by this pipeline:
Last_modified=180
*/
@echo 'Annotations marked for processing: ';
UPDATE FULL_ANNOT
SET FULL_ANNOT.LAST_MODIFIED_DATE=to_date('01/01/1900', 'MM/DD/YYYY')
WHERE FULL_ANNOT.LAST_MODIFIED_BY=180;
/* Update valid annotations with the latest terms, names, symbols and
last_modified_date
*/
@echo 'Annotations updated: ';
UPDATE FULL_ANNOT fa
SET
  (
    term,
    object_symbol,
    object_name,
    last_modified_date
  )
  =
  (SELECT ot.TERM,
    st.STRAIN_SYMBOL,
    st.FULL_NAME,
    sysdate
  FROM ONT_TERMS ot,
    strains st,
    RGD_IDS
  WHERE fa.ANNOTATED_OBJECT_RGD_ID = rgd_ids.RGD_ID
  AND rgd_ids.OBJECT_STATUS      = 'ACTIVE'
  AND fa.TERM_ACC              = ot.TERM_ACC
  AND fa.ANNOTATED_OBJECT_RGD_ID = st.RGD_ID
  )
WHERE fa.LAST_MODIFIED_BY=180
AND EXISTS (
  (SELECT ot.TERM,
    st.STRAIN_SYMBOL,
    st.FULL_NAME,
    sysdate
  FROM ONT_TERMS ot,
    strains st,
    RGD_IDS
  WHERE fa.ANNOTATED_OBJECT_RGD_ID = rgd_ids.RGD_ID
  AND rgd_ids.OBJECT_STATUS      = 'ACTIVE'
  AND fa.TERM_ACC              = ot.TERM_ACC
  AND fa.ANNOTATED_OBJECT_RGD_ID = st.RGD_ID
  )
);
/* Delete obsolete annotations which are not touched by the update annotations updates */
@echo 'Records deleted: ';
DELETE
FROM FULL_ANNOT fa
WHERE fa.LAST_MODIFIED_BY = 180
AND fa.LAST_MODIFIED_DATE = to_date('01/01/1900', 'MM/DD/YYYY');
/* Insert new annotations */
@echo 'New records inserted: ';
INSERT
INTO FULL_ANNOT
  (
    FULL_ANNOT_KEY,
    term,
    annotated_object_rgd_id,
    rgd_object_key,
    data_src,
    object_symbol,
    ref_rgd_id,
    evidence,
    aspect,
    object_name,
    created_date,
    last_modified_date,
    term_acc,
    created_by,
    last_modified_by
  )
SELECT FULL_ANNOT_SEQ.nextval,
  ot.TERM,
  st.RGD_ID,
  5,
  'RGD',
  st.STRAIN_SYMBOL,
  7241799,
  'IEA',
  'S',
  st.FULL_NAME,
  sysdate,
  sysdate,
  ot.TERM_ACC,
  180,
  180
FROM ONT_TERMS ot,
  ONT_SYNONYMS os,
  strains st,
  RGD_IDS
WHERE os.SYNONYM_NAME LIKE 'RGD ID:%'
AND to_number(SUBSTR(os.SYNONYM_NAME,9, 100)) = st.RGD_ID
AND st.rgd_id                                 = rgd_ids.rgd_id
AND ot.TERM_ACC = os.TERM_ACC
AND rgd_ids.OBJECT_STATUS                    = 'ACTIVE'
AND NOT EXISTS
  (SELECT fa.FULL_ANNOT_KEY
  FROM FULL_ANNOT fa
  WHERE fa.TERM_ACC              = ot.TERM_ACC
  AND fa.ANNOTATED_OBJECT_RGD_ID = st.rgd_id
  AND fa.created_by = 180
  );

