/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // 品牌青绿色板（与 main.css 的 --brand-* token 同源）
        ocean: {
          50: '#effcf9',
          100: '#d7f7ef',
          200: '#b0efe0',
          300: '#7ae2cc',
          400: '#3fcdb2',
          500: '#1cb89d',
          600: '#0f9481',
          700: '#107769',
          800: '#125e55',
          900: '#134e48',
          950: '#052e2c',
        },
      },
      fontFamily: {
        sans: ['"PingFang SC"', '"Microsoft YaHei"', 'sans-serif'],
      },
      boxShadow: {
        'card': '0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06)',
        'card-hover': '0 12px 40px rgba(0,0,0,0.08), 0 4px 12px rgba(0,0,0,0.05)',
        'nav': '0 1px 3px rgba(0,0,0,0.08)',
      }
    },
  },
  plugins: [],
}
