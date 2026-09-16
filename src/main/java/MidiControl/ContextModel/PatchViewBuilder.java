package MidiControl.ContextModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gson.Gson;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PatchViewBuilder implements ViewBuilder {

    private static final Logger log = Logger.getLogger(PatchViewBuilder.class.getName());

    private static String deskCache;
    private static List<ChannelSource> sourceCache = List.of();

    private static final Set<String> OUTPUT_GROUPS = Set.of(
        "kOmniOutput",
        "kSlotOutput",
        "kAdatOutput",
        "kUsbOutput",
        "kDigital2trOut",
        "kDirectOut",
        "kStereoOut",
        "kGEQInsertion",
        "kBusInsertInput",
        "kAUXInsertInput",
        "kMatrixInsertInput",
        "kStereoInsertInput"
    );

    private static final List<String> OUTPUT_SUBCONTROL_PREFIXES = List.of(
        "kSlotOut",
        "kOmniOut",
        "kBusInsertIn",
        "kAUXInsertIn",
        "kMatrixInsertIn",
        "kStereoInsertIn",
        "kDirectOutIndex",
        "kDigital2trOutIndex",
        "kStereoOutIndex",
        "kGEQInsert"
    );


    @Override
    public List<ViewControl> build(Context context,
                                CanonicalRegistry registry,
                                String suffix) {

        List<ViewControl> result = new ArrayList<>();

        String deskModel   = registry.getDeskType();
        ContextType type   = context.getContextType();   // <- use this, don't cache
        String viewType    = "channel-view";
        String viewSuffix  = suffix != null ? suffix : context.getId();

        // Cache only by desk model
        if (!deskModel.equals(deskCache)) {
            deskCache = deskModel;

            log.fine(() -> String.format(
                "Loading source map for type=%s desk=%s",
                type, deskModel
            ));

            // sourceCache = (type == ContextType.CHANNEL)
            //         ? ChannelSourceMapLoader.load(deskModel)
            //         : OutputSourceMapLoader.load(deskModel);
            sourceCache = ChannelSourceMapLoader.load(deskModel);
        }

        int ch = extractContextIndex(context.getId());
        if (ch < 0) {
            log.warning(() -> String.format(
                "[PatchViewBuilder] Could not extract index from contextId=%s",
                context.getId()
            ));
            return result;
        }

        List<ControlInstance> all = new ArrayList<>();
        for (ContextFilter filter : context.getFilters()) {
            all.addAll(registry.getInstancesForFilter(filter));
        }

        ControlInstance patchTarget =
            all.stream()
            .filter(instance -> isPatchDestination(instance, type))
            .findFirst()
            .orElse(null);

        if (patchTarget == null) {
            log.warning(() -> String.format(
                "No patch destination found for contextId=%s type=%s",
                context.getId(), type
            ));
            return result;
        }

        log.info(String.format(
            "[PatchViewBuilder]   Control: group=%s sub=%s idx=%d",
            patchTarget.getGroup(),
            patchTarget.getSubcontrol(),
            patchTarget.getInstanceIndex()
        ));
        
        result.add(createPatchSelector(patchTarget, viewType, viewSuffix, ch, type));
        return result;
    }


    private boolean isPatchDestination(ControlInstance ci, ContextType type) {
        String g = ci.getGroup();
        String s = ci.getSubcontrol();

        log.info(String.format("Current type to check %s against %s.%s",type, g,s));

        return switch (type) {
            case CHANNEL -> (
                g.equals("kChannelInput") ||
                g.equals("kChannelInsertInput") ||
                g.equals("kEffectInput") ||
                s.contains("PatchIn")
            );

            case OUTPUT -> (
                OUTPUT_GROUPS.contains(g) &&
                OUTPUT_SUBCONTROL_PREFIXES.stream().anyMatch(prefix -> s.startsWith(prefix))
            );

            default -> false;
        };
    }

    private ViewControl createPatchSelector(ControlInstance ci,
                                            String viewType,
                                            String viewSuffix,
                                            int channelIndex,
                                            ContextType type) {

        int min = sourceCache.get(0).source;
        int max = sourceCache.get(sourceCache.size() - 1).source;

        String label = switch (type) {
            case CHANNEL -> "Input Patch";
            case MIX     -> "Bus Patch";
            case OUTPUT  -> "Output Patch";
            default      -> "Patch";
        };

        return new ViewControl(
            "PATCH_SELECTOR",
            "input.patch",
            label,
            ControlType.SELECTOR,
            0,
            min,
            max,
            ci.getValue(),
            (channelIndex + 1),
            ci.getGroup(),
            ci.getSubcontrol(),
            ci.getInstanceIndex(),
            viewType,
            viewSuffix,
            "INPUT_PATCH",
            null,
            channelIndex
        );
    }

    private int extractContextIndex(String contextId) {
        int dot = contextId.lastIndexOf('.');
        if (dot == -1) return -1;

        try {
            return Integer.parseInt(contextId.substring(dot + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static class SourceMapServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            if (deskCache == null || sourceCache == null || sourceCache.isEmpty()) {

                ErrorResponse err = new ErrorResponse(
                    "NO_SOURCE_MAP",
                    "Source map not yet available. UI models may not be loaded."
                );

                new Gson().toJson(err, resp.getWriter());
                return;
            }

            new Gson().toJson(sourceCache, resp.getWriter());
        }

        private static class ErrorResponse {
            public final String status;
            public final String message;

            public ErrorResponse(String status, String message) {
                this.status = status;
                this.message = message;
            }
        }
    }
}
