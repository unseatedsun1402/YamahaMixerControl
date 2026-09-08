import { renderSelect } from "./select.js";

function loadPatchMatrixCss() {
  const link = document.createElement("link");
  link.rel = "stylesheet";
  link.href = "css/widgets/patch-matrix.css";
  document.head.appendChild(link);
}

export function renderHiddenPatchControls(model, container) {

    if (!container._hiddenControls) {
        container._hiddenControls = new Map();
        loadPatchMatrixCss();
    }

    for (const control of model.controls) {

        const canonicalId = control.canonicalId;

        if (container._hiddenControls.has(canonicalId)) {
            continue;
        }

        const hidden = renderSelect(control);
        hidden.style.display = "none";

        container.appendChild(hidden);
        container._hiddenControls.set(canonicalId, hidden);
    }
}

export function renderPatchMatrixOverlay(container) {

    if (container._canvas) {
        container._canvas.remove();
    }

    const canvas = document.createElement("canvas");
    canvas.className = "patch-matrix-canvas";
    container._canvas = canvas;

    container.appendChild(canvas);

    const ctx = canvas.getContext("2d");

    const rows = [...container._hiddenControls.entries()];
    const anyHidden = rows[0][1];
    const select = anyHidden.querySelector("select");

    const minSrc = Number(select.options[0].value);
    const maxSrc = Number(select.options[select.options.length - 1].value);

    const cellW = 32;
    const cellH = 24;
    const labelW = 120;

    const dpr = window.devicePixelRatio || 1;

    canvas.width = (labelW + (maxSrc - minSrc + 1) * cellW) * dpr;
    canvas.height = (rows.length * cellH) * dpr;

    canvas.style.width = labelW + (maxSrc - minSrc + 1) * cellW + "px";
    canvas.style.height = rows.length * cellH + "px";

    ctx.scale(dpr, dpr);

    // store state
    container._canvasState = {
        ctx,
        rows,
        minSrc,
        maxSrc,
        cellW,
        cellH,
        labelW
    };

    drawMatrix(container);
    attachCanvasEvents(container);
    attachUpdateListeners(container);
}

function drawMatrix(container) {
    const canvas = container._canvas;
    const {
        ctx, rows, minSrc, maxSrc,
        cellW, cellH, labelW
    } = container._canvasState;

    const headerH = 24;
    const marginLeft = 24;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    ctx.font = "12px Segoe UI";
    ctx.fillStyle = "#e6e6e6";
    ctx.imageSmoothingEnabled = false;
    ctx.textBaseline = "top";

    // header
    for (let src = minSrc; src <= maxSrc; src++) {
        const x = marginLeft + labelW + (src - minSrc) * cellW;
        ctx.fillText(`S${src}`, x + 4, 4);
    }

    // rows
    rows.forEach(([canonicalId, hidden], rowIndex) => {
        const hiddenSelect = hidden.querySelector("select");
        const current = Number(hiddenSelect.value);

        const y = headerH + rowIndex * cellH;

        ctx.fillStyle = "#e6e6e6";
        ctx.fillText(canonicalId, marginLeft, y + 4);

        for (let src = minSrc; src <= maxSrc; src++) {
            const x = marginLeft + labelW + (src - minSrc) * cellW;

            ctx.fillStyle = (src === current) ? "#4da3ff" : "#1a1a1c";
            ctx.fillRect(x, y, cellW - 1, cellH - 1);
        }
    });

    // keep headerH/marginLeft in state for hit‑testing
    container._canvasState.headerH = headerH;
    container._canvasState.marginLeft = marginLeft;
}

function attachCanvasEvents(container) {
    const canvas = container._canvas;
    const {
        rows, minSrc, maxSrc,
        cellW, cellH, labelW,
        headerH, marginLeft
    } = container._canvasState;

    canvas.addEventListener("click", (e) => {
        const rect = canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        // ignore header
        if (y < headerH) return;

        const rowIndex = Math.floor((y - headerH) / cellH);
        const srcIndex = Math.floor((x - (marginLeft + labelW)) / cellW) + minSrc;

        if (rowIndex < 0 || rowIndex >= rows.length) return;
        if (srcIndex < minSrc || srcIndex > maxSrc) return;

        const [, hidden] = rows[rowIndex];
        const hiddenSelect = hidden.querySelector("select");

        hiddenSelect.value = String(srcIndex);
        hiddenSelect.dispatchEvent(new Event("change"));
        hidden.dispatchEvent(new CustomEvent("control-update", { detail: { value: srcIndex } }));

        drawMatrix(container);
    });
}

function attachUpdateListeners(container) {
    const { rows } = container._canvasState;

    rows.forEach(([canonicalId, hidden]) => {
        hidden.addEventListener("control-update", () => {
            drawMatrix(container);
        });
    });
}
