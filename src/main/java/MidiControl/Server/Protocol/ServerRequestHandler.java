
package MidiControl.Server.Protocol;

@FunctionalInterface
public interface ServerRequestHandler<C> {
    void handle(C context, ServerRequest request);
}
