package MidiControl.UserInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import MidiControl.ContextModel.BankContext;
import MidiControl.ContextModel.BankFilter;
import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextDiscoveryEngine;
import MidiControl.ContextModel.ContextFactory;
import MidiControl.ContextModel.ContextType;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Server.MidiServer;
import MidiControl.UserInterface.DTO.UiBankDTO;

public class UiBankFactory{

    private final ContextDiscoveryEngine discoveryEngine;
    private final CanonicalRegistry registry;

    public UiBankFactory(ContextDiscoveryEngine discoveryEngine, MidiServer server) {
        this.discoveryEngine = discoveryEngine;
        this.registry = server.getCanonicalRegistry();
    }

    public List<String> sortContextIds(List<Context> contexts) {
        return contexts.stream()
            .sorted((a, b) -> {

                String[] pa = a.getId().split("\\.");
                String[] pb = b.getId().split("\\.");

                int prefixCmp = pa[0].compareTo(pb[0]);
                if (prefixCmp != 0) return prefixCmp;

                try {
                    int ia = Integer.parseInt(pa[1]);
                    int ib = Integer.parseInt(pb[1]);
                    return Integer.compare(ia, ib);
                } catch (Exception ex) {
                    return 0;
                }
            })
        .map(Context::getId)
        .toList();
    }

    private boolean matchesFilters(Context ctx, BankContext bankCtx) {

        for (BankFilter filter : bankCtx.getFilters()) {

            if (filter.getPrefix() != null) {
                if (!ctx.getId().startsWith(filter.getPrefix() + ".")) {
                    continue;
                }
            }

            if (filter.getIndex() != null) {
                String[] parts = ctx.getId().split("\\.");
                if (parts.length != 2) continue;

                try {
                    int ctxIndex = Integer.parseInt(parts[1]);
                    if (ctxIndex != filter.getIndex()) {
                        continue;
                    }
                } catch (NumberFormatException ex) {
                    continue;
                }
            }

            if (filter.getType() != null) {
                if (ctx.getContextType() != filter.getType()) {
                    continue;
                }
            }

            return true;
        }

        return false;
    }

    private Map<String, Object> buildMetadata(Context bankCtx) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("label", bankCtx.getLabel());
        meta.put("contextType", bankCtx.getContextType().name());
        meta.put("rolesAllowed", bankCtx.getRolesAllowed());
        return meta;
    }

    public UiBankDTO buildBank(String bankId, BankContext bankCtx) {

        // Banks are not real contexts — create a synthetic one
        Context bankDefinition = new Context(
            bankId,
            bankId,                 // or a nicer label later
            ContextType.BANK,
            List.of(),              // banks have no role restrictions
            List.of()               // banks have no filters
        );

        List<Context> allContexts = discoveryEngine.discoverContexts();

        List<Context> matches = allContexts.stream()
            .filter(ctx -> matchesFilters(ctx, bankCtx))
            .toList();

        List<String> orderedIds = sortContextIds(matches);

        Map<String, Object> metadata = buildMetadata(bankDefinition);

        UiBankDTO dto = new UiBankDTO();
        dto.contextId = bankId;
        dto.contexts = orderedIds;
        dto.metadata = metadata;

        return dto;
    }
}