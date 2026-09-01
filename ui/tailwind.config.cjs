/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{vue,ts,tsx,js,jsx}'],
  theme: {
    extend: {},
  },
  plugins: [],
  corePlugins: {
    preflight: false, // 不重置样式，避免干扰 Halo 控制台
  },
  important: false,
}
