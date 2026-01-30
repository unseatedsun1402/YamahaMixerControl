package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;
import MidiControl.Mocks.MockCanonicalRegistry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

public class OutputContextDiscovererTest {

    @Test
    public void testDiscoverStereoLRMappingNotExists() {
        // Arrange
        CanonicalRegistry registry = new MockCanonicalRegistry();
        OutputContextDiscoverer discoverer = new OutputContextDiscoverer();
        List<Context> contexts = new ArrayList<>();

        // Act
        discoverer.discover(contexts, registry);

        // Assert
        assertTrue(contexts.isEmpty(), "No contexts should be discovered");
    }

    @Test
    public void testDiscoverOmniOutputs() {
        // Arrange
        CanonicalRegistry registry = new MockCanonicalRegistry();
        ControlGroup omniGroup = new ControlGroup("kOmniOut");
        omniGroup.getSubcontrols().put("kOmniOut1Level", new SubControl(omniGroup,"kOmniOut1Level"));
        omniGroup.getSubcontrols().put("kOmniOut2On", new SubControl(omniGroup,"kOmniOut2On"));
        registry.getGroups().put("omni", omniGroup);

        OutputContextDiscoverer discoverer = new OutputContextDiscoverer();
        List<Context> contexts = new ArrayList<>();

        // Act
        discoverer.discover(contexts, registry);

        // Assert
        assertTrue(contexts.stream().anyMatch(c -> c.getId().equals("omni.1")),
                "Expected Omni Out 1 context");
        assertTrue(contexts.stream().anyMatch(c -> c.getId().equals("omni.2")),
                "Expected Omni Out 2 context");
    }
}
