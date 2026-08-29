module.exports = {
  content: [
    "./src/main/resources/templates/error.html"
  ],
  theme: {
    extend: {
      colors: {
        primary: "#000d1b",
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
