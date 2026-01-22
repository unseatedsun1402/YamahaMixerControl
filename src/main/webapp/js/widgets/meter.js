
console.log(">>> Meter.js LOADED <<<");

function normaliseRawValue(raw, is14Bit) {
    const is14 = (is14Bit === true || is14Bit === "true");
    const max = is14 ? 4368 : 127;
    const normalised = Math.floor((raw / max) * 15);
    return Math.min(Math.max(normalised, 0), 15);
}

function throttle(updateFn, interval = 100) {
    let lastTime = 0;
    let pending = null;

    return (...args) => {
        const now = performance.now();

        if (now - lastTime >= interval) {
            lastTime = now;
            updateFn(...args);
        } else {
            pending = args;
            setTimeout(() => {
                if (pending && performance.now() - lastTime >= interval) {
                    updateFn(...pending);
                    lastTime = performance.now();
                    pending = null;
                }
            }, interval);
        }
    };
}


export function createSevenSegmentMeter() {
    const wrapper = document.createElement("div");
    wrapper.className = "seven-segment-meter";

    // Channel label
    const channelLabel = document.createElement("div");
    channelLabel.className = "meter-channel-label";
    channelLabel.textContent = "--";
    wrapper.appendChild(channelLabel);

    // Main value display
    const display = document.createElement("div");
    display.className = "seven-segment-display";
    display.textContent = "--";
    wrapper.appendChild(display);

    // dB label
    const dbLabel = document.createElement("div");
    dbLabel.className = "meter-db-label";
    dbLabel.textContent = "-- dB";
    wrapper.appendChild(dbLabel);

    wrapper.update = throttle((value, dB, offset) => {
        channelLabel.textContent = `CH ${offset}`;
        display.textContent = value;
        dbLabel.textContent = `${dB} dB`;

        const numericDb = parseFloat(dB);
        if (numericDb > -20) display.style.color = "red";
        else if (numericDb > -40) display.style.color = "yellow";
        else display.style.color = "#0f0";
    }, 100);

    return wrapper;
}



export function createFifteenSegmentMeter() {
    const wrapper = document.createElement("div");
    wrapper.className = "fifteen-segment-meter";

    // Channel label
    const channelLabel = document.createElement("div");
    channelLabel.className = "meter-channel-label";
    channelLabel.textContent = "--";
    wrapper.appendChild(channelLabel);

    // Segments
    const segments = [];
    for (let i = 0; i < 16; i++) {
        const seg = document.createElement("div");
        seg.className = "segment";
        wrapper.appendChild(seg);
        segments.push(seg);
    }

    // dB label
    const dbLabel = document.createElement("div");
    dbLabel.className = "meter-db-label";
    dbLabel.textContent = "-- dB";
    wrapper.appendChild(dbLabel);

    wrapper.update = throttle((rawValue, dB, is14Bit, offset) => {

        channelLabel.textContent = `CH ${offset}`;
        dbLabel.textContent = `${dB} dB`;

        const level = normaliseRawValue(rawValue, is14Bit);

        wrapper.title = `Level: ${dB} dB`;

        const OFF = "#222";

        for (let i = 0; i < level; i++) {
            let colour;
            if (i < 10) colour = "#0f0";
            else if (i < 13) colour = "yellow";
            else colour = "red";

            if (segments[i].style.backgroundColor !== colour) {
                segments[i].style.backgroundColor = colour;
            }
        }

        for (let i = level; i < segments.length; i++) {
            if (segments[i].style.backgroundColor !== OFF) {
                segments[i].style.backgroundColor = OFF;
            }
        }

    }, 100);

    return wrapper;
}

 