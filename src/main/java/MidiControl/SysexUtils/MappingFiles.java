
package MidiControl.SysexUtils;

public enum MappingFiles {
    YAMAHA_01V96I("MidiControl/01v96i_sysex_mappings.json"),
    YAMAHA_M7CL("MidiControl/m7cl_sysex_mappings.json");

    private final String filePath;

    MappingFiles(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public static String getFilePathByKey(String key) {
        for (MappingFiles mapping : MappingFiles.values()) {
            if (mapping.name().equalsIgnoreCase(key)) {
                return mapping.getFilePath();
            }
        }
        return null;
    }
}
