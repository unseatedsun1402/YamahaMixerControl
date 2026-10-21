package MidiControl.MidiDeviceManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import MidiControl.SysexUtils.MappingFiles;
import MidiControl.SystemTools.ConfigDirectoryProvider;

public class ServerSettings implements Settings{
    private static Logger logger = Logger.getLogger(ServerSettings.class.getName());
    private static final String SETTINGS_DIR="MidiControl";
    private static String savePath;
    private static String cache = null;
  
    public int inputDeviceIndex = -1;
    public String inputDeviceName;
    public int outputDeviceIndex = -1;
    public String outputDeviceName;

    public String consoleMappingsPath;
    private String consoleName;

    public ServerSettings(){
        loadSettings();
    }

    static {
        String configPath = ConfigDirectoryProvider.getConfigPath(SETTINGS_DIR);
        String settingsName = "settings.conf";
        savePath=configPath+"\\"+settingsName;
        if(System.getProperty("os.name").toLowerCase().contains("win")) savePath=configPath+"\\"+settingsName;
        logger.info(String.format("Save path for settings.conf: %s",savePath));
    }

    public static String getSavePath(){return savePath;}

    @Override
    public String getSettings(){
        if(cache != null){logger.fine("Cache hit "+cache); return cache;}
        logger.info(String.format("Reading settings from save path: %s",savePath));
        try (BufferedReader reader = new BufferedReader(new FileReader(savePath))){
            boolean reading = true;
            String settings="";
            String line;
            while (reading){
                line = reader.readLine();
                if(line == null){break;}
                settings+=line;
            }
            reader.close();
            return settings;
        }
        catch (IOException e){
            logger.warning("Failed to open "+savePath);
        }

        return "empty";
    }

    public void loadSettings() {

        if(cache == null) {
            String json = getSettings();

            if("empty".equals(json)) {
                logger.warning("Loading settings failed");
                return;
            }

            if(json == null || json.trim().isEmpty()) {
                logger.warning("Settings file is empty");
                return;
            }

            cache = json;
        }

        try {

            JsonObject loadedSettings =
                new Gson().fromJson(cache, JsonObject.class);

            if(loadedSettings == null) {
                logger.warning("Settings file contains no JSON object");
                return;
            }

            inputDeviceIndex =
                loadedSettings.has("inputDeviceIndex")
                ? loadedSettings.get("inputDeviceIndex").getAsInt() : -1;

            outputDeviceIndex =
                loadedSettings.has("outputDeviceIndex")
                ? loadedSettings.get("outputDeviceIndex").getAsInt() : -1;

            inputDeviceName =
                loadedSettings.has("inputDeviceName")
                ? loadedSettings.get("inputDeviceName").getAsString() : null;

            outputDeviceName =
                loadedSettings.has("outputDeviceName")
                ? loadedSettings.get("outputDeviceName").getAsString() : null;

            consoleName =
                loadedSettings.has("consoleName")
                ? loadedSettings.get("consoleName").getAsString() : null;

            consoleMappingsPath =
                loadedSettings.has("consoleMappingsPath")
                ? loadedSettings.get("consoleMappingsPath").getAsString() : null;

        } catch (Exception e) {

            logger.warning(String.format("Failed to parse settings file: %s", e.getMessage()));

            cache = null;
        }
    }

    public String getOutputDeviceName(){return (this.outputDeviceName != null) ? this.outputDeviceName : "empty";}

    public String getInputDeviceName(){return (this.inputDeviceName != null) ? this.inputDeviceName : "empty";}

    public int getOutputDeviceIndex(){return (this.outputDeviceIndex != -1) ? this.outputDeviceIndex : -1;}

    public int getInputDeviceIndex(){return (this.inputDeviceIndex != -1) ? this.inputDeviceIndex : -1;}

    public String getConsoleMappingsFilePath(){return (this.consoleMappingsPath != null) ? this.consoleMappingsPath : "empty";}

    public String getConsoleName(){return (this.consoleName != null) ? this.consoleName : "empty";}

    public void newSettings(int inputIndex,String inputName,int outputIndex,String outputName, String consoleName) {
    this.inputDeviceIndex = inputIndex;
    this.inputDeviceName = inputName;
    this.outputDeviceIndex = outputIndex;
    this.outputDeviceName = outputName;
    this.consoleName = consoleName;
    String path = (MappingFiles.getFilePathByKey(consoleName) != null) ? MappingFiles.getFilePathByKey(consoleName) : "empty";
    this.consoleMappingsPath = path;
    }

    public boolean saveSettings() {
    String toSave = toJson();
    try{
        new Gson().fromJson(toSave,JsonObject.class);
    }
    catch (JsonSyntaxException e){
        logger.severe(String.format("Settings json is invalid %e",e));
        return false;
    } 

    logger.info(String.format("Saving settings: %s",toSave));
    if(consoleName == null){
        logger.warning("Cannot save setings - the console name is empty");
        return false;
    }

    if(evalSettings(toSave)){
        logger.info("Settings have the same data, nothing new to write");
        return true;
    }

    logger.info(String.format("Writing settings to %s",savePath));
    
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(savePath))) {
        writer.write(toSave);
    } catch (IOException e) {
        logger.severe("An io error occurred while writing to the settings file: " + e.getMessage());
        return false;
    }

    cache = null;
    String written = getSettings();

    if(!toSave.equals(written)) {
        logger.severe("Settings save verification failed");
        logger.severe("Expected: " + toSave);
        logger.severe("Actual  : " + written);
        return false;
    }

    cache = toSave;
    logger.info("Written new settings to: "+savePath);
    return true;
    }

    public String toJson() {
        return "{"
            +     "\"timestamp\": \"" + java.time.Instant.now() + "\","
            +     "\"inputDeviceName\": \"" + inputDeviceName + "\","
            +     "\"inputDeviceIndex\": " + inputDeviceIndex + ","
            +     "\"outputDeviceName\": \"" + outputDeviceName + "\","
            +     "\"outputDeviceIndex\": " + outputDeviceIndex + ","
            +     "\"consoleName\": \"" + consoleName + "\","
            +     "\"consoleMappingsPath\": \"" + consoleMappingsPath + "\""
            + "}";
    }


    public boolean evalSettings(String json) {
        try {
            Gson gson = new Gson();
            JsonObject expected = gson.fromJson(json, JsonObject.class);          //evaluate
            JsonObject actual   = gson.fromJson(getSettings(), JsonObject.class); //cache version
            expected.remove("timestamp");
            actual.remove("timestamp");
            return expected.equals(actual);

        } catch (Exception e) {
        logger.warning("Json comparision check to see if current settings have changed has failed" + e);
        return false;
        }
    }

    public static void clearCache(){
    cache = null;
    logger.info("Cache cleared manually");
    }
}
