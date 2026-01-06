export const clamp = (v, min, max) => Math.max(min, Math.min(max, v));

export function valueToAngle(value, min, max) {
    const percent = (value - min) / (max - min);
    return -135 + percent * 270;
}