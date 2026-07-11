/* @ds-bundle: {"format":4,"namespace":"MaterialFlowDesignSystem_c693e1","components":[{"name":"Button","sourcePath":"components/actions/Button.jsx"},{"name":"IconButton","sourcePath":"components/actions/IconButton.jsx"},{"name":"IconChip","sourcePath":"components/content/IconChip.jsx"},{"name":"ListRow","sourcePath":"components/content/ListRow.jsx"},{"name":"SoundCard","sourcePath":"components/content/SoundCard.jsx"},{"name":"Icon","sourcePath":"components/core/Icon.jsx"},{"name":"SelectTile","sourcePath":"components/forms/SelectTile.jsx"},{"name":"Slider","sourcePath":"components/forms/Slider.jsx"},{"name":"Switch","sourcePath":"components/forms/Switch.jsx"},{"name":"TextField","sourcePath":"components/forms/TextField.jsx"},{"name":"NavBar","sourcePath":"components/navigation/NavBar.jsx"}],"sourceHashes":{"components/actions/Button.jsx":"937123fe8e87","components/actions/IconButton.jsx":"f316ed9b76b8","components/content/IconChip.jsx":"6b9c88b0a06c","components/content/ListRow.jsx":"5091d6041327","components/content/SoundCard.jsx":"e110077a6a95","components/core/Icon.jsx":"0422f8f61027","components/forms/SelectTile.jsx":"036032a457bc","components/forms/Slider.jsx":"78718bf59c09","components/forms/Switch.jsx":"ed468d94ffb6","components/forms/TextField.jsx":"c123d6ae0778","components/navigation/NavBar.jsx":"224f29cffcb2"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.MaterialFlowDesignSystem_c693e1 = window.MaterialFlowDesignSystem_c693e1 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/actions/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Button — Material Flow's primary action control.
 *
 * Variants:
 *  - "gradient": the signature CTA. Carries the moving violet→pink gradient
 *    (idle drift + press-surge). Use for the single most important action.
 *  - "tonal": neutral filled button on a raised surface (Material FilledTonal).
 *  - "text": low-emphasis accent-colored text button.
 *
 * All variants are pill-shaped and 52px tall by default (44px for "sm").
 */
function Button({
  children,
  variant = "gradient",
  size = "md",
  disabled = false,
  leadingIcon,
  fullWidth = false,
  style,
  ...rest
}) {
  const [pressed, setPressed] = React.useState(false);
  const heights = {
    sm: 44,
    md: 52
  };
  const height = heights[size] || heights.md;
  const base = {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: "var(--sp-2)",
    height,
    padding: `0 ${size === "sm" ? "18px" : "24px"}`,
    borderRadius: "var(--radius-pill)",
    border: "none",
    fontFamily: "var(--font-sans)",
    fontSize: "var(--body-size)",
    fontWeight: "var(--fw-semibold)",
    letterSpacing: "0.01em",
    cursor: disabled ? "not-allowed" : "pointer",
    width: fullWidth ? "100%" : "auto",
    userSelect: "none",
    transition: "transform var(--dur-med) var(--ease-emphasized), filter var(--dur-fast) var(--ease-standard)",
    outline: "none"
  };
  const variants = {
    gradient: {
      color: "var(--text-on-accent)",
      backgroundImage: disabled ? "none" : "var(--flow-gradient)",
      backgroundColor: disabled ? "var(--surface-pressed)" : undefined,
      backgroundSize: "var(--flow-size)",
      backgroundPosition: "0% 50%",
      boxShadow: disabled ? "none" : "var(--glow-accent)",
      animation: disabled ? "none" : `flow-drift var(--dur-drift) var(--ease-standard) infinite`
    },
    tonal: {
      color: "var(--text-strong)",
      background: "var(--surface-pressed)"
    },
    text: {
      color: "var(--accent-solid)",
      background: "transparent",
      boxShadow: "none",
      padding: "0 12px"
    }
  };
  const pressedStyle = pressed && !disabled ? variant === "gradient" ? {
    animation: `flow-surge var(--dur-flow) var(--ease-emphasized) forwards`,
    transform: "scale(0.97)"
  } : {
    transform: "scale(0.97)",
    filter: "brightness(1.12)"
  } : null;
  const disabledStyle = disabled ? {
    color: "var(--text-faint)",
    opacity: variant === "text" ? 0.5 : 1
  } : null;
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    disabled: disabled,
    onPointerDown: () => setPressed(true),
    onPointerUp: () => setPressed(false),
    onPointerLeave: () => setPressed(false),
    style: {
      ...base,
      ...variants[variant],
      ...pressedStyle,
      ...disabledStyle,
      ...style
    }
  }, rest), leadingIcon, children);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/actions/Button.jsx", error: String((e && e.message) || e) }); }

// components/content/ListRow.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * ListRow — the grouped list item (Material ListItem). Leading slot (icon chip
 * or glyph), headline + optional supporting text, trailing slot (switch,
 * chevron, play button). Stack rows and set `position` so a group shares 4px
 * inner corners and reads as one card. `highlighted` paints the active wash.
 */
function ListRow({
  leading,
  headline,
  supporting,
  trailing,
  position = "single",
  highlighted = false,
  onClick,
  style,
  ...rest
}) {
  const outer = "var(--radius-md)";
  const inner = "var(--radius-grouped-inner)";
  const top = position === "single" || position === "first" ? outer : inner;
  const bottom = position === "single" || position === "last" ? outer : inner;
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onClick,
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--sp-3)",
      padding: "12px 16px",
      minHeight: 64,
      boxSizing: "border-box",
      borderRadius: `${top} ${top} ${bottom} ${bottom}`,
      background: highlighted ? "var(--surface-card-active)" : "var(--surface-card)",
      cursor: onClick ? "pointer" : "default",
      transition: "background var(--dur-fast) var(--ease-standard)",
      ...style
    }
  }, rest), leading && /*#__PURE__*/React.createElement("div", {
    style: {
      flex: "0 0 auto",
      display: "flex"
    }
  }, leading), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: "1 1 auto",
      minWidth: 0,
      display: "flex",
      flexDirection: "column",
      gap: 3
    }
  }, headline && /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-sans)",
      fontSize: "var(--label-size)",
      fontWeight: "var(--fw-bold)",
      color: "var(--text-strong)",
      lineHeight: 1.2
    }
  }, headline), supporting && /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-sans)",
      fontSize: "var(--body-sm-size)",
      color: "var(--text-muted)",
      lineHeight: 1.35
    }
  }, supporting)), trailing && /*#__PURE__*/React.createElement("div", {
    style: {
      flex: "0 0 auto",
      display: "flex",
      alignItems: "center"
    }
  }, trailing));
}
Object.assign(__ds_scope, { ListRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/content/ListRow.jsx", error: String((e && e.message) || e) }); }

// components/core/Icon.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Icon — renders one glyph from Material Flow's Lucide-based icon set.
 *
 * The SVGs live in `assets/icons/<name>.svg` and are drawn with
 * `stroke="currentColor"`, so the icon is tinted by CSS `color`. We render them
 * via `mask-image` so a single element inherits color from its parent (accent,
 * muted, faint …) exactly like the Compose `Icon(tint = …)` calls.
 *
 * `base` is the URL prefix to the icons folder; override it if your page sits at
 * a different depth than the default.
 */
function Icon({
  name,
  size = 22,
  color = "currentColor",
  base = "assets/icons/",
  style,
  className,
  ...rest
}) {
  const url = `${base}${name}.svg`;
  return /*#__PURE__*/React.createElement("span", _extends({
    className: className,
    "aria-hidden": "true",
    style: {
      display: "inline-block",
      width: size,
      height: size,
      flex: "0 0 auto",
      backgroundColor: color,
      WebkitMaskImage: `url("${url}")`,
      maskImage: `url("${url}")`,
      WebkitMaskRepeat: "no-repeat",
      maskRepeat: "no-repeat",
      WebkitMaskPosition: "center",
      maskPosition: "center",
      WebkitMaskSize: "contain",
      maskSize: "contain",
      ...style
    }
  }, rest));
}
Object.assign(__ds_scope, { Icon });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Icon.jsx", error: String((e && e.message) || e) }); }

// components/actions/IconButton.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * IconButton — a square/circular button wrapping a single glyph.
 *
 * variant:
 *  - "neutral": filled neutral surface (edit / timer FABs, mini row actions).
 *  - "gradient": the signature flowing gradient (the play/pause button).
 * shape:
 *  - "circle": fully round.
 *  - "rounded": soft square (radius-sm) — used for the 28–40px row actions.
 *  - "morph": round when idle, morphs to a 20px squircle when `active` — the
 *    Material media play↔pause behavior. Implies the gradient look.
 */
function IconButton({
  icon,
  size = 62,
  variant = "neutral",
  shape = "circle",
  active = false,
  color,
  iconSize,
  disabled = false,
  elevated = false,
  base = "assets/icons/",
  style,
  ...rest
}) {
  const [pressed, setPressed] = React.useState(false);
  const isGradient = variant === "gradient" || shape === "morph";
  let radius;
  if (shape === "rounded") radius = "var(--radius-sm)";else if (shape === "morph") radius = active ? "20px" : "50%";else radius = "50%";
  const glyphColor = color || (isGradient ? "var(--text-on-accent)" : "var(--text-strong)");
  const gsize = iconSize || Math.round(size * 0.38);
  const box = {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: size,
    height: size,
    borderRadius: radius,
    border: "none",
    cursor: disabled ? "not-allowed" : "pointer",
    padding: 0,
    outline: "none",
    transition: "border-radius var(--dur-med) var(--ease-emphasized), transform var(--dur-med) var(--ease-emphasized), background var(--dur-fast) var(--ease-standard)",
    ...(isGradient ? {
      backgroundImage: "var(--flow-gradient)",
      backgroundSize: "var(--flow-size)",
      backgroundPosition: "0% 50%",
      boxShadow: "var(--glow-accent)",
      animation: `flow-drift var(--dur-drift) var(--ease-standard) infinite`
    } : {
      background: "var(--surface-pressed)",
      boxShadow: elevated ? "var(--shadow-3)" : "none"
    })
  };
  const pressedStyle = pressed && !disabled ? isGradient ? {
    animation: `flow-surge var(--dur-flow) var(--ease-emphasized) forwards`,
    transform: "scale(0.94)"
  } : {
    transform: "scale(0.92)",
    background: "var(--surface-hover)"
  } : null;
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    disabled: disabled,
    onPointerDown: () => setPressed(true),
    onPointerUp: () => setPressed(false),
    onPointerLeave: () => setPressed(false),
    style: {
      ...box,
      ...pressedStyle,
      ...(disabled ? {
        opacity: 0.5
      } : null),
      ...style
    }
  }, rest), typeof icon === "string" ? /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: gsize,
    color: glyphColor,
    base: base
  }) : icon);
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/actions/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/content/IconChip.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * IconChip — the rounded-square icon container used as leading art in cards
 * and rows. Tones: "neutral" (raised surface + muted glyph), "accent"
 * (accent-tint + accent glyph, e.g. preset/settings rows), "active"
 * (translucent white + strong glyph, e.g. an on sound card).
 */
function IconChip({
  icon,
  tone = "neutral",
  size = 44,
  iconSize,
  base = "assets/icons/",
  style,
  ...rest
}) {
  const tones = {
    neutral: {
      bg: "var(--surface-raised)",
      fg: "var(--text-muted)"
    },
    accent: {
      bg: "var(--accent-tint)",
      fg: "var(--accent-solid)"
    },
    active: {
      bg: "rgba(255,255,255,0.18)",
      fg: "var(--text-strong)"
    }
  };
  const t = tones[tone] || tones.neutral;
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      width: size,
      height: size,
      flex: "0 0 auto",
      borderRadius: "var(--radius-sm)",
      background: t.bg,
      transition: "background var(--dur-med) var(--ease-standard)",
      ...style
    }
  }, rest), typeof icon === "string" ? /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: iconSize || Math.round(size * 0.5),
    color: t.fg,
    base: base
  }) : icon);
}
Object.assign(__ds_scope, { IconChip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/content/IconChip.jsx", error: String((e && e.message) || e) }); }

// components/content/SoundCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * SoundCard — Material Flow's signature tile (Google-Home style): the entire
 * box IS the volume control. The accent gradient fills left→right to the
 * current level and, while active, gently FLOWS — the motif that names the
 * system. Tap toggles on/off; horizontal drag sets the level.
 */
function SoundCard({
  name,
  icon,
  volume = 0,
  onVolumeChange,
  onToggle,
  base = "assets/icons/",
  style,
  ...rest
}) {
  const ref = React.useRef(null);
  const v = Math.max(0, Math.min(1, volume));
  const active = v > 0;
  const drag = React.useRef({
    on: false,
    moved: false,
    start: 0,
    startVol: 0,
    w: 0
  });
  const down = e => {
    const el = ref.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    drag.current = {
      on: true,
      moved: false,
      start: e.clientX,
      startVol: v,
      w: r.width
    };
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch (_) {}
  };
  const move = e => {
    const d = drag.current;
    if (!d.on) return;
    const dx = e.clientX - d.start;
    if (Math.abs(dx) > 3) d.moved = true;
    if (d.moved && d.w > 0) {
      const nv = Math.max(0, Math.min(1, d.startVol + dx / d.w));
      onVolumeChange && onVolumeChange(nv);
    }
  };
  const up = () => {
    const d = drag.current;
    if (d.on && !d.moved) onToggle && onToggle();
    d.on = false;
  };
  return /*#__PURE__*/React.createElement("div", _extends({
    ref: ref,
    onPointerDown: down,
    onPointerMove: move,
    onPointerUp: up,
    onPointerCancel: () => drag.current.on = false,
    style: {
      position: "relative",
      overflow: "hidden",
      minHeight: 116,
      padding: "var(--card-pad)",
      borderRadius: "var(--radius-md)",
      background: "var(--surface-card)",
      cursor: "pointer",
      userSelect: "none",
      touchAction: "pan-y",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      inset: 0,
      width: `${v * 100}%`,
      backgroundImage: "var(--flow-gradient)",
      backgroundSize: "var(--flow-size)",
      backgroundPosition: "0% 50%",
      animation: active ? "flow-drift var(--dur-drift) var(--ease-standard) infinite" : "none",
      transition: "width var(--dur-med) var(--ease-emphasized)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      display: "flex",
      flexDirection: "column",
      gap: "var(--sp-3)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.IconChip, {
    icon: icon,
    tone: active ? "active" : "neutral",
    size: 42,
    iconSize: 22,
    base: base
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-mono)",
      fontSize: "var(--mono-caption-size)",
      fontWeight: "var(--mono-caption-weight)",
      letterSpacing: "var(--mono-caption-track)",
      color: active ? "var(--text-strong)" : "var(--text-faint)"
    }
  }, active ? Math.round(v * 100) : "OFF")), /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-sans)",
      fontSize: "var(--label-size)",
      fontWeight: "var(--label-weight)",
      color: active ? "var(--text-strong)" : "var(--text-body)"
    }
  }, name)));
}
Object.assign(__ds_scope, { SoundCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/content/SoundCard.jsx", error: String((e && e.message) || e) }); }

// components/forms/SelectTile.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * SelectTile — a selectable option in a grid. Two looks:
 *  - "gradient" (default): fills with the flowing gradient when selected
 *    (timer presets). Neutral raised surface when not.
 *  - "tint": accent-tint background + accent hairline when selected
 *    (icon picker chips). Neutral raised surface when not.
 */
function SelectTile({
  selected = false,
  variant = "gradient",
  children,
  style,
  ...rest
}) {
  const [pressed, setPressed] = React.useState(false);
  const selGradient = {
    color: "var(--text-on-accent)",
    backgroundImage: "var(--flow-gradient)",
    backgroundSize: "var(--flow-size)",
    backgroundPosition: pressed ? "100% 50%" : "0% 50%",
    border: "1.5px solid transparent"
  };
  const selTint = {
    color: "var(--accent-solid)",
    background: "var(--accent-tint)",
    border: "1.5px solid var(--accent-solid)"
  };
  const unselected = {
    color: "var(--text-body)",
    background: "var(--surface-raised)",
    border: "1.5px solid transparent"
  };
  const chosen = !selected ? unselected : variant === "tint" ? selTint : selGradient;
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    "aria-pressed": selected,
    onPointerDown: () => setPressed(true),
    onPointerUp: () => setPressed(false),
    onPointerLeave: () => setPressed(false),
    style: {
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      minHeight: 52,
      padding: "0 12px",
      borderRadius: "var(--radius-md)",
      cursor: "pointer",
      fontFamily: "var(--font-sans)",
      fontSize: "var(--body-size)",
      fontWeight: "var(--fw-semibold)",
      outline: "none",
      transition: "background-position var(--dur-flow) var(--ease-emphasized), transform var(--dur-med) var(--ease-emphasized), background var(--dur-fast) var(--ease-standard), border-color var(--dur-fast) var(--ease-standard)",
      transform: pressed ? "scale(0.96)" : "none",
      ...chosen,
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { SelectTile });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/SelectTile.jsx", error: String((e && e.message) || e) }); }

// components/forms/Slider.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Slider — Material 3 expressive (Pixel-style) slider.
 * Thick rounded track split in two segments; a thin vertical bar handle
 * travels between them with a 6px gap on each side; the inactive segment
 * carries a small stop-indicator dot at its far end.
 * Active fill = the flow gradient; muted fill = faint grey.
 * One size only: 28px track — anything smaller is too fiddly to use.
 */
function Slider({
  value = 0,
  onChange,
  active = true,
  disabled = false,
  style,
  ...rest
}) {
  const ref = React.useRef(null);
  const [dragging, setDragging] = React.useState(false);
  const v = Math.max(0, Math.min(1, value));
  const d = {
    track: 28,
    hw: 5,
    hh: 44,
    gap: 6,
    dot: 4,
    inner: 4
  };
  const outer = d.track / 2;
  const setFromClientX = clientX => {
    const el = ref.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    const nv = Math.max(0, Math.min(1, (clientX - r.left) / r.width));
    onChange && onChange(nv);
  };
  const down = e => {
    if (disabled) return;
    setDragging(true);
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch (_) {}
    setFromClientX(e.clientX);
  };
  const move = e => {
    if (dragging) setFromClientX(e.clientX);
  };
  const up = () => setDragging(false);
  const fill = active ? "var(--accent-gradient)" : "var(--text-faint)";
  const handleColor = active ? "var(--accent-solid)" : "var(--text-faint)";
  const trans = dragging ? "none" : "width var(--dur-fast) var(--ease-standard), left var(--dur-fast) var(--ease-standard)";
  // handle left edge travels 0 → (100% - hw)
  const handleLeft = `calc((100% - ${d.hw}px) * ${v})`;
  return /*#__PURE__*/React.createElement("div", _extends({
    ref: ref,
    onPointerDown: down,
    onPointerMove: move,
    onPointerUp: up,
    onPointerCancel: up,
    role: "slider",
    "aria-valuenow": Math.round(v * 100),
    "aria-valuemin": 0,
    "aria-valuemax": 100,
    style: {
      position: "relative",
      width: "100%",
      height: d.hh,
      display: "flex",
      alignItems: "center",
      cursor: disabled ? "default" : "pointer",
      touchAction: "none",
      opacity: disabled ? 0.5 : 1,
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      left: 0,
      top: "50%",
      transform: "translateY(-50%)",
      width: `max(0px, calc(${handleLeft} - ${d.gap}px))`,
      height: d.track,
      borderRadius: `${outer}px ${d.inner}px ${d.inner}px ${outer}px`,
      backgroundImage: active ? fill : "none",
      backgroundColor: active ? undefined : "var(--text-faint)",
      transition: trans
    }
  }), /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      right: 0,
      top: "50%",
      transform: "translateY(-50%)",
      width: `max(0px, calc(100% - ${handleLeft} - ${d.hw + d.gap}px))`,
      height: d.track,
      borderRadius: `${d.inner}px ${outer}px ${outer}px ${d.inner}px`,
      background: "var(--surface-pressed)",
      transition: trans
    }
  }), /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      right: outer - d.dot / 2,
      top: "50%",
      transform: "translateY(-50%)",
      width: d.dot,
      height: d.dot,
      borderRadius: "50%",
      background: handleColor,
      opacity: 0.7
    }
  }), /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      left: handleLeft,
      top: "50%",
      transform: "translateY(-50%)",
      width: d.hw,
      height: d.hh,
      borderRadius: d.hw,
      background: handleColor,
      transition: trans
    }
  }));
}
Object.assign(__ds_scope, { Slider });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Slider.jsx", error: String((e && e.message) || e) }); }

// components/forms/Switch.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Switch — Material 3 toggle. Solid accent track when on (gradients aren't used
 * on small controls), surface-app thumb; faint thumb on a raised track when off.
 * The thumb grows on check, mirroring M3.
 */
function Switch({
  checked = false,
  onChange,
  disabled = false,
  style,
  ...rest
}) {
  const W = 52,
    H = 32;
  const thumbOn = 24,
    thumbOff = 16;
  const size = checked ? thumbOn : thumbOff;
  const pad = (H - thumbOn) / 2;
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    role: "switch",
    "aria-checked": checked,
    disabled: disabled,
    onClick: () => !disabled && onChange && onChange(!checked),
    style: {
      position: "relative",
      width: W,
      height: H,
      borderRadius: H / 2,
      border: "none",
      cursor: disabled ? "not-allowed" : "pointer",
      padding: 0,
      background: checked ? "var(--accent-solid)" : "var(--surface-raised)",
      transition: "background var(--dur-med) var(--ease-standard)",
      opacity: disabled ? 0.5 : 1,
      outline: "none",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      top: "50%",
      left: checked ? W - pad - size : pad + (thumbOn - thumbOff) / 2,
      width: size,
      height: size,
      borderRadius: "50%",
      background: checked ? "var(--surface-app)" : "var(--text-faint)",
      transform: "translateY(-50%)",
      transition: "left var(--dur-med) var(--ease-emphasized), width var(--dur-med) var(--ease-emphasized), background var(--dur-med) var(--ease-standard)"
    }
  }));
}
Object.assign(__ds_scope, { Switch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Switch.jsx", error: String((e && e.message) || e) }); }

// components/forms/TextField.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * TextField — outlined text input. Faint border at rest, accent border + glow
 * on focus, accent caret. Used in the rename sheet and new-preset dialog.
 */
function TextField({
  value,
  onChange,
  placeholder,
  disabled = false,
  fullWidth = true,
  style,
  ...rest
}) {
  const [focused, setFocused] = React.useState(false);
  return /*#__PURE__*/React.createElement("input", _extends({
    type: "text",
    value: value,
    placeholder: placeholder,
    disabled: disabled,
    onChange: e => onChange && onChange(e.target.value),
    onFocus: () => setFocused(true),
    onBlur: () => setFocused(false),
    style: {
      width: fullWidth ? "100%" : undefined,
      boxSizing: "border-box",
      height: 54,
      padding: "0 16px",
      borderRadius: "var(--radius-sm)",
      background: "transparent",
      color: "var(--text-strong)",
      fontFamily: "var(--font-sans)",
      fontSize: "var(--body-size)",
      fontWeight: "var(--fw-medium)",
      border: `1.5px solid ${focused ? "var(--accent-solid)" : "var(--line-2)"}`,
      boxShadow: focused ? "0 0 0 3px rgba(198,155,255,0.14)" : "none",
      caretColor: "var(--accent-solid)",
      outline: "none",
      transition: "border-color var(--dur-fast) var(--ease-standard), box-shadow var(--dur-fast) var(--ease-standard)",
      ...style
    }
  }, rest));
}
Object.assign(__ds_scope, { TextField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/TextField.jsx", error: String((e && e.message) || e) }); }

// components/navigation/NavBar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * NavBar — bottom navigation. Compact 62px bar on the card surface, no pill
 * indicator: the selected item simply turns accent-solid; unselected items are
 * faint. Labels are small semibold.
 */
function NavBar({
  items = [],
  value,
  onChange,
  base = "assets/icons/",
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("nav", _extends({
    style: {
      display: "flex",
      height: "var(--nav-height)",
      background: "var(--surface-card)",
      borderRadius: "4px 4px 0 0",
      ...style
    }
  }, rest), items.map(it => {
    const selected = it.id === value;
    const color = selected ? "var(--accent-solid)" : "var(--text-faint)";
    return /*#__PURE__*/React.createElement("button", {
      key: it.id,
      type: "button",
      onClick: () => onChange && onChange(it.id),
      style: {
        flex: "1 1 0",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: 4,
        background: "transparent",
        border: "none",
        cursor: "pointer",
        outline: "none",
        padding: 0
      }
    }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
      name: it.icon,
      size: 22,
      color: color,
      base: base
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        fontFamily: "var(--font-sans)",
        fontSize: "10.5px",
        fontWeight: "var(--fw-semibold)",
        color,
        transition: "color var(--dur-fast) var(--ease-standard)"
      }
    }, it.label));
  }));
}
Object.assign(__ds_scope, { NavBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/NavBar.jsx", error: String((e && e.message) || e) }); }

__ds_ns.Button = __ds_scope.Button;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.IconChip = __ds_scope.IconChip;

__ds_ns.ListRow = __ds_scope.ListRow;

__ds_ns.SoundCard = __ds_scope.SoundCard;

__ds_ns.Icon = __ds_scope.Icon;

__ds_ns.SelectTile = __ds_scope.SelectTile;

__ds_ns.Slider = __ds_scope.Slider;

__ds_ns.Switch = __ds_scope.Switch;

__ds_ns.TextField = __ds_scope.TextField;

__ds_ns.NavBar = __ds_scope.NavBar;

})();
