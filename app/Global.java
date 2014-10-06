import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import models.entity.PropertyModel;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.db.DB;
import settings.Settings;
import utils.StringUtils;

import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;


public class Global extends GlobalSettings
{
    @Override
    public void onStart(Application app)
    {
        try {
            // init configurations...

            Logger.info("Load Settings");
            initSettings();

            Logger.info("Load Property Data");
            new PropertyModel().loadFromFile();

            // Logger.info("Load Concept Data");
            // new ConceptModel().loadFromFile();

            Logger.info("Application has started");
        } catch(QueryExceptionHTTP e) {
            Logger.info("Error during startup! " + StringUtils.getStackTrace(e));
        }
    }


    @Override
    public void onStop(Application app)
    {
        Logger.info("Application shutdown...");

    }


    private void initSettings()
    {
        // Web Console server running at http://192.168.0.10:8082 (only local connections)
        // TCP server running at tcp://192.168.0.10:9092 (only local connections)
        // PG server running at pg://192.168.0.10:5435 (only local connections)
        // jdbc:h2:tcp://localhost/file:lode

        try {
            // DataSource ds = DB.getDataSource();
            Statement stmt;
            boolean foundTable = false;
            Connection connection = DB.getConnection();

            // drop table
            /*
             * stmt = connection.createStatement();
             * stmt.executeUpdate("DROP TABLE IF EXISTS " + Setting.DB_SETTINGS_TABLE);
             * stmt.close();
             */

            // check if table exists
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet res = meta.getTables(null, null, null, new String[] { "TABLE" });
            while(res.next()) {
                // System.out.println("   " + res.getString("TABLE_CAT") + ", " + res.getString("TABLE_SCHEM") + ", " + res.getString("TABLE_NAME") + ", " + res.getString("TABLE_TYPE") + ", " + res.getString("REMARKS"));
                if(res.getString("TABLE_NAME").equalsIgnoreCase(Settings.DB_SETTINGS_TABLE))
                    foundTable = true;
            }

            // create table
            if(!foundTable) {
                String createString = "create table " + Settings.DB_SETTINGS_TABLE + "(S_ID VARCHAR(20) NOT NULL, S_VALUE NVARCHAR(MAX) NOT NULL, PRIMARY KEY(S_ID))";
                stmt = connection.createStatement();
                stmt.executeUpdate(createString);
                stmt.close();
                Settings.saveSettings();
            }
            connection.close();

            // Settings.saveSettings(); // set default values
            Settings.loadSettings();

        } catch(SQLException ex) {
            System.err.println("SQLException: " + ex.getMessage());
        } catch(QueryExceptionHTTP e) {
            Logger.info("Error during startup! " + StringUtils.getStackTrace(e));
        }

    }
}