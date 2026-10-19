package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.dao.impl.AnnotationDAO;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
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

    /**
     * current database (server) time, used as the deletion cutoff.
     * It is sourced from the DB rather than the app machine so that it is comparable
     * with the LOCALTIMESTAMP that the update/insert steps stamp into last_modified_date,
     * even when the app and the database run on different machines whose clocks are
     * slightly out of sync.
     * @return database LOCALTIMESTAMP
     * @throws Exception on spring framework dao failure
     */
    public Date getDbDate() throws Exception {
        return new JdbcTemplate(annotationDAO.getDataSource())
                .queryForObject("SELECT LOCALTIMESTAMP(0)", Timestamp.class);
    }

    public int updateStrainRsoAnnotations() throws Exception {
        String sql = """
            UPDATE full_annot fa
            SET (term, object_symbol, object_name, last_modified_date)
              =
              (SELECT ot.term, st.strain_symbol, st.full_name, LOCALTIMESTAMP(0)
              FROM ont_terms ot,
                strains st,
                rgd_ids i
              WHERE
               fa.term_acc              = ot.term_acc
              AND fa.annotated_object_rgd_id = st.rgd_id
              AND fa.annotated_object_rgd_id = i.rgd_id
              AND i.object_status = 'ACTIVE'
              AND ot.is_obsolete = 0
              )
            WHERE fa.last_modified_by=?
            AND EXISTS
              (SELECT ot.term, st.strain_symbol, st.full_name, LOCALTIMESTAMP(0)
              FROM ont_terms ot,
                strains st,
                rgd_ids i
              WHERE fa.term_acc              = ot.term_acc
              AND fa.annotated_object_rgd_id = st.rgd_id
              AND fa.annotated_object_rgd_id = i.rgd_id
              AND i.object_status = 'ACTIVE'
              AND ot.is_obsolete = 0
            )
            """;
        return annotationDAO.update(sql, getCreatedBy());
    }

    /**
     * delete annotations not refreshed by the update step, i.e. last modified before the
     * given DB cutoff time; also log all deleted annotations into deleted.log
     * @param cutoff DB-sourced timestamp captured before the update step (see getDbDate)
     * @return count of obsolete annotations deleted
     */
    public int deleteStrainRsoAnnotations(Date cutoff) throws Exception {

        List<Annotation> obsoleteAnnots = annotationDAO.getAnnotationsModifiedBeforeTimestamp(getCreatedBy(), cutoff, getRefRgdId());
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
            INSERT INTO full_annot (
                full_annot_key, term, annotated_object_rgd_id, rgd_object_key, data_src,
                object_symbol, ref_rgd_id, evidence, aspect, object_name,
                created_date, last_modified_date, term_acc, created_by, last_modified_by
              )
            SELECT nextval('full_annot_seq'), ot.term, st.rgd_id, 5, 'RGD',
              st.strain_symbol, ?, 'IEA', 'S', st.full_name,
              LOCALTIMESTAMP(0), LOCALTIMESTAMP(0), ot.term_acc, ?, ?
            FROM ont_terms ot,
              ont_synonyms os,
              strains st,
              rgd_ids i
            WHERE os.synonym_name = 'RGD ID: ' || st.rgd_id -- synonyms of RS terms are stored as 'RGD ID: 12345'
              AND st.rgd_id = i.rgd_id
              AND ot.term_acc = os.term_acc
              AND i.object_status = 'ACTIVE'
              AND ot.is_obsolete = 0
              AND NOT EXISTS
              (SELECT fa.full_annot_key FROM full_annot fa
               WHERE fa.term_acc = ot.term_acc
                 AND fa.annotated_object_rgd_id = st.rgd_id
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
