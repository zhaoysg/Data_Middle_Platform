import { ref } from 'vue'

type Locale = 'zh-CN' | 'en-US'

const locale = ref<Locale>('zh-CN')

export function useLocale() {
  function setLocale(newLocale: Locale) {
    locale.value = newLocale
    document.documentElement.setAttribute('lang', newLocale)
  }

  return {
    locale,
    setLocale
  }
}
