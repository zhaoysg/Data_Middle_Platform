import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useMenuStore = defineStore('menu', () => {
  /**
   * 菜单版本号，每次修改菜单后递增。
   * 仅供菜单管理页面保存后通知相关组件刷新使用。
   */
  const version = ref(0)

  function incrementVersion() {
    version.value++
  }

  return {
    version,
    incrementVersion
  }
})
