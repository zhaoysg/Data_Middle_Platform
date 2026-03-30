import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import Components from 'unplugin-vue-components/vite'
import AutoImport from 'unplugin-auto-import/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'
import compression from 'vite-plugin-compression'

export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
      eslintrc: { enabled: true }
    }),
    Components({
      resolvers: [
        AntDesignVueResolver({ cjs: true, importStyle: false })
      ],
      dts: false,
      include: [
        'src/views/**/*.{vue,ts}',
        'src/layouts/**/*.{vue,ts}',
        'src/components/**/*.{vue,ts}'
      ]
    }),
    compression({
      algorithm: 'gzip',
      threshold: 10240
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '#': resolve(__dirname, 'src/types'),
      // More specific paths must come before less specific ones (aliases map to directories, not index.js)
      'ant-design-vue/lib': resolve(__dirname, 'node_modules/ant-design-vue/lib'),
      'ant-design-vue/es': resolve(__dirname, 'node_modules/ant-design-vue/es'),
      'ant-design-vue': resolve(__dirname, 'node_modules/ant-design-vue/lib'),
      'lodash-es': resolve(__dirname, 'node_modules/lodash-es'),
      '@ant-design/icons-vue': resolve(__dirname, 'node_modules/@ant-design/icons-vue'),
      '@ant-design/icons-svg': resolve(__dirname, 'node_modules/@ant-design/icons-svg')
    },
    extensions: ['.ts', '.tsx', '.vue', '.json', '.js', '.jsx']
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  },
  build: {
    target: 'es2015',
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'ant-vendor': ['ant-design-vue']
        }
      }
    }
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
        modifyVars: {
          'primary-color': '#1677FF',
          'border-radius-base': '6px'
        }
      }
    }
  }
})
