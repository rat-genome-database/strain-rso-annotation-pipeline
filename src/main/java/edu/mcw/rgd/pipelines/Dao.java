package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.dao.impl.AnnotationDAO;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

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

    private Logger logDeleted = Logger.getLogger("deleted");

    public String getConnectionInfo() {
        return annotationDAO.getConnectionInfo();
    }

    public int updateStrainRsoAnnotations() throws Exception {
        String sql = "UPDATE FULL_ANNOT fa\n" +
                "SET (\n" +
                "    term," +
                "    object_symbol," +
                "    object_name," +
                "    last_modified_date" +
                "  )\n" +
                "  =\n" +
                "  (SELECT ot.TERM,\n" +
                "    st.STRAIN_SYMBOL,\n" +
                "    st.FULL_NAME,\n" +
                "    sysdate\n" +
                "  FROM ONT_TERMS ot,\n" +
                "    strains st,\n" +
                "    rgd_ids i\n" +
                "  WHERE \n" +
                "   fa.TERM_ACC              = ot.TERM_ACC\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = st.RGD_ID\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = i.RGD_ID\n" +
                "  AND i.object_status = 'ACTIVE'\n" +
                "  AND ot.is_obsolete = 0\n" +
                "  )\n" +
                "WHERE fa.LAST_MODIFIED_BY=?\n" +
                "AND EXISTS\n" +
                "  (SELECT ot.TERM,\n" +
                "    st.STRAIN_SYMBOL,\n" +
                "    st.FULL_NAME,\n" +
                "    sysdate\n" +
                "  FROM ONT_TERMS ot,\n" +
                "    strains st,\n" +
                "    rgd_ids i\n" +
                "  WHERE fa.TERM_ACC              = ot.TERM_ACC\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = st.RGD_ID\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = i.RGD_ID\n" +
                "  AND i.object_status = 'ACTIVE'\n" +
                "  AND ot.is_obsolete = 0\n" +
                ")";
        return annotationDAO.update(sql, getCreatedBy());
    }

    /**
     * delete annotations that are older than 1 hour; also log all deleted annotations into deleted.log
     * @return count of obsolete annotations deleted
     */
    public int deleteStrainRsoAnnotations() throws Exception {

        Date cutoffDate = Utils.addHoursToDate(null, -1);

        List<Annotation> obsoleteAnnots = annotationDAO.getAnnotationsModifiedBeforeTimestamp(getCreatedBy(), cutoffDate, getRefRgdId());
        List<Integer> fullAnnotKeys = new ArrayList<Integer>(obsoleteAnnots.size());
        for( Annotation a: obsoleteAnnots ) {
            fullAnnotKeys.add(a.getKey());
            logDeleted.info(a.dump("|"));
        }
        annotationDAO.deleteAnnotations(fullAnnotKeys);

        return obsoleteAnnots.size();
    }

    public int insertStrainRsoAnnotations() throws Exception {
        String sql = "INSERT INTO FULL_ANNOT (\n" +
            "    full_annot_key,\n" +
            "    term,\n" +
            "    annotated_object_rgd_id,\n" +
            "    rgd_object_key,\n" +
            "    data_src,\n" +
            "    object_symbol,\n" +
            "    ref_rgd_id,\n" +
            "    evidence,\n" +
            "    aspect,\n" +
            "    object_name,\n" +
            "    created_date,\n" +
            "    last_modified_date,\n" +
            "    term_acc,\n" +
            "    created_by,\n" +
            "    last_modified_by\n" +
            "  )\n" +
            "SELECT FULL_ANNOT_SEQ.nextval,\n" +
            "  ot.TERM,\n" +
            "  st.RGD_ID,\n" +
            "  5,\n" +
            "  'RGD',\n" +
            "  st.STRAIN_SYMBOL,\n" +
            "  ?,\n" +
            "  'IEA',\n" +
            "  'S',\n" +
            "  st.FULL_NAME,\n" +
            "  sysdate,\n" +
            "  sysdate,\n" +
            "  ot.TERM_ACC,\n" +
            "  ?,\n" +
            "  ?\n" +
            "FROM ONT_TERMS ot,\n" +
            "  ONT_SYNONYMS os,\n" +
            "  strains st,\n" +
            "  RGD_IDS i\n" +
            "WHERE os.SYNONYM_NAME LIKE 'RGD ID:%'\n" +
            "AND to_number(SUBSTR(os.SYNONYM_NAME,9, 100)) = st.RGD_ID\n" +
            "AND st.rgd_id = i.rgd_id\n" +
            "AND ot.TERM_ACC = os.TERM_ACC\n" +
            "AND i.OBJECT_STATUS = 'ACTIVE'\n" +
            "  AND ot.is_obsolete = 0\n" +
            "AND NOT EXISTS\n" +
            "  (SELECT fa.FULL_ANNOT_KEY\n" +
            "  FROM FULL_ANNOT fa\n" +
            "  WHERE fa.TERM_ACC = ot.TERM_ACC\n" +
            "  AND fa.ANNOTATED_OBJECT_RGD_ID = st.rgd_id\n" +
            "  AND fa.created_by = ?\n" +
            ")";
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
