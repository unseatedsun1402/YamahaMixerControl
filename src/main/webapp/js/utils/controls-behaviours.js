export function doubleTapReset(
    inputEl,
    canonicalId,
    defaultValue
) {
    if (defaultValue == null) {
        return;
    }

    let lastTap = 0;

    inputEl.addEventListener("pointerdown", () => {

        const now = Date.now();

        if (now - lastTap < 300) {

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