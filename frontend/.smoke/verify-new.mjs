// 针对本轮改动页面的定向验证：about 页 + 路线列表搜索框 + 首页 Hero tabs
import puppeteer from 'puppeteer-core'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const BASE = 'http://localhost:5173'
const routes = [
  { id: 1, name: '杭州经典两日游', intro: '西湖-灵隐-龙井', userId: { id: 10, nickname: '达人', avatar: '' }, dayCount: 2, startCity: '杭州', traffic: '高铁', budget: 800, status: 1, createTime: '2026-06-15T12:00:00.0' },
]
const pageOf = (c) => ({ code: 1, msg: 'ok', content: { content: c, totalElements: c.length, totalPages: 1 } })

const browser = await puppeteer.launch({ executablePath: EDGE, headless: 'new', args: ['--no-sandbox', '--disable-gpu'] })
const page = await browser.newPage()
await page.setViewport({ width: 1280, height: 900 })
await page.setRequestInterception(true)
page.on('request', (req) => {
  const url = req.url()
  const CORS = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type,Authorization' }
  if (url.includes('localhost:8081')) {
    if (req.method() === 'OPTIONS') return req.respond({ status: 200, headers: CORS, body: '' })
    if (/route\/queryByPage/.test(url)) return req.respond({ status: 200, headers: CORS, contentType: 'application/json', body: JSON.stringify(pageOf(routes)) })
    return req.respond({ status: 500, headers: CORS, contentType: 'application/json', body: '{"code":0,"msg":"no"}' })
  }
  req.continue()
})

const errors = []
page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
page.on('pageerror', (e) => errors.push(e.message))

async function goto(path) {
  try {
    await page.goto(BASE + path, { waitUntil: 'domcontentloaded', timeout: 15000 })
  } catch (e) {
    // 忽略导航超时，改用 JS 判断是否已加载目标文档
  }
  await new Promise(r => setTimeout(r, 1500))
  return await page.evaluate(() => document.location.pathname)
}

// 先登录页打开目标源，再写入登录态
await goto('/travel/login')
await page.evaluate(() => {
  localStorage.setItem('travel_admin_token', 'eyJ' + 'x'.repeat(80))
  localStorage.setItem('travel_admin_user', JSON.stringify({ userRole: 0, nickname: '测试用户', account: 'tester', phone: '13800138000', avatar: '' }))
})

// 1) 关于页
const aboutPath = await goto('/travel/home/about?section=privacy')
const aboutText = await page.evaluate(() => document.body.innerText)
const aboutOk = aboutText.includes('关于平台') && aboutText.includes('隐私政策') && aboutText.includes('帮助中心')
console.log(`关于页:      ${aboutOk ? 'OK' : 'FAIL'} path=${aboutPath} | 含关于/帮助/隐私章节 = ${aboutOk}`)

// 2) 路线列表 + 搜索框 + URL keyword 生效
const routePath = await goto('/travel/home/routes?keyword=杭州')
const routeState = await page.evaluate(() => {
  const text = document.body.innerText
  return {
    hasInput: !!document.querySelector('.search-card input'),
    hasResult: text.includes('杭州经典两日游')
  }
})
console.log(`路线列表:    ${routeState.hasInput && routeState.hasResult ? 'OK' : 'FAIL'} path=${routePath} ${JSON.stringify(routeState)}`)

// 3) 首页 Hero 搜索 tabs
const homePath = await goto('/travel/home')
const heroTabs = await page.evaluate(() => [...document.querySelectorAll('.tab-btn')].map(b => b.textContent.trim()))
console.log(`首页Hero tabs: ${heroTabs.length === 3 ? 'OK' : 'FAIL'} path=${homePath} ${JSON.stringify(heroTabs)}`)

console.log('控制台报错:', errors.length ? errors : '（无）')
await browser.close()
