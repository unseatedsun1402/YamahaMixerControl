export function doubleTapReset(
    inputEl,
    getCanonicalId,
    defaultValue
) {
    if (defaultValue == null) {
        return;
    }

    let lastTap = 0;
    inputEl.resetDoubleTap = () => { lastTap = 0; };

    inputEl.addEventListener("pointerdown", () => {

        const now = Date.now();

        if (now - lastTap < 300) {

            const canonicalId = getCanonicalId();
            console.log(
                "RESET",
                canonicalId,
                defaultValue
            );

            window.wsClient?.sendControlChange(
                canonicalId,
                Number(defaultValue)
            );
        }

        lastTap = now;
    });
}