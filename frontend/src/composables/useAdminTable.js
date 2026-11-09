/**
 * 后台管理页分页列表组合式函数
 * - 竞态守卫：快速切换筛选/翻页时，先发出的旧请求晚返回不会覆盖新结果
 * - code=0（如「没有找到数据」空态）时清空旧列表，避免表格残留脏数据
 * - 删除/筛选导致总页数变少时，自动把页码 clamp 回最后一页并补一次查询
 *
 * @param {Function} fetchFn  (params) => Promise<res>，res.code===1 时 res.content 为 Spring Page
 * @param {Object}   options  { size: 每页条数(默认10), extraParams: () => 附加筛选参数 }
 */
import { ref } from 'vue'

export function useAdminTable(fetchFn, options = {}) {
  const list = ref([])
  const loading = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(options.size || 10)
  const totalElements = ref(0)
  const totalPages = ref(0)

  let latestSeq = 0

  async function fetchData() {
    const seq = ++latestSeq
    loading.value = true
    try {
      const params = {
        page: currentPage.value,
        size: pageSize.value,
        ...(options.extraParams?.() || {})
      }
      const res = await fetchFn(params)
      if (seq !== latestSeq) return // 已有更新的查询发出，丢弃过期结果
      if (res.code === 1) {
        list.value = res.content?.content || []
        totalElements.value = res.content?.totalElements ?? list.value.length
        totalPages.value = res.content?.totalPages ?? Math.ceil(totalElements.value / pageSize.value)
        // 页码越界（删除/筛选后总页数变少）时回到最后一页再查一次
        const maxPage = Math.max(totalPages.value, 1)
        if (currentPage.value > maxPage) {
          currentPage.value = maxPage
          return fetchData()
        }
      } else {
        // 「没有找到数据」等空态：清空旧列表，保持总数与表格一致
        list.value = []
        totalElements.value = 0
        totalPages.value = 0
      }
    } catch {
      // 拦截器已统一提示，这里不再弹错
    } finally {
      if (seq === latestSeq) loading.value = false
    }
  }

  return { list, loading, currentPage, pageSize, totalElements, totalPages, fetchData }
}
