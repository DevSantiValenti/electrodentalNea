---
name: Lumina Tech Core
colors:
  surface: '#011427'
  surface-dim: '#011427'
  surface-bright: '#293a4f'
  surface-container-lowest: '#000f20'
  surface-container-low: '#091d30'
  surface-container: '#0d2134'
  surface-container-high: '#192b3f'
  surface-container-highest: '#24364a'
  on-surface: '#d1e4fe'
  on-surface-variant: '#bcc9cc'
  inverse-surface: '#d1e4fe'
  inverse-on-surface: '#203246'
  outline: '#869396'
  outline-variant: '#3d494b'
  surface-tint: '#59d7ec'
  primary: '#66e2f8'
  on-primary: '#00363e'
  primary-container: '#43c6db'
  on-primary-container: '#004f59'
  inverse-primary: '#006876'
  secondary: '#a4c9fc'
  on-secondary: '#00315b'
  secondary-container: '#204874'
  on-secondary-container: '#93b8ea'
  tertiary: '#7ae2e2'
  on-tertiary: '#003737'
  tertiary-container: '#5cc6c6'
  on-tertiary-container: '#005050'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#9eefff'
  primary-fixed-dim: '#59d7ec'
  on-primary-fixed: '#001f24'
  on-primary-fixed-variant: '#004e59'
  secondary-fixed: '#d3e4ff'
  secondary-fixed-dim: '#a4c9fc'
  on-secondary-fixed: '#001c38'
  on-secondary-fixed-variant: '#204874'
  tertiary-fixed: '#8cf3f3'
  tertiary-fixed-dim: '#6ed7d7'
  on-tertiary-fixed: '#002020'
  on-tertiary-fixed-variant: '#004f50'
  background: '#011427'
  on-background: '#d1e4fe'
  surface-variant: '#24364a'
typography:
  headline-xl:
    fontFamily: Sora
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Sora
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.2'
  headline-lg-mobile:
    fontFamily: Sora
    fontSize: 28px
    fontWeight: '600'
    lineHeight: '1.2'
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  label-sm:
    fontFamily: Sora
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 24px
  lg: 48px
  xl: 80px
  container-max: 1280px
  gutter: 24px
---

## Brand & Style

The visual identity of the design system is anchored in "Technical Precision." It is designed to evoke a sense of high-end reliability and cutting-edge innovation for the dental technology sector. The aesthetic balances the sterility of medical environments with the sophistication of modern SaaS platforms.

The design style utilizes **Glassmorphism** as its primary structural driver. Surfaces are treated as translucent, layered panes that allow the deep technical blues of the background to permeate through, creating a sense of depth and architectural complexity. This is complemented by **Corporate Modern** sensibilities—generous whitespace, meticulous alignment, and a focus on clarity. The emotional response should be one of "calm confidence" and "expert-grade quality."

## Colors

The palette is built on a "Deep Sea Tech" foundation. 

- **Primary (#43C6DB):** Used for interactive elements, primary call-to-actions, and key data visualizations. It represents the "pulse" of the technology.
- **Secondary (#0D3B66):** Functions as the base for container backgrounds and depth layers.
- **Tertiary (#7EE6E6):** A highlight color for accents, successful states, and decorative micro-details.
- **Neutral (#071B2E):** The deep canvas. It provides the infinite depth required for glass effects to thrive.

The default color mode is **Dark**. In this mode, surfaces use the `surface_glass` token—a semi-transparent Technical Blue—to maintain the glassmorphic theme. Light Gray (#F4F7FA) is reserved for subtle borders or background elements in administrative/document-heavy views.

## Typography

This design system uses a dual-sans-serif approach to reinforce the technical narrative. 

**Sora** is the display face. Its geometric construction and wide stance give headings a futuristic, architectural feel. It should be used for all headlines and interactive labels (buttons, tabs).

**Hanken Grotesk** serves as the functional workhorse. It offers exceptional legibility at smaller sizes for technical specifications and body copy. 

To maintain the premium feel, use tighter letter spacing on large headlines and increased line-height (1.6) on body text to ensure the interface feels airy and readable.

## Layout & Spacing

The design system employs a **Fixed Grid** model for desktop to maintain a highly curated, "editorial" technical layout. 

- **Desktop (1200px+):** 12-column grid, 24px gutters, and 80px side margins.
- **Tablet (768px - 1199px):** 8-column grid, 24px gutters, 40px margins. Content may fluidly resize within these bounds.
- **Mobile (0 - 767px):** 4-column fluid grid, 16px gutters, 16px margins.

Spacing follows a strict 8px base unit. Use larger gaps (`xl`) between major sections to emphasize the premium "whitespace-first" philosophy. Components should utilize internal padding of `md` (24px) to ensure content never feels cramped within its glass container.

## Elevation & Depth

Depth is not communicated through traditional black shadows, but through **Tonal Glassmorphism** and light-source simulation.

1.  **Backdrop Blur:** All floating containers (cards, modals, dropdowns) must have a `backdrop-filter: blur(20px)`.
2.  **Surface Strokes:** Every elevated element requires a 1px solid border. Use `rgba(255, 255, 255, 0.1)` on the top and left, and `rgba(67, 198, 219, 0.2)` on the bottom and right to simulate a technical light source hitting the edges.
3.  **Shadows:** Use large, diffused "Ambient Glows" instead of shadows. For example: `0 20px 40px rgba(7, 27, 46, 0.4)`. For primary actions, use a soft blue tint in the shadow (`rgba(64, 198, 219, 0.15)`).

## Shapes

The shape language is defined by **expansive curves**. 

- **Containers/Cards:** Use `rounded-xl` (1.5rem / 24px) to soften the technical aesthetic and provide a premium, modern feel.
- **Buttons:** Use pill-shaped (100px) or `rounded-lg` (1rem / 16px) depending on the hierarchy. 
- **Iconography:** Icons must be linear, using a 2px stroke width with slightly rounded terminals to match the font geometry of Sora. 

Avoid sharp corners entirely; even input fields should maintain a minimum of 8px (rounded-md) to ensure consistency across the UI.

## Components

### Buttons
- **Primary:** Gradient fill (Technical Blue to Technological Light Blue), white text, 16px horizontal padding, Sora SemiBold.
- **Secondary (Glass):** `surface_glass` background with 1px light blue border and blur.
- **Ghost:** No background, primary color text, 2px bottom border on hover.

### Cards
Cards are the "hero" component. They feature the 20px backdrop blur, 24px internal padding, and the dual-tone 1px border. Background should be a subtle gradient: `linear-gradient(135deg, rgba(13, 59, 102, 0.6) 0%, rgba(7, 27, 46, 0.8) 100%)`.

### Inputs & Form Fields
Fields should be dark and recessed. Use `rgba(0,0,0,0.2)` fill with a 1px Technical Blue border. On focus, the border transitions to Soft Cyan with a 4px outer "glow" (spread shadow).

### Chips/Badges
Small, high-contrast pills. Use a solid Technical Blue background with Soft Cyan text for "Status" indicators.

### Micro-Animations
- **Hover States:** Elements should subtly "lift" (move -4px Y-axis) and the border-opacity should increase.
- **Transitions:** Use `cubic-bezier(0.4, 0, 0.2, 1)` for all opacity and transform transitions to ensure a "snappy yet smooth" technical feel.