package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mtutaj on 5/30/2017
 */
public class StrainRsoAnnotation {

    private String version;
    private Dao dao;

    Logger log = Logger.getLogger("summary");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        StrainRsoAnnotation manager = (StrainRsoAnnotation) (bf.getBean("manager"));

        try {
            manager.run();
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void run() throws Exception {
        long time0 = System.currentTimeMillis();

        log.info(getVersion());

        Date dateStart = new Date();
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("  started at: "+sdt.format(dateStart));
        log.info("  "+dao.getConnectionInfo());
        log.info("===");

        // Update valid annotations with the latest terms, names, symbols and last_modified_date
        int rowsAffected = dao.updateStrainRsoAnnotations();
        log.info("Annotations updated: "+rowsAffected);

        // Delete obsolete annotations which are not touched by the update annotations updates
        rowsAffected = dao.deleteStrainRsoAnnotations();
        log.info("Annotations deleted: "+rowsAffected);

        // Insert new annotations
        rowsAffected = dao.insertStrainRsoAnnotations();
        log.info("Annotations inserted: "+rowsAffected);


        String msg = "=== OK === elapsed "+ Utils.formatElapsedTime(time0, System.currentTimeMillis());
        log.info(msg);
        log.info("");
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Dao getDao() {
        return dao;
    }
}
