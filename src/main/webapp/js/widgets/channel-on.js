export function createChannelOnWidget(section) {

    const chon = section.querySelector(
        '[data-role="INPUT_CHANNEL_ON"]'
    );

    if (!chon) {
        return;
    }

    if (section.querySelector(".channelon-widget")) {
        return;
    }

    chon.style.display = "none";

    const widget = document.createElement("div");
    widget.className = "channelon-widget";

    const channelEnable =
        document.createElement("button");

    channelEnable.className =
        "toggle-button channelon";

    channelEnable.textContent =
        "Channel On";
    
        
    const backingButton =
        chon?.querySelector(
            ".toggle-button"
        );

    if (
        backingButton?.classList.contains(
            "active"
        )
    ) {
        channelEnable.classList.add(
            "active"
        );
    }

    channelEnable.addEventListener(
        "click",
        () => {

            const enabled =
                !channelEnable.classList.contains(
                    "active"
                );

            channelEnable.classList.toggle(
                "active",
                enabled
            );

            window.wsClient?.sendControlChange(
                chon.dataset.canonicalId,
                enabled ? 127 : 0
            );
        }
    );

    chon.addEventListener(
        "control-update",
        e => {

            channelEnable.classList.toggle(
                "active",
                e.detail.value > 0
            );
        }
    );

    const row =
        document.createElement("div");

    row.className =
        "channelon-toggle-row";

    row.appendChild(
        channelEnable
    );

    widget.appendChild(
        row
    );

    section.appendChild(
        widget
    );
}