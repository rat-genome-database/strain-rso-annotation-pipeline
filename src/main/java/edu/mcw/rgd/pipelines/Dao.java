package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.dao.AbstractDAO;

/**
 * @author mtutaj
 * @since 5/30/2017
 * All database code lands here
 */
public class Dao {

    AbstractDAO adao = new AbstractDAO();

    public int markAnnotationsForProcessing() throws Exception {
        String sql = "UPDATE FULL_ANNOT\n" +
                "SET FULL_ANNOT.LAST_MODIFIED_DATE=to_date('01/01/1900', 'MM/DD/YYYY')\n" +
                "WHERE FULL_ANNOT.LAST_MODIFIED_BY=180";
        return adao.update(sql);
    }

    public int updateStrainRsoAnnotations() throws Exception {
        String sql = "UPDATE FULL_ANNOT fa\n" +
                "SET\n" +
                "  (\n" +
                "    term,\n" +
                "    object_symbol,\n" +
                "    object_name,\n" +
                "    last_modified_date\n" +
                "  )\n" +
                "  =\n" +
                "  (SELECT ot.TERM,\n" +
                "    st.STRAIN_SYMBOL,\n" +
                "    st.FULL_NAME,\n" +
                "    sysdate\n" +
                "  FROM ONT_TERMS ot,\n" +
                "    strains st,\n" +
                "    rgd_ids\n" +
                "  WHERE \n" +
                "   fa.TERM_ACC              = ot.TERM_ACC\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = st.RGD_ID\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = rgd_ids.RGD_ID\n" +
                "  AND rgd_ids.OBJECT_STATUS      = 'ACTIVE'\n" +
                "  )\n" +
                "WHERE fa.LAST_MODIFIED_BY=180\n" +
                "AND EXISTS\n" +
                "  (SELECT ot.TERM,\n" +
                "    st.STRAIN_SYMBOL,\n" +
                "    st.FULL_NAME,\n" +
                "    sysdate\n" +
                "  FROM ONT_TERMS ot,\n" +
                "    strains st,\n" +
                "    rgd_ids\n" +
                "  WHERE fa.TERM_ACC              = ot.TERM_ACC\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = st.RGD_ID\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = rgd_ids.RGD_ID\n" +
                "  AND rgd_ids.OBJECT_STATUS      = 'ACTIVE'\n" +
                ")";
        return adao.update(sql);
    }

    public int deleteStrainRsoAnnotations() throws Exception {
        String sql = "DELETE\n" +
                "FROM FULL_ANNOT fa\n" +
                "WHERE fa.LAST_MODIFIED_BY = 180\n" +
                "AND fa.LAST_MODIFIED_DATE = to_date('01/01/1900', 'MM/DD/YYYY')";
        return adao.update(sql);
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
            "  7241799,\n" +
            "  'IEA',\n" +
            "  'S',\n" +
            "  st.FULL_NAME,\n" +
            "  sysdate,\n" +
            "  sysdate,\n" +
            "  ot.TERM_ACC,\n" +
            "  180,\n" +
            "  180\n" +
            "FROM ONT_TERMS ot,\n" +
            "  ONT_SYNONYMS os,\n" +
            "  strains st,\n" +
            "  RGD_IDS\n" +
            "WHERE os.SYNONYM_NAME LIKE 'RGD ID:%'\n" +
            "AND to_number(SUBSTR(os.SYNONYM_NAME,9, 100)) = st.RGD_ID\n" +
            "AND st.rgd_id = rgd_ids.rgd_id\n" +
            "AND ot.TERM_ACC = os.TERM_ACC\n" +
            "AND rgd_ids.OBJECT_STATUS = 'ACTIVE'\n" +
            "AND NOT EXISTS\n" +
            "  (SELECT fa.FULL_ANNOT_KEY\n" +
            "  FROM FULL_ANNOT fa\n" +
            "  WHERE fa.TERM_ACC = ot.TERM_ACC\n" +
            "  AND fa.ANNOTATED_OBJECT_RGD_ID = st.rgd_id\n" +
            "  AND fa.created_by = 180\n" +
            "  )";
        return adao.update(sql);
    }
}
