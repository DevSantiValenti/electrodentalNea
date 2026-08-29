module.exports = {
  content: [
    "./src/main/resources/templates/catalogo.html",
    "./src/main/resources/templates/checkout-resultado.html",
    "./src/main/resources/templates/contact.html",
    "./src/main/resources/templates/home.html",
    "./src/main/resources/templates/producto.html",
    "./src/main/resources/templates/simple-page.html",
    "./src/main/resources/templates/fragments/shop.html"
  ],
  theme: {
    extend: {
      colors: {
        primary: "#000d1b",
        "primary-container": "#0b2438",
        secondary: "#b71f2a",
        background: "#f8f9fb",
        surface: "#ffffff",
        "surface-soft": "#f2f4f6",
        outline: "#c3c7cd"
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"]
      }
    }
  },
  plugins: [
    require("@tailwindcss/forms"),
    require("@tailwindcss/container-queries")
  ]
};
