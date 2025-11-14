package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.dao.impl.AnnotationDAO;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mtutaj
 * @since 5/30/2017
 * All database code lands here
 */
public class Dao {

    private AnnotationDAO annotationDAO = new AnnotationDAO();

    private int createdBy;
    private int refRgdId;

    private Logger logDeleted = LogManager.getLogger("deleted");

    public String getConnectionInfo() {
        return annotationDAO.getConnectionInfo();
    }

    public int updateStrainRsoAnnotations() throws Exception {
        String sql = """
            UPDATE FULL_ANNOT fa
            SET (term, object_symbol, object_name, last_modified_date)
              =
              (SELECT ot.TERM, st.STRAIN_SYMBOL, st.FULL_NAME, sysdate
              FROM ONT_TERMS ot,
                strains st,
                rgd_ids i
              WHERE
               fa.TERM_ACC              = ot.TERM_ACC
              AND fa.ANNOTATED_OBJECT_RGD_ID = st.RGD_ID
              AND fa.ANNOTATED_OBJECT_RGD_ID = i.RGD_ID
              AND i.object_status = 'ACTIVE'
              AND ot.is_obsolete = 0
              )
            WHERE fa.LAST_MODIFIED_BY=?
            AND EXISTS
              (SELECT ot.TERM, st.STRAIN_SYMBOL, st.FULL_NAME, sysdate
              FROM ONT_TERMS ot,
                strains st,
                rgd_ids i
              WHERE fa.TERM_ACC              = ot.TERM_ACC
              AND fa.ANNOTATED_OBJECT_RGD_ID = st.RGD_ID
              AND fa.ANNOTATED_OBJECT_RGD_ID = i.RGD_ID
              AND i.object_status = 'ACTIVE'
              AND ot.is_obsolete = 0
            )
            """;
        return annotationDAO.update(sql, getCreatedBy());
    }

    /**
     * delete annotations that are older than 1 hour; also log all deleted annotations into deleted.log
     * @return count of obsolete annotations deleted
     */
    public int deleteStrainRsoAnnotations() throws Exception {

        Date cutoffDate = Utils.addHoursToDate(null, -1);

        List<Annotation> obsoleteAnnots = annotationDAO.getAnnotationsModifiedBeforeTimestamp(getCreatedBy(), cutoffDate, getRefRgdId());
        List<Integer> fullAnnotKeys = new ArrayList<>(obsoleteAnnots.size());
        for( Annotation a: obsoleteAnnots ) {
            fullAnnotKeys.add(a.getKey());
            logDeleted.debug(a.dump("|"));
        }
        annotationDAO.deleteAnnotations(fullAnnotKeys);

        return obsoleteAnnots.size();
    }

    public int insertStrainRsoAnnotations() throws Exception {
        String sql = """
            INSERT INTO FULL_ANNOT (
                full_annot_key, term, annotated_object_rgd_id, rgd_object_key, data_src,
                object_symbol, ref_rgd_id, evidence, aspect, object_name,
                created_date, last_modified_date, term_acc, created_by, last_modified_by
              )
            SELECT FULL_ANNOT_SEQ.nextval, ot.TERM, st.RGD_ID, 5, 'RGD',
              st.STRAIN_SYMBOL, ?, 'IEA', 'S', st.FULL_NAME,
              sysdate, sysdate, ot.TERM_ACC, ?, ?
            FROM ONT_TERMS ot,
              ONT_SYNONYMS os,
              strains st,
              RGD_IDS i
            WHERE os.SYNONYM_NAME LIKE 'RGD ID:%'
              AND to_number(SUBSTR(os.SYNONYM_NAME,9, 100)) = st.RGD_ID
              AND st.rgd_id = i.rgd_id
              AND ot.TERM_ACC = os.TERM_ACC
              AND i.OBJECT_STATUS = 'ACTIVE'
              AND ot.is_obsolete = 0
              AND NOT EXISTS
              (SELECT fa.FULL_ANNOT_KEY FROM FULL_ANNOT fa
               WHERE fa.TERM_ACC = ot.TERM_ACC
                 AND fa.ANNOTATED_OBJECT_RGD_ID = st.rgd_id
                AND fa.created_by = ?
               )
            """;
        return annotationDAO.update(sql, getRefRgdId(), getCreatedBy(), getCreatedBy(), getCreatedBy());
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setRefRgdId(int refRgdId) {
        this.refRgdId = refRgdId;
    }

    public int getRefRgdId() {
        return refRgdId;
    }
}
