// 用户端冒烟测试：拦截 API 返回模拟数据，遍历关键页面截图并收集控制台报错
import puppeteer from 'puppeteer-core'
import { mkdirSync } from 'node:fs'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const BASE = 'http://localhost:5173'
const OUT = 'd:/project/travel-platform/.smoke'
mkdirSync(OUT, { recursive: true })

const COVER = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="140"><rect width="200" height="140" fill="%231CB89D"/><text x="50%" y="50%" fill="white" font-size="20" text-anchor="middle">景点图</text></svg>'

const spots = [
  { id: 1, name: '西湖风景区', cover: COVER, price: 0, score: 4.8, address: '浙江省杭州市西湖区', categoryId: { id: 1, name: '湖泊' }, status: 1, openTime: '08:00-18:00', viewCount: 1234, intro: '西湖是杭州的灵魂，三面云山一面城。', images: `${COVER},${COVER}` },
  { id: 2, name: '故宫博物院', cover: COVER, price: 60, score: 4.9, address: '北京市东城区景山前街4号', categoryId: { id: 2, name: '古迹' }, status: 1, openTime: '08:30-17:00', viewCount: 999, intro: '世界文化遗产，明清两代皇宫。', images: `${COVER}` },
  { id: 3, name: '张家界国家森林公园', cover: COVER, price: 228, score: 4.7, address: '湖南省张家界市', categoryId: { id: 3, name: '山川' }, status: 1, openTime: '07:00-18:30', viewCount: 888, intro: '三千奇峰，八百秀水。', images: `${COVER},${COVER},${COVER}` },
]
const categories = [
  { id: 1, name: '湖泊' }, { id: 2, name: '古迹' }, { id: 3, name: '山川' },
  { id: 4, name: '历史' }, { id: 5, name: '文化' }, { id: 6, name: '森林' },
]
const strategies = [
  { id: 1, title: '杭州三日游全攻略', cover: COVER, content: '第一天逛西湖，第二天去灵隐寺，第三天逛河坊街。这是一篇很长的攻略内容，用于测试正文换行与截断显示是否正常。', userId: { id: 10, nickname: '旅行达人', avatar: '' }, auditStatus: 1, collectCount: 32, readCount: 1560, createTime: '2026-07-01T10:00:00.0' },
  { id: 2, title: '北京周末游路线', cover: COVER, content: '故宫、天坛、颐和园，两天走遍北京经典。', userId: { id: 11, nickname: '阿川', avatar: '' }, auditStatus: 1, collectCount: 18, readCount: 760, createTime: '2026-07-10T09:30:00.0' },
]
const routes = [
  { id: 1, name: '杭州经典两日游', intro: '西湖-灵隐-龙井，最经典的杭州路线。', userId: { id: 10, nickname: '旅行达人', avatar: '' }, dayCount: 2, startCity: '杭州', traffic: '高铁', budget: 800, status: 1, createTime: '2026-06-15T12:00:00.0' },
  { id: 2, name: '北京皇城三日游', intro: '故宫-长城-颐和园，感受历史底蕴。', userId: { id: 11, nickname: '阿川', avatar: '' }, dayCount: 3, startCity: '北京', traffic: '自驾', budget: 1500, status: 1, createTime: '2026-06-20T12:00:00.0' },
]
const routeDetails = [
  { id: 1, routeId: { id: 1, name: '杭州经典两日游' }, scenicId: { id: 1, name: '西湖风景区' }, dayNo: 1, sort: 1, stayTime: '4小时' },
  { id: 2, routeId: { id: 1, name: '杭州经典两日游' }, scenicId: { id: 3, name: '张家界国家森林公园' }, dayNo: 1, sort: 2, stayTime: '3小时' },
  { id: 3, routeId: { id: 2, name: '北京皇城三日游' }, scenicId: { id: 2, name: '故宫博物院' }, dayNo: 1, sort: 1, stayTime: '全天' },
]

function pageOf(content) {
  return { code: 1, msg: 'ok', content: { content, totalElements: content.length, totalPages: 1 } }
}

const mocks = [
  [/scenic\/queryByPage/, pageOf(spots)],
  [/scenicCategory\/queryByPage/, pageOf(categories)],
  [/strategy\/queryByPage/, pageOf(strategies)],
  [/route\/queryByPage/, pageOf(routes)],
  [/routeDetail\/queryByPage/, pageOf(routeDetails)],
]

const routesToTest = [
  ['/travel/login', 'login'],
  ['/travel/register', 'register'],
  ['/travel/home', 'home'],
  ['/travel/home/spots', 'spots'],
  ['/travel/home/spots/1', 'spot-detail'],
  ['/travel/home/guides', 'guides'],
  ['/travel/home/guides/1', 'guide-detail'],
  ['/travel/home/routes', 'routes'],
  ['/travel/home/routes/1', 'route-detail'],
  ['/travel/home/about', 'about'],
  ['/travel/home/mine/profile', 'mine-profile'],
  ['/travel/home/mine/orders', 'mine-orders'],
  ['/travel/home/mine/favorites', 'mine-favorites'],
  ['/travel/home/mine/comments', 'mine-comments'],
]

const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: 'new',
  args: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage']
})

const page = await browser.newPage()
await page.setViewport({ width: 1280, height: 900 })

// 拦截后端请求 → 返回模拟数据
// 注意：需正确处理 OPTIONS 预检并给 mock 响应带 CORS 头，否则浏览器判定跨域失败、真实请求不生效
const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type,Authorization'
}

await page.setRequestInterception(true)
page.on('request', (req) => {
  const url = req.url()
  if (url.includes('localhost:8081')) {
    if (req.method() === 'OPTIONS') {
      req.respond({ status: 200, headers: CORS_HEADERS, body: '' })
      return
    }
    const hit = mocks.find(([re]) => re.test(url))
    if (hit) {
      req.respond({ status: 200, headers: CORS_HEADERS, contentType: 'application/json', body: JSON.stringify(hit[1]) })
    } else {
      // 未知接口（myProfile/orders/comments/collect/detail 等）返回失败，验证优雅降级
      req.respond({ status: 500, headers: CORS_HEADERS, contentType: 'application/json', body: JSON.stringify({ code: 0, msg: 'mock-unavailable' }) })
    }
  } else {
    req.continue()
  }
})

const errors = []
page.on('console', (msg) => { if (msg.type() === 'error') errors.push(`[console] ${msg.text()}`) })
page.on('pageerror', (err) => errors.push(`[pageerror] ${err.message}`))

const results = []
for (const [path, name] of routesToTest) {
  try {
    // 首个页面（login）无需种子；其余页面需登录态
    if (path !== '/travel/login') {
      await page.evaluate(() => {
        localStorage.setItem('travel_admin_token', 'eyJ' + 'x'.repeat(80))
        localStorage.setItem('travel_admin_user', JSON.stringify({ userRole: 0, nickname: '测试用户', account: 'tester', phone: '13800138000', avatar: '' }))
      })
    }
    await page.goto(BASE + path, { waitUntil: 'networkidle2', timeout: 20000 })
    await new Promise(r => setTimeout(r, 800))
    await page.screenshot({ path: `${OUT}/${name}.png` })
    // 抓取页面标题文本与是否出现降级空态
    const title = await page.title()
    const bodyText = (await page.evaluate(() => document.body.innerText)).slice(0, 120).replace(/\s+/g, ' ')
    results.push(`OK   ${path.padEnd(28)} title="${title}" body="${bodyText}"`)
  } catch (e) {
    results.push(`FAIL ${path.padEnd(28)} ${e.message}`)
  }
}

console.log('=== 页面结果 ===')
console.log(results.join('\n'))
console.log('\n=== 控制台报错 ===')
console.log(errors.length ? errors.join('\n') : '（无）')

await browser.close()
