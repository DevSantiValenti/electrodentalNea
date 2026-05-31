---
name: Precision Dental Commerce
colors:
  surface: '#f8f9fb'
  surface-dim: '#d9dadc'
  surface-bright: '#f8f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#edeef0'
  surface-container-high: '#e7e8ea'
  surface-container-highest: '#e1e2e4'
  on-surface: '#191c1e'
  on-surface-variant: '#43474c'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f3'
  outline: '#73777d'
  outline-variant: '#c3c7cd'
  surface-tint: '#4a6077'
  primary: '#000d1b'
  on-primary: '#ffffff'
  primary-container: '#0b2438'
  on-primary-container: '#758ca4'
  inverse-primary: '#b2c9e3'
  secondary: '#b71f2a'
  on-secondary: '#ffffff'
  secondary-container: '#fe5455'
  on-secondary-container: '#5c000a'
  tertiary: '#130b00'
  on-tertiary: '#ffffff'
  tertiary-container: '#2f2000'
  on-tertiary-container: '#b18200'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#cee5ff'
  primary-fixed-dim: '#b2c9e3'
  on-primary-fixed: '#031d31'
  on-primary-fixed-variant: '#32495e'
  secondary-fixed: '#ffdad7'
  secondary-fixed-dim: '#ffb3af'
  on-secondary-fixed: '#410005'
  on-secondary-fixed-variant: '#930017'
  tertiary-fixed: '#ffdea3'
  tertiary-fixed-dim: '#f8bd39'
  on-tertiary-fixed: '#261900'
  on-tertiary-fixed-variant: '#5d4200'
  background: '#f8f9fb'
  on-background: '#191c1e'
  surface-variant: '#e1e2e4'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
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
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  container-margin: 24px
  gutter: 16px
---

## Brand & Style

This design system is built on the pillars of precision, sterility, and technical expertise. It caters to healthcare professionals who require efficiency and reliability when sourcing high-end dental equipment. The aesthetic is **Corporate / Modern**, leaning heavily into **Minimalism** to ensure that complex product specifications remain legible and accessible. 

The emotional response should be one of "Clinical Confidence"—the UI feels as organized and high-performance as a modern dental operatory. We utilize generous whitespace to reduce cognitive load, suggesting a premium, well-maintained environment where quality is paramount.

## Colors

The palette is anchored by **Dark Technical Blue**, which establishes an immediate sense of institutional trust and authority. **Brand Red** is reserved strictly for high-priority actions (CTAs) and urgent alerts, while **Brand Gold** is used as a signifier for premium equipment tiers, special offers, and loyalty status.

The foundation relies on a "Super-White" surface (#FFFFFF) resting on a "Clinical Grey" background (#F7F8FA) to create subtle separation between the canvas and the content containers. Text utilizes a high-contrast charcoal for primary information and a softer slate for metadata to maintain a clear visual hierarchy.

## Typography

The design system utilizes **Inter** exclusively to maintain a systematic, utilitarian appearance that prioritizes legibility. The type scale is optimized for technical documentation and e-commerce listings.

Headlines use semi-bold weights with slight negative letter spacing to feel "engineered" and compact. Body copy is set with generous line heights to ensure readability of technical specifications and long-form equipment descriptions. Label styles are used for navigation and data points, often employing a medium weight to distinguish them from standard body text.

## Layout & Spacing

This design system follows an **8px linear scale** to ensure consistent rhythm across all components. The layout uses a **12-column fluid grid** for desktop and a single-column fluid grid for mobile.

- **Desktop (1200px+):** 24px margins, 16px gutters.
- **Tablet (768px - 1199px):** 24px margins, 16px gutters.
- **Mobile (Up to 767px):** 16px margins, 12px gutters.

Spacing should be "generous" to avoid a cluttered, discount-store feel. Large-scale equipment photos should be surrounded by `xl` or `xxl` padding to emphasize their value and technical detail.

## Elevation & Depth

Visual hierarchy is achieved through a combination of **Tonal Layers** and **Ambient Shadows**. 

The background (#F7F8FA) serves as the lowest level. Content containers (Cards) sit on the middle level using #FFFFFF. To create depth, cards use a 1px border (#E5E7EB) and a very soft, diffused shadow: `0 4px 20px rgba(11, 36, 56, 0.05)`. This shadow is tinted with the primary dark blue color to maintain a cohesive "technical" atmosphere.

Interactive elements like hovered buttons or active modals use a slightly more pronounced shadow to indicate they have "lifted" off the surface.

## Shapes

The shape language is "Soft-Technical." By using a **Rounded** setting (0.5rem base), we balance the coldness of a medical/technical site with a modern, approachable feel. 

- **Small Components (Checkboxes, Tags):** 4px (Soft)
- **Buttons & Inputs:** 8px (Rounded)
- **Cards & Large Containers:** 24px (Rounded-XL)

The significant rounding on cards (24px) is a signature element of the design system, creating a distinctive "pillowed" look for product showcases that contrasts with the sharp, clean lines of the typography.

## Components

### Buttons
- **Primary:** Background #B8202A, white text. Bold and high-contrast for "Add to Cart" or "Buy Now."
- **Secondary:** Background #0B2438, white text. Used for technical downloads or secondary navigation.
- **Outline:** Transparent background, 1px border #E5E7EB, text #1F2933. Used for less important actions.

### Cards
Cards are white (#FFFFFF) with 24px corner radius. They must include a subtle 1px border. Padding inside cards should be generous (24px or 32px) to allow product photography to breathe.

### Input Fields
Inputs use an 8px radius with a 1px #E5E7EB border. When focused, the border transitions to the Primary Blue (#0B2438) with a 2px outer glow in a translucent version of the same color.

### Chips & Badges
Small, 4px rounded labels. For "New" or "In Stock," use a soft blue tint. For "Sale" or "Premium," use the Brand Gold (#D9A21B) with white text.

### Icons
Use **Linear Icons** with a consistent 2px stroke weight. Icons should be rendered in #0B2438 for active states and #6B7280 for decorative/inactive states. Avoid filled icons unless used as a notification indicator.

### Lists
Lists for technical specs should use a light border-bottom (#E5E7EB) between items, with 12px padding top and bottom to maintain vertical rhythm.