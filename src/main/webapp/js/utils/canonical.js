export function canonicalIdFor(control) {
    return `${control.hwGroup}.${control.hwSubcontrol}.${control.hwInstance}`;
}