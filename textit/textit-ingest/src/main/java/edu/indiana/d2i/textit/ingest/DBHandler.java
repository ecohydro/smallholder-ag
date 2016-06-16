package edu.indiana.d2i.textit.ingest;

import edu.indiana.d2i.textit.ingest.utils.MongoDB;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by charmadu on 6/15/16.
 */
public class DBHandler {

    private String output_dir =  "./output";
    public static final String FLOWS = "flows";
    public static final String RUNS = "runs";
    public static final String CONTACTS = "contacts";

    private static Logger logger = Logger.getLogger(DBHandler.class);
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public DBHandler(String output_dir) {
        this.output_dir = output_dir;
        df.setTimeZone(TimeZone.getTimeZone("timezone"));
    }

    public boolean persistData(){
        JSONObject statusObject = new JSONObject();
        statusObject.put(MongoDB.DATE, df.format(new Date()));
        statusObject.put(MongoDB.ACTION, MongoDB.WRITE_TO_MONGO);
        statusObject.put(MongoDB.TYPE, "all");

        try {
            saveRawRuns();
            saveRuns();
            saveFlows();
            saveContacts();
        } catch (Exception e) {
            logger.error(e.getMessage());
            statusObject.put(MongoDB.STATUS, MongoDB.FAILURE);
            statusObject.put(MongoDB.MESSAGE, e.getMessage());
            MongoDB.addStatus(statusObject.toString());
            return false;
        }
        statusObject.put(MongoDB.STATUS, MongoDB.SUCCESS);
        MongoDB.addStatus(statusObject.toString());
        return true;
    }

    private boolean saveRawRuns() throws FileNotFoundException {
        String out_dir = output_dir+ "/" + RUNS;
        File dir = new File(out_dir);
        File[] directoryListing = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".json");
            }
        });
        if (directoryListing != null) {
            for (File child : directoryListing) {
                MongoDB.addRawRuns(out_dir, child.getName());
            }
        }
        return true;
    }

    private boolean saveRuns() throws IOException {
        String out_dir = output_dir+ "/" + RUNS;
        File dir = new File(out_dir);
        File[] directoryListing = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".json");
            }
        });
        if (directoryListing != null) {
            for (File child : directoryListing) {
                String fileString = new String(Files.readAllBytes(Paths.get(out_dir + "/" + child.getName())));
                JSONObject runs = new JSONObject(fileString);
                JSONArray runsArray = runs.getJSONArray("results");
                for(int i=0 ; i < runsArray.length() ; i++){
                    JSONObject run = runsArray.getJSONObject(i);
                    MongoDB.addRun(run.getString("flow_uuid"), run.getString("contact")
                            , runsArray.getJSONObject(i).toString());
                }
            }
        }
        return true;
    }

    private boolean saveFlows() throws IOException {
        String out_dir = output_dir+ "/" + FLOWS;
        File dir = new File(out_dir);
        File[] directoryListing = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".json");
            }
        });
        if (directoryListing != null) {
            for (File child : directoryListing) {
                String fileString = new String(Files.readAllBytes(Paths.get(out_dir + "/" + child.getName())));
                JSONObject flows = new JSONObject(fileString);
                JSONArray flowsArray = flows.getJSONArray("results");
                for(int i=0 ; i < flowsArray.length() ; i++){
                    MongoDB.addFlow(flowsArray.getJSONObject(i).getString("uuid"),
                            flowsArray.getJSONObject(i).toString());
                }
            }
        }
        return true;
    }

    private boolean saveContacts() throws IOException {
        String out_dir = output_dir+ "/" + CONTACTS;
        File dir = new File(out_dir);
        File[] directoryListing = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".json");
            }
        });
        if (directoryListing != null) {
            for (File child : directoryListing) {
                String fileString = new String(Files.readAllBytes(Paths.get(out_dir + "/" + child.getName())));
                JSONObject contacts = new JSONObject(fileString);
                JSONArray contactsArray = contacts.getJSONArray("results");
                for(int i=0 ; i < contactsArray.length() ; i++){
                    MongoDB.addContact(contactsArray.getJSONObject(i).getString("uuid"),
                            contactsArray.getJSONObject(i).toString());
                }
            }
        }
        return true;
    }
}