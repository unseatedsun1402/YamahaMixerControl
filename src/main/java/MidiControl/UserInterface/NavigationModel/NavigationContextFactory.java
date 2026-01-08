package MidiControl.UserInterface.NavigationModel;

import MidiControl.ContextModel.ControlSchema;
import MidiControl.Controls.CanonicalRegistry;

public class NavigationContextFactory {

    private final CanonicalRegistry registry;
    private final ControlSchema schema;

    public NavigationContextFactory(CanonicalRegistry registry, ControlSchema schema) {
        this.registry = registry;
        this.schema = schema;
    }

    public NavigationModel buildNavigation() {
        NavigationModel model = new NavigationModel();

        // Inputs
        addCategory(model, "Inputs", "channel", detectCount("kFader"), "CH");

        // Mixes
        addCategory(model, "Mixes", "mix", detectCount("kMix"), "Mix");

        // Matrices
        addCategory(model, "Matrices", "matrix", detectCount("kMatrix"), "Matrix");

        // DCAs
        addCategory(model, "DCAs", "dca", detectCount("kDCAName"), "DCA");

        // Stereo Inputs (optional)
        addCategory(model, "Stereo Inputs", "stereo", detectCount("kStereoInChannelName"), "ST");

        return model;
    }

    private int detectCount(String prefix) {
        return schema.detectIndexedRangeForAnyGroup(prefix);
    }

    private void addCategory(NavigationModel model, String categoryName, String idPrefix, int count, String labelPrefix) {
        if (count <= 0)
            return;

        NavigationCategory cat = new NavigationCategory();
        cat.category = categoryName;

        for (int i = 1; i <= count; i++) {
            String id = idPrefix + "." + i;
            String label = labelPrefix + " " + i;
            cat.contexts.add(new NavigationItem(id, label));
        }

        model.navigation.add(cat);
    }
}
