package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

/**
 * Created by mtutaj on 5/30/2017
 */
public class StrainRsoAnnotation {

    private String version;

    Logger log = Logger.getRootLogger();

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

        Dao dao = new Dao();

        // Mark all annotations that were created by this pipeline: Last_modified=180
        int rowsAffected = dao.markAnnotationsForProcessing();
        log.info("Annotations marked for processing: "+rowsAffected);

        // Update valid annotations with the latest terms, names, symbols and last_modified_date
        rowsAffected = dao.updateStrainRsoAnnotations();
        log.info("Annotations updated: "+rowsAffected);

        // Delete obsolete annotations which are not touched by the update annotations updates
        rowsAffected = dao.deleteStrainRsoAnnotations();
        log.info("Records deleted: "+rowsAffected);

        // Insert new annotations
        rowsAffected = dao.insertStrainRsoAnnotations();
        log.info("New records inserted: "+rowsAffected);


        String msg = "=== OK === elapsed "+ Utils.formatElapsedTime(time0, System.currentTimeMillis());
        log.info(msg);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
