package ma.projet.util;
import ma.projet.classes.*;
import ma.projet.beans.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import java.util.*;
import java.io.*;

public final class HibernateUtil {
    private HibernateUtil() {}
    private static final Class<?>[] ENTITIES={Categorie.class,Produit.class,Commande.class,LigneCommandeProduit.class,Employe.class,Projet.class,Tache.class,EmployeTache.class,Personne.class,Homme.class,Femme.class,Mariage.class};
    private static class Holder { static final SessionFactory FACTORY=build("evaluation",false); }
    public static SessionFactory getSessionFactory() { return Holder.FACTORY; }
    public static SessionFactory build(String database,boolean test) {
        boolean mysql=(!test&&"mysql".equals(System.getProperty("db")))||(test&&"mysql".equals(System.getProperty("test.db")));
        Properties properties=new Properties();
        String resource=mysql?"application-mysql.properties":"application.properties";
        try(InputStream input=HibernateUtil.class.getClassLoader().getResourceAsStream(resource)) {
            if(input==null)throw new IllegalStateException("Configuration introuvable : "+resource);
            properties.load(input);
        } catch(IOException e) { throw new IllegalStateException("Lecture de configuration impossible",e); }
        if(!mysql)properties.setProperty("hibernate.connection.url","jdbc:h2:mem:"+database+";DB_CLOSE_DELAY=-1");
        else {
            for(var pair:Map.of("DB_URL","hibernate.connection.url","DB_USER","hibernate.connection.username","DB_PASSWORD","hibernate.connection.password").entrySet()) {
                String value=System.getenv(pair.getKey());if(value!=null)properties.setProperty(pair.getValue(),value);
            }
            if(test)properties.setProperty("hibernate.hbm2ddl.auto","create-drop");
        }
        Configuration configuration=new Configuration().setProperties(properties);
        for(Class<?> entity:ENTITIES)configuration.addAnnotatedClass(entity);
        return configuration.buildSessionFactory();
    }
    public static void shutdown() { getSessionFactory().close(); }
}
