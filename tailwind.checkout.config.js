module.exports = {
  content: [
    "./src/main/resources/templates/finalizar-compra.html"
  ],
  theme: {
    extend: {
      colors: {
        primary: "#0b2438",
        secondary: "#b71f2a",
        background: "#f8f9fb",
        outline: "#d5d9df"
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
