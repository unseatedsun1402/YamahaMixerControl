package MidiControl.unit.MidiDeviceManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.google.gson.*;

import MidiControl.MidiDeviceManager.ServerSettings;
import MidiControl.MidiDeviceManager.Settings;
import MidiControl.SysexUtils.MappingFiles;

@Tag("unit")
public class SettingsTest {
    @Test
    public void testSettingsInstatiate(){
        Settings settings = new ServerSettings();
        assertInstanceOf(Settings.class, settings);
    }

    @Test
    public void testSettingsIsPopulated(){
        Settings settings = new ServerSettings();
        assertInstanceOf(Settings.class, settings);

        int mockInDeviceIndex=0;
        int mockOutDeviceIndex=1;
        String mockInDeviceName="testinput";
        String mockOutDeviceName="testoutput";
        String testConsoleName="YAMAHA_01V96I";

        settings.newSettings(mockInDeviceIndex, mockInDeviceName, mockOutDeviceIndex, mockOutDeviceName, testConsoleName);

        String getSettings = settings.toJson();
        Gson gson = new Gson();
        JsonObject parsedJson = gson.fromJson(getSettings, JsonObject.class);

        assertEquals(mockInDeviceName,parsedJson.get("inputDeviceName").getAsString());
        assertEquals(mockOutDeviceName,parsedJson.get("outputDeviceName").getAsString());
        assertEquals(mockInDeviceIndex, parsedJson.get("inputDeviceIndex").getAsInt());
        assertEquals(mockOutDeviceIndex, parsedJson.get("outputDeviceIndex").getAsInt());
        assertEquals(testConsoleName, parsedJson.get("consoleName").getAsString());
        assertEquals(MappingFiles.getFilePathByKey(testConsoleName), parsedJson.get("consoleMappingsPath").getAsString());
    }

    @Test
    public void testSaveWhenEmptyFails(){
        ServerSettings.clearCache();
        File file = new File(ServerSettings.getSavePath());
        if(file.exists()){file.delete();System.out.println("file deleted "+file.getAbsolutePath());}

        assertFalse(new ServerSettings().saveSettings());
    }

    @Test
    public void testSaveWhenPopulatedSucceeds(){
        Settings settings = new ServerSettings();
        assertInstanceOf(Settings.class, settings);

        int mockInDeviceIndex=0;
        int mockOutDeviceIndex=1;
        String mockInDeviceName="testinput";
        String mockOutDeviceName="testoutput";
        String testConsoleName="testConsole";

        File testFile = new File(ServerSettings.getSavePath());

        settings.newSettings(mockInDeviceIndex, mockInDeviceName, mockOutDeviceIndex, mockOutDeviceName, testConsoleName);
        if(testFile.exists()){testFile.delete();}
        assertTrue(settings.saveSettings());
    }

    @Test
    public void testGetSavePath(){
        assertDoesNotThrow(()-> ServerSettings.getSavePath());
    }

    @Test
    public void testLoadingPreviousSettings(){
        Settings settings = new ServerSettings();
        assertInstanceOf(Settings.class, settings);

        int mockInDeviceIndex=0;
        int mockOutDeviceIndex=1;
        String mockInDeviceName="testinput";
        String mockOutDeviceName="testoutput";
        String testConsoleName="testConsole";

        settings.newSettings(mockInDeviceIndex, mockInDeviceName, mockOutDeviceIndex, mockOutDeviceName, testConsoleName);
        settings.saveSettings();
        
        assertDoesNotThrow(() -> new Gson().fromJson(new ServerSettings().getSettings(), JsonObject.class));
    }

    @Test
    public void testCheckSettingsAreTheSame(){
        Settings settings = new ServerSettings();
        assertInstanceOf(Settings.class, settings);

        int mockInDeviceIndex=0;
        int mockOutDeviceIndex=1;
        String mockInDeviceName="testinput";
        String mockOutDeviceName="testoutput";
        String testConsoleName="testConsole";

        settings.newSettings(mockInDeviceIndex, mockInDeviceName, mockOutDeviceIndex, mockOutDeviceName, testConsoleName);
        settings.saveSettings();
        assertTrue(settings.evalSettings(settings.toJson()));
    }

    @Test
    public void testCheckSettingsAreDifferent(){
        Settings settings = new ServerSettings();
        assertInstanceOf(Settings.class, settings);

        int mockInDeviceIndex=0;
        int mockOutDeviceIndex=1;
        String mockInDeviceName="testinput";
        String mockOutDeviceName="testoutput";
        String testConsoleName="testConsole";

        String testJson="{\"timestamp\":\"notImportant\",\"inputDeviceName\":\"Yamaha 01V96i-1\",\"inputDeviceIndex\":13,"+
        "\"outputDeviceName\":\"Yamaha 01V96i-1\",\"outputDeviceIndex\":4,\"consoleName\":"+
        "\"YAMAHA_01V96I\",\"consoleMappingsPath\":\"MidiControl/01v96i_sysex_mappings.json\"}";

        settings.newSettings(mockInDeviceIndex, mockInDeviceName, mockOutDeviceIndex, mockOutDeviceName, testConsoleName);
        assertFalse(settings.evalSettings(testJson));
    }

    @Test
    public void testDeviceGetters(){
        Settings settings = new ServerSettings();
        assertInstanceOf(Settings.class, settings);

        int mockInDeviceIndex=0;
        int mockOutDeviceIndex=1;
        String mockInDeviceName="testinput";
        String mockOutDeviceName="testoutput";
        String testConsoleName="testConsole";

        File testFile = new File(ServerSettings.getSavePath());

        settings.newSettings(mockInDeviceIndex, mockInDeviceName, mockOutDeviceIndex, mockOutDeviceName, testConsoleName);
        if(testFile.exists()){testFile.delete();}
        assertTrue(settings.saveSettings());

        settings = new ServerSettings();
        assertEquals(mockInDeviceIndex, settings.getInputDeviceIndex());
        assertEquals(mockOutDeviceIndex, settings.getOutputDeviceIndex());
        assertEquals(mockInDeviceName, settings.getInputDeviceName());
        assertEquals(mockOutDeviceName, settings.getOutputDeviceName());
        assertEquals(testConsoleName, settings.getConsoleName());
        assertEquals("empty", settings.getConsoleMappingsFilePath());
    }
}
