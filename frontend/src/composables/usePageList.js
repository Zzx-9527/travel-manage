import { ref } from 'vue'
import { normalizeList } from '@/utils/format'

/**
 * 分页列表组合式函数（用户端列表页通用）
 * 复刻后台各管理页的 currentPage/pageSize/totalElements/totalPages 模式
 *
 * @param {Function} fetchFn  (params) => Promise<res>，res.code===1 时 res.content 为 Spring Page 或数组
 * @param {Object}   options  { size: 每页条数，默认 12 }
 */
export function usePageList(fetchFn, options = {}) {
  const list = ref([])
  const total = ref(0)
  const totalPages = ref(0)
  const page = ref(1)
  const size = ref(options.size || 12)
  const loading = ref(false)
  // 接口不可用（未实现/网络错误）时为 true，页面据此渲染空态
  const degraded = ref(false)

  let lastParams = {}

  async function load(extraParams = {}) {
    lastParams = extraParams
    loading.value = true
    try {
      const params = { page: page.value, size: size.value, ...lastParams }
      const res = await fetchFn(params)
      if (res.code === 1) {
        list.value = normalizeList(res.content)
        total.value = res.content?.totalElements ?? list.value.length
        totalPages.value = res.content?.totalPages ?? Math.ceil(total.value / size.value)
        degraded.value = false
      } else {
        // code=0（如「没有找到数据」）属正常空态：清空列表，不弹错也不算降级
        list.value = []
        total.value = 0
        totalPages.value = 0
        degraded.value = false
      }
    } catch (err) {
      // 拦截器已处理提示（silent 除外），这里标记降级
      degraded.value = true
    } finally {
      loading.value = false
    }
  }

  function reset() {
    page.value = 1
    list.value = []
    total.value = 0
    totalPages.value = 0
  }

  function setPage(p) {
    page.value = p
    return load()
  }

  function setSize(s) {
    size.value = s
    page.value = 1
    return load()
  }

  return { list, total, totalPages, page, size, loading, degraded, load, reset, setPage, setSize }
}
