package MidiControl.MidiDeviceManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import MidiControl.SysexUtils.MappingFiles;
import MidiControl.SystemTools.ConfigDirectoryProvider;
import jakarta.json.Json;

public class ServerSettings implements Settings{
  private static Logger logger = Logger.getLogger(ServerSettings.class.getName());
  private static final String SETTINGS_DIR="MidiControl";
  private static final String savePath;
  
  public int inputDeviceIndex;
  public String inputDeviceName;
  public int outputDeviceIndex;
  public String outputDeviceName;

  public String consoleMappingsPath;
  private String consoleName;

  public ServerSettings(){
    loadSettings();
  }

  static {
    String configPath = ConfigDirectoryProvider.getConfigPath(SETTINGS_DIR);
    String settingsName = "settings.conf";
    if(System.getProperty("os.name").toLowerCase().contains("win")){savePath=configPath+"\\"+settingsName;}
    else{savePath=configPath+"\\"+settingsName;}
  }

  public static String getSavePath(){return savePath;}

  @Override
  public String getSettings(){
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

  public void loadSettings(){
    String json = getSettings();
    if (json == "empty"){
      logger.warning("Loading settings to "+this.getClass().getName()+" failed");
      return;
    }
    JsonObject loadedSettings = new Gson().fromJson(json,JsonObject.class);
    this.inputDeviceIndex = loadedSettings.get("inputDeviceIndex").getAsInt();
    this.outputDeviceIndex = loadedSettings.get("outputDeviceIndex").getAsInt();
    this.inputDeviceName = loadedSettings.get("inputDeviceName").getAsString();
    this.outputDeviceName = loadedSettings.get("outputDeviceName").getAsString();
    this.consoleName = loadedSettings.get("consoleName").getAsString();
    this.consoleMappingsPath = loadedSettings.get("consoleMappingsPath").getAsString();
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
    logger.fine("Saving settings "+toJson());
    if(consoleName == null){
      return false;
    }
    if(evalSettings(getSettings())){
      logger.info("Settings have the same data, nothing new to write");
      return true;
    }
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(savePath))) {
        writer.write(toJson());
    } catch (IOException e) {
        System.err.println("An error occurred while writing to the settings file: " + e.getMessage());
        return false;
    }
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
          JsonObject expected = gson.fromJson(this.toJson(), JsonObject.class);
          JsonObject actual   = gson.fromJson(json, JsonObject.class);
          expected.remove("timestamp");
          actual.remove("timestamp");
          return expected.equals(actual);

      } catch (Exception e) {
        logger.warning("Json comparision check to see if current settings have changed has failed" + e);
        return false;
      }
  }
}
