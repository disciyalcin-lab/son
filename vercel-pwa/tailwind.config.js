/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'],
        arabic: ['"Amiri"', '"Scheherazade New"', 'serif']
      },
      colors: {
        crimson: {
          DEFAULT: '#B7282E',
          dark: '#E2474D',
          container: '#FDECEC',
          onContainer: '#8E1B20',
          containerDark: '#4E161A',
          onContainerDark: '#FFD9DA'
        },
        champagne: {
          DEFAULT: '#C89D42',
          light: '#DEC286',
          secondaryLight: '#9E7B3B',
          containerLight: '#DEC286',
          onContainerLight: '#2C2214',
          containerDark: '#4F3E1E',
          onContainerDark: '#F7E3B5'
        },
        obsidian: {
          bgLight: '#F7F7F9',
          surfaceLight: '#FFFFFF',
          variantLight: '#F0F0F4',
          outlineLight: '#8A8A96',
          bgDark: '#111115',
          surfaceDark: '#1B1B22',
          variantDark: '#262630',
          outlineDark: '#7E7E8C'
        },
        success: '#2E7D32',
        warning: '#E65100'
      }
    }
  },
  plugins: []
};
