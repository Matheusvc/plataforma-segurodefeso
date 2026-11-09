/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', '"Segoe UI"', 'Roboto', 'sans-serif'],
      },
      colors: {
        marca: {
          950: '#0B2F39',
          900: '#0E3B47',
          800: '#134B59',
          700: '#0E5A6B',
          600: '#156E82',
          100: '#D7EEF2',
          50: '#E6F1F4',
        },
        fundo: '#F3F5F6',
        borda: '#DCE3E6',
      },
    },
  },
  plugins: [],
}
