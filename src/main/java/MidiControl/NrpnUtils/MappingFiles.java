package MidiControl.NrpnUtils;

public enum MappingFiles {
    YAMAHA_01V96I("MidiControl/nrpn/01v96i_nrpn_mappings.json"),
    YAMAHA_M7CL("MidiControl/nrpn/m7cl_nrpn_mappings.json");

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
